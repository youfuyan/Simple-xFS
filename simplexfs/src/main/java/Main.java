import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;


import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
class Main {
    public static void main(String[] args) throws InterruptedException {
        // Get the path to the test resources directory
        String homeDirectory = System.getProperty("user.home");
        String resourcePath = homeDirectory + "/Desktop/UMN/2023Spring/CSCI5105/5105-P3/simplexfs/src/test/resources/files";

        System.out.println(resourcePath);
        if (resourcePath == null) {
            throw new RuntimeException("Test resources not found");
        }
        String latencyFilePath = resourcePath + "/latency.csv"; // Update this path as needed

        // Start the tracking server
        TrackingServer server = new TrackingServer(8080);
        new Thread(() -> server.start()).start();

        // Initialize peer nodes using the test resources directory
        PeerNode peerNode1 = new PeerNode(resourcePath + "/peer1", 8001, latencyFilePath);
        peerNode1.initialize();
        PeerNode peerNode2 = new PeerNode(resourcePath + "/peer2", 8002, latencyFilePath);
        peerNode2.initialize();

        // Start peer nodes in separate threads
        new Thread(() -> peerNode1.start()).start();
        new Thread(() -> peerNode2.start()).start();

        // Update file list to the tracking server
        peerNode1.updateFileList("localhost", 8080);
        peerNode2.updateFileList("localhost", 8080);

        // Allow time for the server to update the file list
        Thread.sleep(1000);

        // Test findFile
        List<String> peerList1 = peerNode1.findFile("sample1.txt", "localhost", 8080);
        List<String> peerList2 = peerNode1.findFile("sample2.txt", "localhost", 8080);

        //print the peerList
        System.out.println("peerList1: " + peerList1);
        System.out.println("peerList2: " + peerList2);

        //check fileRegistry in server
        System.out.println("fileRegistry: " + server.getFileRegistry());
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
