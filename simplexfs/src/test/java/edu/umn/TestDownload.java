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

import static edu.umn.utils.Checksum.computeChecksum;
import static org.junit.jupiter.api.Assertions.*;

public class TestDownload {
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
        // Start peer nodes in separate threads

        new Thread(() -> peerNode1.start()).start();
        new Thread(() -> peerNode2.start()).start();
        // Allow time for the peer nodes to start
        Thread.sleep(1000);

    }
    @Test
    public void testDownload() throws Exception {
        URL resourceUrl = getClass().getClassLoader().getResource("files");
        System.out.println(resourceUrl);
        if (resourceUrl == null) {
            throw new RuntimeException("Test resources not found");
        }
        String resourcePath = resourceUrl.getPath();
        // Update file list to the tracking server
        peerNode1.updateFileList();
        peerNode2.updateFileList();

        // Allow time for the server to update the file list
        Thread.sleep(1000);

        // Test findFile
        List<String> peerList = peerNode1.findFile("sample2.txt");
        //make sure findFile returns the correct peer
//        Thread.sleep(500);
        assertEquals(1, peerList.size());
        // Test downloadFile: Peer 1 downloads sample2.txt from Peer 2
        // using peerList1 to download sample2.txt from peerList1.get(0)
        String peerIpAddress = peerList.get(0).split(":")[0];
        System.out.println(peerIpAddress);
        int peerPort = Integer.parseInt(peerList.get(0).split(":")[1]);
        System.out.println(peerPort);
        peerNode1.downloadFile("sample2.txt", peerIpAddress, peerPort);
        // Allow time for the download to complete
        Thread.sleep(1000);
        // Check that the file was downloaded
        boolean fileExists = computeChecksum(Path.of(resourcePath + "/peer1/sample2.txt")).equals(computeChecksum(Path.of(resourcePath + "/peer2/sample2.txt")));
        assertEquals(true, fileExists);

        // Test download non existent file
        String nonExistentFile = "nonExistentTestFile.txt";
        assertThrows(Exception.class, () -> peerNode1.downloadFile(nonExistentFile, peerIpAddress, peerPort), "File not found exception should be thrown");




    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        // Stop the peer nodes
        peerNode1.stop();
        peerNode2.stop();
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

    }

}
