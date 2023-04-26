package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
public class testChecksumAndCommunication {
    private TrackingServer server;
    private PeerNode peerNode1;
    private PeerNode peerNode2;

    @BeforeEach
    public void setUp() throws InterruptedException {
        // Get the path to the test resources directory
        URL resourceUrl = getClass().getClassLoader().getResource("files");
        System.out.println(resourceUrl);
        if (resourceUrl == null) {
            throw new RuntimeException("Test resources not found");
        }
        String resourcePath = resourceUrl.getPath();
        String latencyFilePath = resourcePath + "/latency.csv"; // Update this path as needed

        // Start the tracking server
        server = new TrackingServer(8080);
        new Thread(() -> server.start()).start();

        // Allow time for the server to start accepting connections
        Thread.sleep(1000);

        // Initialize peer nodes using the test resources directory
        peerNode1 = new PeerNode(resourcePath + "/peer1", 8001, latencyFilePath);
        peerNode1.initialize();
        peerNode2 = new PeerNode(resourcePath + "/peer2", 8002, latencyFilePath);
        peerNode2.initialize();

        // Start peer nodes in separate threads
        // Start peer nodes in separate threads
        Thread.sleep(1000); // Introduce a delay before starting the peer nodes
        new Thread(() -> peerNode1.start()).start();
        new Thread(() -> peerNode2.start()).start();
        // Allow time for the peer nodes to start
        Thread.sleep(1000);

    }

    @Test
    public void testChecksumAndCommunication() throws Exception {
        // Update file list to the tracking server
        peerNode1.updateFileList("localhost", 8080);
        peerNode2.updateFileList("localhost", 8080);

        // Allow time for the server to update the file list
        Thread.sleep(1000);

        // Test findFile
        List<String> peerList1 = peerNode1.findFile("sample1.txt", "localhost", 8080);
        List<String> peerList2 = peerNode1.findFile("sample2.txt", "localhost", 8080);

        // Verify that only peerNode1 has "sample1.txt" and only peerNode2 has "sample2.txt"
        assertEquals(1, peerList1.size());
        assertEquals(1, peerList2.size());


        // Verify that the computed checksum matches the actual checksum of "sample1.txt"
        String computedChecksum1 = peerNode1.getFileChecksums().get("sample1.txt");
        String actualChecksum1 = computeChecksum(Path.of(getClass().getClassLoader().getResource("files/peer1/sample1.txt").toURI()));
        assertEquals(computedChecksum1, actualChecksum1);

        // Verify that the computed checksum matches the actual checksum of "sample2.txt"
        String computedChecksum2 = peerNode2.getFileChecksums().get("sample2.txt");
        String actualChecksum2 = computeChecksum(Path.of(getClass().getClassLoader().getResource("files/peer2/sample2.txt").toURI()));
        assertEquals(computedChecksum2, actualChecksum2);
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


    @AfterEach
    public void tearDown() throws InterruptedException {
        // Stop server and peer nodes
        server.stop();
        peerNode1.stop();
        peerNode2.stop();

    }
}
