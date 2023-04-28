package edu.umn.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;


public class TrackingServer {
    private final int port;
    private final Map<String, FileInfo> fileRegistry;

    private ServerSocket serverSocket;

    private volatile boolean running;


    public TrackingServer(int port) {
        this.port = port;
        this.fileRegistry = new ConcurrentHashMap<>();
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

//    public synchronized void start() {
//        running = true;
//        System.out.println("Starting server on port " + port);
//        try (ServerSocket serverSocket = new ServerSocket(port)) {
//            this.serverSocket = serverSocket; // Store the reference to the server socket
//            while (running) {
//                try {
//                    Socket socket = serverSocket.accept();
//                    // Handle incoming connections from peer nodes
//                    try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
//                         ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
//
//                        String requestType = (String) inputStream.readObject();
//                        if ("FIND".equals(requestType)) {
//                            String filename = (String) inputStream.readObject();
//                            List<String> peerList = find(filename);
//                            outputStream.writeObject(peerList);
//                        } else if ("UPDATE_LIST".equals(requestType)) {
//                            int peerPort = inputStream.readInt();
//                            @SuppressWarnings("unchecked")
//                            Map<String, String> fileList = (HashMap<String, String>) inputStream.readObject();
//                            // Handle the received file list (peer IP address and port can be obtained from the socket)
//                            receiveFileList(socket.getInetAddress().getHostAddress(), peerPort, fileList);
//
//                        } else {
//                            // Unknown request type
//                            System.out.println("Unknown request type: " + requestType);
//                        }
//                    } catch (IOException | ClassNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                } catch (SocketException e) {
//                    if (running) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
public synchronized void start() {
    running = true;
    System.out.println("Starting server on port " + port);
    try (ServerSocket serverSocket = new ServerSocket(port)) {
        this.serverSocket = serverSocket; // Store the reference to the server socket
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                // Handle incoming connections from peer nodes
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

    public static void main(String[] args) {
        TrackingServer trackingServer = new TrackingServer(8080);
        trackingServer.start();

    }
}
