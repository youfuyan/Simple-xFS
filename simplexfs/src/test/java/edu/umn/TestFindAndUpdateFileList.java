package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFindAndUpdateFileList {
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
//        Thread.sleep(1000);

    }

    @Test
    public void testFindAndUpdateFileList() throws InterruptedException {
        // Update file list to the tracking server
        peerNode1.updateFileList();
        peerNode2.updateFileList();

        // Allow time for the server to update the file list
        Thread.sleep(1000);

        // Test findFile
        List<String> peerList = peerNode1.findFile("sample2.txt");
        //make sure findFile returns the correct peer
        assertEquals(1, peerList.size());
        // Test find non-exist file
        List<String> peerList2 = peerNode2.findFile("sample3.txt");
        //make sure findFile returns the correct peer
        assertEquals(0, peerList2.size());
        // Test server update file list
        // make sure server has the correct file list
        // Test server update file list
        // Make sure server has the correct file list
        assertEquals(2, server.getFileRegistry().size());

//        Thread.sleep(1000);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        // Stop server and peer nodes
        server.stop();
        peerNode1.stop();
        peerNode2.stop();
        Thread.sleep(1000);
    }
}
