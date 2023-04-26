package edu.umn.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;


public class TrackingServer {
    private int port;
    private Map<String, FileInfo> fileRegistry;

    public TrackingServer(int port) {
        this.port = port;
        this.fileRegistry = new ConcurrentHashMap<>();
    }

    private static class FileInfo {
        private String checksum;
        private Map<String, PeerInfo> peers;

        public FileInfo(String checksum) {
            this.checksum = checksum;
            this.peers = new HashMap<>();
        }
    }

    private static class PeerInfo {
        private String ipAddress;
        private int port;

        public PeerInfo(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }
    }
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                // Handle incoming connections from peer nodes
                try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                     ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

                    String requestType = (String) inputStream.readObject();
                    if ("FIND".equals(requestType)) {
                        String filename = (String) inputStream.readObject();
                        List<String> peerList = find(filename);
                        outputStream.writeObject(peerList);
                    } else if ("UPDATE_LIST".equals(requestType)) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> fileList = (HashMap<String, String>) inputStream.readObject();
                        // Handle the received file list (peer IP address and port can be obtained from the socket)
                        receiveFileList(socket.getInetAddress().getHostAddress(), socket.getPort(), fileList);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
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
                String peerAddress = peerInfo.ipAddress + ":" + peerInfo.port;
                peerList.add(peerAddress);
            }
            return peerList;
        }
        return new ArrayList<>();
    }

    private void receiveFileList(String ipAddress, int port, Map<String, String> fileList) {
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
            fileInfo.peers.put(peerInfo.ipAddress, peerInfo);
        }
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

}