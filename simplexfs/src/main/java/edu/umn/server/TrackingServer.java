package edu.umn.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class TrackingServer {
    private static final int RECOVERY_TIMEOUT_SECONDS = 10;
    private final int port;
    private final Map<String, FileInfo> fileRegistry;

    private ServerSocket serverSocket;

    private volatile boolean running;

    private final Set<String> knownPeers;

    private final ThreadPoolExecutor executor;

    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(1);

    private final Set<String> peerInfoReceived = ConcurrentHashMap.newKeySet();

    public TrackingServer(int port) {
        this.port = port;
        this.fileRegistry = new ConcurrentHashMap<>();
        this.knownPeers = ConcurrentHashMap.newKeySet();
        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    public Map<String, FileInfo> getFileRegistry() {
        return fileRegistry;
    }

    public static class FileInfo {
        public final String checksum;
        public final Map<String, PeerInfo> peers;

        public FileInfo(String checksum) {
            this.checksum = checksum;
            this.peers = new HashMap<>();
        }
        @Override
        public String toString() {
            return "FileInfo{" +
                    "checksum='" + checksum + '\'' +
                    ", peers=" + peers +
                    '}';
        }
    }

    public static class PeerInfo {
        private final String ipAddress;
        private final int port;


        public PeerInfo(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }
        @Override
        public String toString() {
            return "PeerInfo{" +
                    "ipAddress='" + ipAddress + '\'' +
                    ", port=" + port +
                    '}';
        }

        public int getPort() {
            return port;
        }
        public String getIpAddress() {
            return ipAddress;
        }
    }

    public void printFileRegistry() {
        System.out.println("Updated file registry:");
        for (Map.Entry<String, FileInfo> entry : fileRegistry.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }


    public synchronized void start() {
        running = true;
        System.out.println("Starting server on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket; // Store the reference to the server socket
            // Start to recover server if it recovers from a crash and has known peers
            if (!knownPeers.isEmpty()) {
                recoverServer();
            }
            while (running) {

                try {
                    Socket socket = serverSocket.accept();
                    // Handle incoming connections from peer nodes
                    // Spawn a new thread to handle each connection
                    executor.submit(() -> {
                        try {
                            handleConnection(socket);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

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

    private void handleConnection(Socket socket) throws IOException {
        ObjectInputStream inputStream = null;
        ObjectOutputStream outputStream = null;

        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());

            String requestType = (String) inputStream.readObject();
            if ("FIND".equals(requestType)) {
                String filename = (String) inputStream.readObject();
                List<String> peerList = find(filename);
                outputStream.writeObject(peerList);
            } else if ("UPDATE_LIST".equals(requestType)) {
                int peerPort = inputStream.readInt();
                @SuppressWarnings("unchecked")
                Map<String, String> fileList = (HashMap<String, String>) inputStream.readObject();
                // Handle the received file list (peer IP address and port can be obtained from the socket)
                receiveFileList(socket.getInetAddress().getHostAddress(), peerPort, fileList);
                // Send response to the client
                outputStream.writeObject("UPDATE_LIST_SUCCESS" + ":" + peerPort);

            } else if ("RECOVER_SERVER_RESPONSE".equals(requestType)) {
                int peerPort = inputStream.readInt();
                @SuppressWarnings("unchecked")
                Map<String, String> fileList = (HashMap<String, String>) inputStream.readObject();
                // Handle the received file list (peer IP address and port can be obtained from the socket)
                receiveFileList(socket.getInetAddress().getHostAddress(), peerPort, fileList);
                outputStream.writeObject("RECOVER_LIST_SUCCESS");
                // Mark the peer as having sent its file list
                peerInfoReceived.add(socket.getInetAddress().getHostAddress() + ":" + peerPort);
            } else {
                // Unknown request type
                System.out.println("Unknown request type: " + requestType);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    public void stop() {
        running = false;
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            serverSocket.close(); // Close the server socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private List<String> find(String filename) {
        // Returns the list of peers that store the specified file and the file's checksum
        FileInfo fileInfo = fileRegistry.get(filename);
        if (fileInfo != null) {
            List<String> peerList = new ArrayList<>();
            for (PeerInfo peerInfo : fileInfo.peers.values()) {
                String peerIpPortCheckSum = peerInfo.ipAddress + ":" + peerInfo.port + ":" + fileInfo.checksum;
                peerList.add(peerIpPortCheckSum);
            }
            return peerList;
        }
        return new ArrayList<>();
    }

    private void receiveFileList(String ipAddress, int port, Map<String, String> fileList) {
        // Debug: Print the received file list
        System.out.println("Received file list: " + fileList);

        // Receives and updates the file list and checksums for the specified peer
        PeerInfo peerInfo = new PeerInfo(ipAddress, port);
        for (Map.Entry<String, String> entry : fileList.entrySet()) {
            String filename = entry.getKey();
            String checksum = entry.getValue();
            FileInfo fileInfo = fileRegistry.get(filename);
            if (fileInfo == null) {
                fileInfo = new FileInfo(checksum);
                fileRegistry.put(filename, fileInfo);
            }
            fileInfo.peers.put(peerInfo.ipAddress + ":" + port, peerInfo);
        }

        // Debug: Print the updated fileRegistry
        System.out.println("Updated file registry: " + fileRegistry);
        // Register the peer if it is not already registered
        registerPeer(ipAddress, port);
    }

    private void updateList(String ipAddress, int port, Map<String, String> fileList) {
        // Updates the list of files stored by the specified peer
        // Remove the old file list associated with this peer
        for (FileInfo fileInfo : fileRegistry.values()) {
            fileInfo.peers.remove(ipAddress);
        }

        // Add the updated file list for this peer
        receiveFileList(ipAddress, port, fileList);
    }

    public void registerPeer(String ipAddress, int port) {
        knownPeers.add(ipAddress + ":" + port);
    }


    public void broadcastRequest() {
        for (String peerAddress : knownPeers) {
            String[] addressParts = peerAddress.split(":");
            String ipAddress = addressParts[0];
            int port = Integer.parseInt(addressParts[1]);
            System.out.println("Sending RECOVER_SERVER request to " + peerAddress);
            // Send RECOVER_SERVER request to the peer
            try (Socket socket = new Socket(ipAddress, port);
                 ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

                outputStream.writeObject("RECOVER_SERVER");
                String response = (String) inputStream.readObject();
                int peerPort = inputStream.readInt();
                Map<String, String> receivedFileChecksums = (Map<String, String>) inputStream.readObject();
                if ("RECOVER_SERVER_RESPONSE".equals(response)) {
                    // Handle received data
                    receiveFileList(ipAddress, peerPort, receivedFileChecksums);

                    // Send the success message back
                    outputStream.writeObject("RECOVER_LIST_SUCCESS");
                    peerInfoReceived.add(peerAddress);
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            timeoutExecutor.schedule(() -> {
                // Check if all known peers have responded
                // If not remove unresponsive peers from the list of known peers
                if (!peerInfoReceived.contains(peerAddress)) {
                    System.out.println("Peer " + peerAddress + " did not respond to RECOVER_SERVER request");
                    knownPeers.remove(peerAddress);
                    fileRegistry.values().forEach(fileInfo -> fileInfo.peers.remove(peerAddress));
                }
            }, RECOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    // Initiate the recovery process
    private void recoverServer() {
        System.out.println("Recovering server state...");
        broadcastRequest();
        try {
            TimeUnit.SECONDS.sleep(RECOVERY_TIMEOUT_SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // if no arguments are provided, start the server on port 8080, else use the provided port
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        TrackingServer trackingServer = new TrackingServer(port);
        // Start the tracking server in a separate thread
        new Thread(trackingServer::start).start();
    }
}
