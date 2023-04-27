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


public class PeerNode {
    private String fileDirectory;
    private int port;
    private Map<String, String> fileChecksums;
    private LatencyTable latencyTable;
    private int loadIndex;
    private ThreadPoolExecutor executor;

    private ServerSocket serverSocket;

    private volatile boolean running;

    // Define constants for max retries and buffer size for file transfer
    private static final int MAX_RETRIES = 3;
    private static final int BUFFER_SIZE = 4096;

    public PeerNode(String fileDirectory, int port, String latencyFilePath) {
        this.fileDirectory = fileDirectory;
        this.port = port;
        this.fileChecksums = new HashMap<>();
        this.latencyTable = new LatencyTable(latencyFilePath);
        this.loadIndex = 0;
        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }
    public Map<String, String> getFileChecksums() {
        return fileChecksums;
    }

    public void initialize() {
        // Scan the file directory and compute checksums
        try {
            Files.walk(Path.of(fileDirectory))
                    .filter(Files::isRegularFile)
                    .forEach(this::computeChecksum);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Debug: Print the fileChecksums map
        System.out.println("File checksums: " + fileChecksums);
        // Debug: Print the file names in the fileChecksums map
        System.out.println("File names: " + fileChecksums.keySet());

    }


    private void computeChecksum(Path filePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] checksum = md.digest(fileBytes);
            fileChecksums.put(filePath.getFileName().toString(), bytesToHex(checksum));
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
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
            if ("FIND".equals(requestType)) {
                // Handle FIND request from other peers or the tracking server
//                handleFindRequest(inputStream, outputStream);
            } else if ("UPDATE_LIST".equals(requestType)) {
                // Handle UPDATE_LIST request from other peers or the tracking server
//                handleUpdateListRequest(inputStream);
            } else {
                // Unknown request type
                //
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

//    private void handleFindRequest(ObjectInputStream inputStream, ObjectOutputStream outputStream) throws IOException {
////        try {
////            String filename = (String) inputStream.readObject();
////            List<String> peerList = findFile(filename, serverIpAddress, serverPort);
////            outputStream.writeObject(peerList);
////        } catch (ClassNotFoundException e) {
////            e.printStackTrace();
////        }
////    }

//    private void handleUpdateListRequest(ObjectInputStream inputStream) throws IOException {
//        try {
//            @SuppressWarnings("unchecked")
//            Map<String, String> fileList = (Map<String, String>) inputStream.readObject();
//            updateFileListFromPeers(fileList);
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    private void updateFileListFromPeers(Map<String, String> fileList) {
        // Update the fileChecksums map with the received file list from other peers
        // ...
    }

    public List<String> findFile(String filename, String serverIpAddress, int serverPort) {
        // Sends a request to the tracking server to get a list of nodes that store the specified file
        try (Socket socket = new Socket(serverIpAddress, serverPort);
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
        return loadIndex;
    }
    private void updateLoadIndex(int delta) {
        // Update the load index by delta
        loadIndex += delta;
    }

    public void downloadFile(String filename, String peerIpAddress, int peerPort) {
        // Download the specified file from the specified peer
        // ...
    }

    public void updateFileList(String serverIpAddress, int serverPort) {
        // Updates the list of files stored in the specific directory and sends the updated list to the tracking server
        try (Socket socket = new Socket(serverIpAddress, serverPort);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

            // Send request
            outputStream.writeObject("UPDATE_LIST");
            outputStream.writeObject(fileChecksums);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
