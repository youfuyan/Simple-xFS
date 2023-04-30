package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;


import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulateMultipleDownloadSend {
    public static void main(String[] args) throws Exception {


        // Get the path to the test resources directory
        URL resourceUrl = SimulateBasicOperation.class.getClassLoader().getResource("files");
        System.out.println(resourceUrl);
        if (resourceUrl == null) {
            throw new RuntimeException("Test resources not found");
        }
        String resourcePath = resourceUrl.getPath();
        String latencyFilePath = resourcePath + "/latency.txt"; // Update this path as needed

        // Start the tracking server
        TrackingServer server = new TrackingServer(8080);
        new Thread(() -> server.start()).start();

        // Initialize peer nodes using the test resources directory
        PeerNode peerNode1 = new PeerNode(resourcePath + "/peer1", 8001, latencyFilePath,"localhost",8080);
        peerNode1.initialize();
        PeerNode peerNode2 = new PeerNode(resourcePath + "/peer2", 8002, latencyFilePath,"localhost",8080);
        peerNode2.initialize();
        PeerNode peerNode3 = new PeerNode(resourcePath + "/peer3", 8003, latencyFilePath,"localhost",8080);
        peerNode3.initialize();
        PeerNode peerNode4 = new PeerNode(resourcePath + "/peer4", 8004, latencyFilePath,"localhost",8080);
        peerNode4.initialize();
        PeerNode peerNode5 = new PeerNode(resourcePath + "/peer5", 8005, latencyFilePath,"localhost",8080);
        peerNode5.initialize();


        // Start peer nodes in separate threads
        new Thread(() -> peerNode1.start()).start();
        new Thread(() -> peerNode2.start()).start();
        new Thread(() -> peerNode3.start()).start();
        new Thread(() -> peerNode4.start()).start();
        new Thread(() -> peerNode5.start()).start();
        // Allow time for the peers to start
        Thread.sleep(5000);



        // Update file list to the tracking server
        peerNode1.updateFileList();
        peerNode2.updateFileList();
        peerNode3.updateFileList();
        peerNode4.updateFileList();
        peerNode5.updateFileList();

        // Allow time for the server to update the file list
        Thread.sleep(1000);

        // Create an ExecutorService to manage download threads
        ExecutorService downloadExecutor = Executors.newCachedThreadPool();

        downloadExecutor.submit(() -> {
            try {
                List<String> peerList1 = peerNode1.findFile("sample2.txt");
                peerNode1.downloadFile("sample2.txt", peerList1, 0.5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        downloadExecutor.submit(() -> {
            try {
                List<String> peerList2  = peerNode2.findFile("test10Mb.db");
                peerNode2.downloadFile("test10Mb.db", peerList2, 0.5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        downloadExecutor.submit(() -> {
            try {
                List<String> peerList3  = peerNode3.findFile("test10Mb.db");
                peerNode3.downloadFile("test10Mb.db", peerList3, 0.5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        downloadExecutor.submit(() -> {
            try {
                List<String> peerList4  = peerNode4.findFile("test10Mb.db");
                peerNode4.downloadFile("test10Mb.db", peerList4, 0.5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        downloadExecutor.submit(() -> {
            try {
                List<String> peerList5  = peerNode5.findFile("test10Mb.db");
                peerNode5.downloadFile("test10Mb.db", peerList5, 0.5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Allow time for the downloads to complete
        downloadExecutor.shutdown();
        try {
            // Wait for all downloads to complete before stopping the peer nodes and the tracking server
            if (!downloadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            downloadExecutor.shutdownNow();
        }

        // Stop the peer nodes
        peerNode1.stop();
        peerNode2.stop();
        peerNode3.stop();
        peerNode4.stop();
        peerNode5.stop();
        // Stop the tracking server
        server.stop();
    }

    private static String computeChecksum(Path filePath) throws Exception {
        byte[] fileBytes = Files.readAllBytes(filePath);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] checksum = md.digest(fileBytes);
        return bytesToHex(checksum);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
