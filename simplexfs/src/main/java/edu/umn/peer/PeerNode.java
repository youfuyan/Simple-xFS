package edu.umn.peer;

import edu.umn.utils.LatencyTable;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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


public class PeerNode {
    private String fileDirectory;
    private int port;
    private Map<String, String> fileChecksums;
    private LatencyTable latencyTable;
    private int loadIndex;
    private ThreadPoolExecutor executor;

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
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                // Handle incoming connections from other peers and the server
                // Spawn a new thread to handle each connection
                executor.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void handleConnection(Socket socket) {
        // Handle communication with other peers and the tracking server
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
            List<String> peerList = (ArrayList<String>) inputStream.readObject();
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
