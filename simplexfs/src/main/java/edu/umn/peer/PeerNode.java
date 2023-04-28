package edu.umn.peer;

import edu.umn.utils.LatencyTable;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PeerNode {
    private final String fileDirectory;
    private final int port;
    private final Map<String, String> fileChecksums;
    private final LatencyTable latencyTable;
    private final AtomicInteger loadIndex;
    private final ThreadPoolExecutor executor;

    private ServerSocket serverSocket;

    private volatile boolean running;

    // Define constants for max retries and buffer size for file transfer
    private static final int MAX_RETRIES = 3;
    private static final int BUFFER_SIZE = 4096;

    // Add tracking server IP and port as class members

    // Use the ServerInfo class to store the tracking server information
    private final ServerInfo trackingServer;

    public PeerNode(String fileDirectory, int port, String latencyFilePath, String trackingServerIp, int trackingServerPort) {
        this.fileDirectory = fileDirectory;
        this.port = port;
        this.fileChecksums = new HashMap<>();
        this.latencyTable = new LatencyTable(latencyFilePath);
        // Create a ServerInfo object for the tracking server
        this.trackingServer = new ServerInfo(trackingServerIp, trackingServerPort);
        this.loadIndex = new AtomicInteger(0);
        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }
    private static class ServerInfo {
        private final String ipAddress;
        private final int port;


        public ServerInfo(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }
        @Override
        public String toString() {
            return "ServerInfo{" +
                    "ipAddress='" + ipAddress + '\'' +
                    ", port=" + port +
                    '}';
        }
    }
    public Map<String, String> getFileChecksums() {
        return fileChecksums;
    }

    public void initialize() {
        // Scan the file directory and compute checksums
        try {
            Files.walk(Path.of(fileDirectory))
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String checksum = computeChecksum(filePath);
                        if (checksum != null) {
                            fileChecksums.put(filePath.getFileName().toString(), checksum);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Debug: Print the fileChecksums map
        System.out.println("File checksums: " + fileChecksums);
        // Debug: Print the file names in the fileChecksums map
        System.out.println("File names: " + fileChecksums.keySet());
    }


    private String computeChecksum(Path filePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] checksum = md.digest(fileBytes);
            return bytesToHex(checksum);

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void start() {
        System.out.println("Starting peer node on port " + port);
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    // Handle incoming connections from other peers and the server
                    // Spawn a new thread to handle each connection
                    executor.submit(() -> handleConnection(socket));
                } catch (SocketException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void stop() {
        running = false;
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            serverSocket.close(); // Close the server socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleConnection(Socket socket) {
        try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

            String requestType = (String) inputStream.readObject();
            if ("DOWNLOAD".equals(requestType)) {
                // Handle DOWNLOAD request from other peers
                handleFileDownloadRequest(inputStream, outputStream, socket);
            } else if ("GET_LOAD".equals(requestType)) {
                // Handle GET_LOAD request from other peers
                handleLoadRequest(outputStream);
            } else {
                // Unknown request type
                System.out.println("Unknown request type: " + requestType);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private void handleLoadRequest(ObjectOutputStream outputStream) throws IOException {
        // Send the current load index to the requesting peer
        outputStream.writeInt(getLoad());
    }
    private void handleFileDownloadRequest(ObjectInputStream inputStream, ObjectOutputStream outputStream, Socket socket) throws IOException {
        try {
            String filename = (String) inputStream.readObject();
            loadIndex.incrementAndGet(); // Increment the load index
            sendFile(filename, socket);
            loadIndex.decrementAndGet(); // Decrement the load index
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void downloadFile(String filename, String peerIpAddress, int peerPort) {
        try (Socket socket = new Socket(peerIpAddress, peerPort);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            // Send download request
            outputStream.writeObject("DOWNLOAD");
            outputStream.writeObject(filename);

            loadIndex.incrementAndGet(); // Increment the load index
            // Receive file
            receiveFile(filename, socket);
            loadIndex.decrementAndGet(); // Decrement the load index

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void downloadFile(String filename, List<String> peerList, double loadWeight) {
        String bestPeer = selectBestPeer(peerList, loadWeight);
        if (bestPeer != null) {
            String[] parts = bestPeer.split(":");
            String ipAddress = parts[0];
            int port = Integer.parseInt(parts[1]);
            downloadFile(filename, ipAddress, port);
        } else {
            System.out.println("No suitable peer found for downloading the file.");
        }
    }



    private void sendFile(String filename, Socket socket) throws IOException {
        File file = new File(fileDirectory, filename);
        if (file.exists()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            try (InputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 OutputStream os = socket.getOutputStream()) {

                int bytesRead;
                while ((bytesRead = bis.read(buffer)) > 0) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } else {
            throw new FileNotFoundException("File not found: " + filename);
        }
    }

    private void receiveFile(String filename, Socket socket) throws IOException {
        File file = new File(fileDirectory, filename);
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream is = socket.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(is);
             OutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            int bytesRead;
            while ((bytesRead = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, bytesRead);
            }
        }

        // Verify the checksum of the received file
        String computedChecksum = computeChecksum(file.toPath());
        System.out.println("Computed checksum: " + computedChecksum);
        // Get the original checksum from the tracking server

        String originalChecksum  = findFile(filename).get(0).split(":")[2];
        System.out.println("Original checksum: " + originalChecksum);

        // If the checksums don't match, retry the verification up to MAX_RETRIES times
        if (!computedChecksum.equals(originalChecksum)) {
            int retryCount = 0;
            boolean successfulVerification = false;

            while (retryCount < MAX_RETRIES && !successfulVerification) {
                computedChecksum = computeChecksum(file.toPath());
                successfulVerification = computedChecksum.equals(originalChecksum);
                retryCount++;
            }

            if (!successfulVerification) {
                System.out.println("File verification failed after " + retryCount + " retries.");
            }
        }
    }

    public List<String> findFile(String filename) {
        // Sends a request to the tracking server to get a list of nodes that store the specified file
        try (Socket socket = new Socket(trackingServer.ipAddress, trackingServer.port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            // Send request
            outputStream.writeObject("FIND");
            outputStream.writeObject(filename);

            // Receive response
            @SuppressWarnings("unchecked")
            List<String> peerList = (List<String>) inputStream.readObject();
            return peerList;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
    public int getLoad() {
        // Get the current load (number of concurrent downloads or uploads) of the peer
        return loadIndex.get();
    }
    private synchronized void updateLoadIndex(int delta) {
        loadIndex.addAndGet(delta);
    }



//    public void updateFileList() {
//        // Updates the list of files stored in the specific directory and sends the updated list to the tracking server
//        try (Socket socket = new Socket(trackingServer.ipAddress, trackingServer.port);
//             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
//
//            // Send request
//            outputStream.writeObject("UPDATE_LIST");
//            outputStream.writeInt(port);
//            outputStream.writeObject(fileChecksums);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void updateFileList() {
        // Updates the list of files stored in the specific directory and sends the updated list to the tracking server
        try (Socket socket = new Socket(trackingServer.ipAddress, trackingServer.port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            // Send request
            outputStream.writeObject("UPDATE_LIST");
            outputStream.writeInt(port);
            outputStream.writeObject(fileChecksums);
            outputStream.flush();

            // Read response
            String response = (String) inputStream.readObject();
            System.out.println("Server response: " + response);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private int getRemotePeerLoad(String ipAddress, int port) {
        int load = -1;
        try (Socket socket = new Socket(ipAddress, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Send request for load index
            out.writeObject("GET_LOAD");
            out.flush();

            // Read load index value from the remote peer
            load = in.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return load;
    }

    public String selectBestPeer(List<String> peerList, double loadWeight) {
        String bestPeer = null;
        double bestScore = Double.MAX_VALUE;

        for (String peer : peerList) {
            String[] parts = peer.split(":");
            String ipAddress = parts[0];
            int port = Integer.parseInt(parts[1]);

            int load = getRemotePeerLoad(ipAddress, port); // Retrieve the load from the remote peer
            int latency = latencyTable.getLatency(this.port, port);

            double score = (1 - loadWeight) * latency + loadWeight * load;

            if (score < bestScore) {
                bestScore = score;
                bestPeer = peer;
            }
        }

        return bestPeer;
    }



}
