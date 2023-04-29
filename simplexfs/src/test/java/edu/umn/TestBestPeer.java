package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;
import edu.umn.utils.LatencyTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static edu.umn.utils.Checksum.computeChecksum;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBestPeer {
    private TrackingServer server;
    private PeerNode peerNode1;
    private PeerNode peerNode2;
    private PeerNode peerNode3;
    private PeerNode peerNode4;
    private PeerNode peerNode5;

    private LatencyTable latencyTable;

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
        // The latency file is used to simulate network latency between peers
        // For now we just use latency test best peer selection and will not simulate latency
        // set random seed to keep the same random values
        int seed = 5105;
        //create a latency table
        LatencyTable latencyTable = new LatencyTable(latencyFilePath);
        //generate fake latency file
        latencyTable.generateFakeLatencyFile(latencyFilePath, 8001, 8005, seed);
        latencyTable.readLatencyFile(latencyFilePath);
        //print latency table
        System.out.println(latencyTable.getLatencyMap());

        // Display the latency table

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
        // Start peer nodes in separate threads

        new Thread(() -> peerNode1.start()).start();
        new Thread(() -> peerNode2.start()).start();
        new Thread(() -> peerNode3.start()).start();
        new Thread(() -> peerNode4.start()).start();
        new Thread(() -> peerNode5.start()).start();
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
        // Set the weights for the peer selection algorithm
        double[] loadWeights = {0.5, 0.5, 0.5, 0.5, 0.5};
        // Test findFile
        List<String> peerList = peerNode2.findFile("test10Mb.db");
        // Peer 2 try to download test10Mb.db from peer1
        peerNode2.downloadFile("test10Mb.db", peerList, loadWeights[1]);
        // Allow time for the download to complete
        Thread.sleep(1000);
        // Check that the file was downloaded successfully
        boolean fileExists12 = computeChecksum(Path.of(resourcePath + "/peer2/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")));
        assertEquals(true, fileExists12);
        // Peer3 try to download test10Mb.db from peer1 and peer2
        peerList = peerNode3.findFile("test10Mb.db");
        peerNode3.downloadFile("test10Mb.db", peerList, loadWeights[2]);
        // Allow time for the download to complete
        Thread.sleep(1000);
        // Check that the file was downloaded successfully
        boolean fileExists13 = computeChecksum(Path.of(resourcePath + "/peer3/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")));
        assertEquals(true, fileExists13);
        // Peer 3 will finally download test10Mb.db from peer1 since latency is the lowest which is 2007ms

        // Peer4 try to download test10Mb.db from peer1, peer2 and peer3
        peerList = peerNode4.findFile("test10Mb.db");
        peerNode4.downloadFile("test10Mb.db", peerList, loadWeights[3]);
        // Allow time for the download to complete
        Thread.sleep(1000);
        // Check that the file was downloaded successfully
        boolean fileExists14 = computeChecksum(Path.of(resourcePath + "/peer4/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")));
        assertEquals(true, fileExists14);
        // Peer4 will finally download test10Mb.db from peer3 since latency is the lowest which is 418ms

        // Peer5 try to download test10Mb.db from peer1, peer2, peer3 and peer4
        peerList = peerNode5.findFile("test10Mb.db");
        peerNode5.downloadFile("test10Mb.db", peerList, loadWeights[4]);
        // Allow time for the download to complete
        Thread.sleep(1000);
        // Check that the file was downloaded successfully
        boolean fileExists15 = computeChecksum(Path.of(resourcePath + "/peer5/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")));
        assertEquals(true, fileExists15);
        // Peer5 will finally download test10Mb.db from peer2 since latency is the lowest which is 116ms

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
        Path path = Path.of(resourcePath + "/peer2/test10Mb.db");
        path.toFile().delete();
        path = Path.of(resourcePath + "/peer3/test10Mb.db");
        path.toFile().delete();
        path = Path.of(resourcePath + "/peer4/test10Mb.db");
        path.toFile().delete();
        path = Path.of(resourcePath + "/peer5/test10Mb.db");
        path.toFile().delete();
    }

}
