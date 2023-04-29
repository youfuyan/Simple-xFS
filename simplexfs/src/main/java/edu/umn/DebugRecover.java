package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;


import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

public class DebugRecover {
    public static void main(String[] args) {
        // Get the path to the test resources directory
        URL resourceUrl = debug.class.getClassLoader().getResource("files");
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

        // Start peer nodes in separate threads
        new Thread(() -> peerNode1.start()).start();
        new Thread(() -> peerNode2.start()).start();
        // Update file list to the tracking server
        peerNode1.updateFileList();
        peerNode2.updateFileList();
        //check fileRegistry in server
        server.printFileRegistry();


        //Make the TrackingServer crash or simulate a crash by stopping the server
        server.stop();
        //allow time for the server to stop
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Update file list for different peers
        // Generate a new file for peer1
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

        //Restart the server
        new Thread(() -> server.start()).start();
        //allow time for the server to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //check fileRegistry in server
        server.printFileRegistry();


       //stop
        peerNode1.stop();
        peerNode2.stop();
        server.stop();

    }
}
