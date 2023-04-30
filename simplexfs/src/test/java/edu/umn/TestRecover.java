package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;
import org.junit.jupiter.api.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestRecover {
    private TrackingServer server;
    private PeerNode peerNode1;
    private PeerNode peerNode2;

    @BeforeEach
    public void setUp() throws InterruptedException {
        // Get the path to the test resources directory
        URL resourceUrl = getClass().getClassLoader().getResource("files");
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

        // Start peer nodes in separate threads
        new Thread(() -> peerNode1.start()).start();
        new Thread(() -> peerNode2.start()).start();
        Thread.sleep(1000);
    }

    @Test
    public void testDebugRecover() throws Exception {
        // Update file list to the tracking server
        peerNode1.updateFileList();
        peerNode2.updateFileList();
        //check fileRegistry in server
        server.printFileRegistry();

        // Make the TrackingServer crash or simulate a crash by stopping the server
        server.stop();
        //allow time for the server to stop
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Update file list for different peers
        // Generate a new file for peer1
        // Get the path to the test resources directory
        URL resourceUrl = getClass().getClassLoader().getResource("files");
        assertNotNull(resourceUrl, "Test resources not found");
        String resourcePath = resourceUrl.getPath();
        Path filePath = Path.of(resourcePath + "/peer1", "newfile.txt");
        try {
            Files.writeString(filePath, "This is a new file");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Update file list for peer1
        peerNode1.initialize();
        // print fileRegistry in peer1
        System.out.println("Peer1 fileRegistry after update:" + peerNode1.getFileChecksums().toString());

        // Restart the server
        new Thread(() -> server.start()).start();
        // Allow time for the server to start accepting connections
        Thread.sleep(1000);
        // Check fileRegistry in server in terminal
        server.printFileRegistry();
        // Check fileRegistry in peer1 including the new file
        // Check file registry in server
        Map<String, TrackingServer.FileInfo> serverFileRegistry = server.getFileRegistry();
        assertNotNull(serverFileRegistry, "Server file registry is null");

    // Check that the file registry contains the new file
            TrackingServer.FileInfo newFileInfo = serverFileRegistry.get("newfile.txt");
            assertNotNull(newFileInfo, "File registry does not contain the new file");

    // Check the checksum of the new file
            String newFileChecksum = newFileInfo.checksum;
            Path newFilePath = Path.of(resourcePath + "/peer1", "newfile.txt");
            String expectedChecksum = computeChecksum(newFilePath);
            assertEquals(expectedChecksum, newFileChecksum, "Checksum of new file is incorrect");

    // Check that the new file is registered only in peer1
            assertEquals(1, newFileInfo.peers.size(), "New file should be registered only in one peer");
            TrackingServer.PeerInfo peerInfo = newFileInfo.peers.values().iterator().next();
            assertEquals("127.0.0.1", peerInfo.getIpAddress(), "New file should be registered only in peer1");
            assertEquals(8001, peerInfo.getPort(), "New file should be registered only in peer1");

    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        // Stop server and peer nodes
        server.stop();
        peerNode1.stop();
        peerNode2.stop();
        // remove newfile.txt
        URL resourceUrl = getClass().getClassLoader().getResource("files");
        assertNotNull(resourceUrl, "Test resources not found");
        String resourcePath = resourceUrl.getPath();
        Path filePath = Path.of(resourcePath + "/peer1", "newfile.txt");
        try {
            Files.delete(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
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