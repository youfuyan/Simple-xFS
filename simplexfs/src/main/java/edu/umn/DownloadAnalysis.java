package edu.umn;

import edu.umn.peer.PeerNode;
import edu.umn.server.TrackingServer;
import edu.umn.utils.LatencyTable;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static edu.umn.utils.Checksum.computeChecksum;

public class DownloadAnalysis {
    public static void main(String[] args) throws Exception {

        // Get the path to the test resources directory
        URL resourceUrl = SimulateBasicOperation.class.getClassLoader().getResource("files");
        System.out.println(resourceUrl);
        if (resourceUrl == null) {
            throw new RuntimeException("Test resources not found");
        }
        String resourcePath = resourceUrl.getPath();
        String latencyFilePath = resourcePath + "/latency.txt"; // Update this path as needed

        //create a latency table
        LatencyTable latencyTable = new LatencyTable(latencyFilePath);
        //generate fake latency file
        latencyTable.readLatencyFile(latencyFilePath);

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

        // Update file list to the tracking server
        peerNode1.updateFileList();
        peerNode2.updateFileList();
        peerNode3.updateFileList();
        peerNode4.updateFileList();
        peerNode5.updateFileList();

        // Initialize necessary variables
        int numDownloads = 20;
        String fileName = "test10Mb.db";
        ArrayList<Long> downloadTimes = new ArrayList<>();
        ArrayList<Integer> latencies_peer2_1 = new ArrayList<>();
        ArrayList<Integer> latencies_peer2_3 = new ArrayList<>();
        ArrayList<Integer> latencies_peer2_4 = new ArrayList<>();
        ArrayList<Integer> latencies_peer2_5 = new ArrayList<>();
        ArrayList<Boolean> downloadSuccess = new ArrayList<>();
        ArrayList<String> bestPeers = new ArrayList<>();
        // Transfer test file to peer3, peer4, peer5 first
        List<String> peerList = peerNode3.findFile(fileName);
        peerNode3.downloadFile(fileName, peerList, 0.5);
        peerList = peerNode4.findFile(fileName);
        peerNode4.downloadFile(fileName, peerList, 0.5);
        peerList = peerNode5.findFile(fileName);
        peerNode5.downloadFile(fileName, peerList, 0.5);
        latencyTable.addOrUpdateLatency(8002, 8002, 0);

        for (int i = 0; i < numDownloads; i++) {
            // Simulate latency for peers
            // Random latency between 100ms and 5000ms
            Random random1 = new Random();
            Random random3 = new Random();
            Random random4 = new Random();
            Random random5 = new Random();
            int latency21 = random1.nextInt(4900) + 100;
            int latency23 = random3.nextInt(4900) + 100;
            int latency24 = random4.nextInt(4900) + 100;
            int latency25 = random5.nextInt(4900) + 100;
            peerNode2.updateLatencyTable(8001, latency21);
            peerNode2.updateLatencyTable(8003, latency23);
            peerNode2.updateLatencyTable(8004, latency24);
            peerNode2.updateLatencyTable(8005, latency25);
            // add latency to the list
            latencies_peer2_1.add(latency21);
            latencies_peer2_3.add(latency23);
            latencies_peer2_4.add(latency24);
            latencies_peer2_5.add(latency25);

            // Download file
            long downloadStartTime = System.currentTimeMillis();
            List<String> peerList2 = peerNode2.findFile(fileName);
            String bestPeer =  peerNode2.downloadFile(fileName, peerList2, 0);
            int bestPort = Integer.parseInt(bestPeer.split(":")[1]);
            // Simulate latency
            int latency = latencyTable.getLatency(8002, bestPort);
            Thread.sleep(latency);
            long downloadEndTime = System.currentTimeMillis();
            // add best peer to the list
            String peerID = bestPeer.split(":")[1].substring(3);
            bestPeers.add(peerID);
            if (computeChecksum(Path.of(resourcePath + "/peer1/test10Mb.db")).equals(computeChecksum(Path.of(resourcePath + "/peer2/test10Mb.db")))) {
                downloadTimes.add(downloadEndTime - downloadStartTime);
                downloadSuccess.add(true); // Assuming download was successful
                //remove the file from peer2
//                Path path = Path.of(resourcePath + "/peer2/test10Mb.db");
//                path.toFile().delete();
            } else {
                downloadSuccess.add(false);
                downloadTimes.add(null);
            }

        }

        // Save data to a CSV file
        try (FileWriter writer = new FileWriter("performance_data.csv")) {
            writer.write("Download Time (ms),Latency_1_2 (ms),Latency_1_3 (ms),Latency_1_4 (ms),Latency_1_5 (ms),Download Success, Best Peer\n");
            for (int i = 0; i < numDownloads; i++) {
                writer.write(downloadTimes.get(i) + "," + latencies_peer2_1.get(i) + "," + latencies_peer2_3.get(i) + "," + latencies_peer2_4.get(i) + "," + latencies_peer2_5.get(i) + "," + downloadSuccess.get(i) + "," + bestPeers.get(i) +"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Stop the peer nodes
        peerNode1.stop();
        peerNode2.stop();
        peerNode3.stop();
        peerNode4.stop();
        peerNode5.stop();
        // Stop the tracking server
        server.stop();
        // Delete the test files
        for (int i = 2; i <= 5; i++) {
            Path path = Path.of(resourcePath + "/peer" + i + "/test10Mb.db");
            path.toFile().delete();
        }
    }
}
