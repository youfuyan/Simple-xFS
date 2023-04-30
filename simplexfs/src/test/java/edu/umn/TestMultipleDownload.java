package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static edu.umn.utils.Checksum.computeChecksum;
import static org.junit.jupiter.api.Assertions.*;

public class TestMultipleDownload {
    private TrackingServer server;
    private PeerNode peerNode1;
    private PeerNode peerNode2;
    private PeerNode peerNode3;
    private PeerNode peerNode4;
    private PeerNode peerNode5;
    @BeforeEach
    public void setUp() throws InterruptedException {
        // Get the path to the test resources directory
        URL resourceUrl = getClass().getClassLoader().getResource("files");
        System.out.println(resourceUrl);
        if (resourceUrl == null) {
            throw new RuntimeException("Test resources not found");
        }
        String resourcePath = resourceUrl.getPath();
        String latencyFilePath = resourcePath + "/latency.txt"; // Update this path as needed

        // Start the tracking server
        server = new TrackingServer(8080);
        new Thread(() -> server.start()).start();
        // Allow time for the server to start accepting connections
        Thread.sleep(1000);


        // Initialize peer nodes using the test resources directory
        peerNode1 = new PeerNode(resourcePath + "/peer1", 8001, latencyFilePath,"localhost",8080);
        peerNode1.initialize();
        peerNode2 = new PeerNode(resourcePath + "/peer2", 8002, latencyFilePath,"localhost",8080);
        peerNode2.initialize();
        peerNode3 = new PeerNode(resourcePath + "/peer3", 8003, latencyFilePath,"localhost",8080);
        peerNode3.initialize();
        peerNode4 = new PeerNode(resourcePath + "/peer4", 8004, latencyFilePath,"localhost",8080);
        peerNode4.initialize();
        peerNode5 = new PeerNode(resourcePath + "/peer5", 8005, latencyFilePath,"localhost",8080);
        peerNode5.initialize();

        // Start peer nodes in separate threads

        new Thread(() -> peerNode1.start()).start();
        new Thread(() -> peerNode2.start()).start();
        new Thread(() -> peerNode3.start()).start();
        new Thread(() -> peerNode4.start()).start();
        new Thread(() -> peerNode5.start()).start();
        // Allow time for the peer nodes to start
        Thread.sleep(5000);





    }
    @Test
    public void testMultipleDownload() throws Exception {
        URL resourceUrl = getClass().getClassLoader().getResource("files");
        System.out.println(resourceUrl);
        if (resourceUrl == null) {
            throw new RuntimeException("Test resources not found");
        }
        String resourcePath = resourceUrl.getPath();

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

        Thread.sleep(1000);
        // Check that the file was downloaded
        boolean fileExists = computeChecksum(Path.of(resourcePath + "/peer1/sample2.txt")).equals(computeChecksum(Path.of(resourcePath + "/peer2/sample2.txt")));
        assertEquals(true, fileExists);
        boolean fileExists2 = computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer2/test10Mb.db")));
        assertEquals(true, fileExists2);
        boolean fileExists3 = computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer3/test10Mb.db")));
        assertEquals(true, fileExists3);
        boolean fileExists4 = computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer4/test10Mb.db")));
        assertEquals(true, fileExists4);
        boolean fileExists5 = computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer5/test10Mb.db")));
        assertEquals(true, fileExists5);


    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        // Stop the peer nodes
        peerNode1.stop();
        peerNode2.stop();
        peerNode3.stop();
        peerNode4.stop();
        peerNode5.stop();
        // Stop the tracking server
        server.stop();
        Thread.sleep(1000);
        // remove the downloaded files from peer 1
        URL resourceUrl = getClass().getClassLoader().getResource("files");
        System.out.println(resourceUrl);
        if (resourceUrl == null) {
            throw new RuntimeException("Test resources not found");
        }
        String resourcePath = resourceUrl.getPath();
        Path path = Path.of(resourcePath + "/peer1/sample2.txt");
        path.toFile().delete();
        Path path2 = Path.of(resourcePath + "/peer1/test10Mb.db");
        path2.toFile().delete();
        Path path3 = Path.of(resourcePath + "/peer1/test10Mb.db");
        path3.toFile().delete();
        Path path4 = Path.of(resourcePath + "/peer1/test10Mb.db");
        path4.toFile().delete();
        Path path5 = Path.of(resourcePath + "/peer1/test10Mb.db");

    }

}
