package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;


import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
public class debug {
    public static void main(String[] args) throws Exception {
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

        // Allow time for the server to update the file list
        Thread.sleep(1000);

        // Test findFile
        List<String> peerList1 = peerNode1.findFile("sample2.txt");
        List<String> peerList2 = peerNode2.findFile("sample1.txt");

        //print the peerList
        System.out.println("peerList1: " + peerList1);
        System.out.println("peerList2: " + peerList2);

        //check fileRegistry in server
        server.printFileRegistry();
//        System.out.println("fileRegistry: " + server.getFileRegistry());


        // Test downloadFile
        //using peerList1 to download sample2.txt from peerList1.get(0)
        String peerIpAddress = peerList1.get(0).split(":")[0];
        System.out.println(peerIpAddress);
        int peerPort = Integer.parseInt(peerList1.get(0).split(":")[1]);
        System.out.println(peerPort);
        peerNode1.downloadFile("sample2.txt", peerIpAddress, peerPort);
        //using peerList2 to download sample1.txt from peerList2.get(0)
        String peerIpAddress2 = peerList2.get(0).split(":")[0];
        System.out.println(peerIpAddress2);
        int peerPort2 = Integer.parseInt(peerList2.get(0).split(":")[1]);
        System.out.println(peerPort2);
        peerNode2.downloadFile("sample1.txt", peerIpAddress2, peerPort2);

        // Allow time for the file transfer to complete
        Thread.sleep(1000);
        //check the file in peer1
        System.out.println("sample2.txt in peer1: " + computeChecksum(Path.of(resourcePath + "/peer1/sample2.txt")));
        if (computeChecksum(Path.of(resourcePath + "/peer1/sample2.txt")).equals(computeChecksum(Path.of(resourcePath + "/peer2/sample2.txt")))) {
            System.out.println("sample2.txt in peer1 is the same as sample2.txt in peer2");
        } else {
            System.out.println("sample2.txt in peer1 is not the same as sample2.txt in peer2");
        }
        //check the file in peer2
        System.out.println("sample1.txt in peer2: " + computeChecksum(Path.of(resourcePath + "/peer2/sample1.txt")));
        if (computeChecksum(Path.of(resourcePath + "/peer1/sample1.txt")).equals(computeChecksum(Path.of(resourcePath + "/peer2/sample1.txt")))) {
            System.out.println("sample1.txt in peer1 is the same as sample1.txt in peer2");
        } else {
            System.out.println("sample1.txt in peer1 is not the same as sample1.txt in peer2");
        }

        // Stop the peer nodes
        peerNode1.stop();
        peerNode2.stop();
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
