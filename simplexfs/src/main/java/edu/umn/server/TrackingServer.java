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

    // Add a set to store known peers
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

    private static class FileInfo {
        private final String checksum;
        private final Map<String, PeerInfo> peers;

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

    private static class PeerInfo {
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
        if (!knownPeers.isEmpty()) {
            recoverServer();
        }
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket; // Store the reference to the server socket
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
        // after server recovery, broadcast to all known peers to recover server fileRegistry

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
                outputStream.writeObject("UPDATE_LIST_SUCCESS");

            } else if ("RECOVER_SERVER_RESPONSE".equals(requestType)) {
                int peerPort = inputStream.readInt();
                @SuppressWarnings("unchecked")
                Map<String, String> fileList = (HashMap<String, String>) inputStream.readObject();
                // Handle the received file list (peer IP address and port can be obtained from the socket)
                receiveFileList(socket.getInetAddress().getHostAddress(), peerPort, fileList);
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


    public void broadcastSendInfoRequest() {
        for (String peerAddress : knownPeers) {
            String[] addressParts = peerAddress.split(":");
            String ipAddress = addressParts[0];
            int port = Integer.parseInt(addressParts[1]);
            System.out.println("Sending UPDATE_LIST request to " + peerAddress);
            // Send RECOVER_SERVER request to the peer
            try (Socket socket = new Socket(ipAddress, port);
                 ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
                outputStream.writeObject("RECOVER_SERVER");
            } catch (IOException e) {
                e.printStackTrace();
            }
            timeoutExecutor.schedule(() -> {
                // Check if all known peers have responded
                // If not, perform necessary actions (e.g., remove unresponsive peers)
                if (!peerInfoReceived.contains(peerAddress)) {
                    System.out.println("Peer " + peerAddress + " did not respond to RECOVER_SERVER request");
                    knownPeers.remove(peerAddress);
                    fileRegistry.values().forEach(fileInfo -> fileInfo.peers.remove(peerAddress));
                }
            }, RECOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    // Add a new method to initiate the recovery process
    private void recoverServer() {
        System.out.println("Recovering server state...");
        broadcastSendInfoRequest();
        try {
            TimeUnit.SECONDS.sleep(RECOVERY_TIMEOUT_SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        TrackingServer trackingServer = new TrackingServer(8080);
        trackingServer.start();

    }
}
