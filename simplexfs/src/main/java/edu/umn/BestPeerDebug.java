package edu.umn;
import edu.umn.peer.PeerNode;
import edu.umn.utils.LatencyTable;

import java.util.ArrayList;
import java.util.List;
public class BestPeerDebug {

    public static void main(String[] args) {
        // Create a test environment with multiple peer nodes
        List<String> peerList = new ArrayList<>();
        peerList.add("192.168.0.2:8001");
        peerList.add("192.168.0.3:8002");
        peerList.add("192.168.0.4:8003");
        peerList.add("192.168.0.5:8004");

        // Set up different load and latency values for each peer
        // You can either hardcode these values or set up a testing environment with real nodes
        // For this example, we will hardcode the values in the LatencyTable class
//        LatencyTable latencyTable = new LatencyTable();
//        latencyTable.addLatency(8000, 8001, 20);
//        latencyTable.addLatency(8000, 8002, 30);
//        latencyTable.addLatency(8000, 8003, 15);
//        latencyTable.addLatency(8000, 8004, 40);

        // Create a test file and distribute it across the peer nodes
        String testFile = "testfile.txt";

        // Initialize the PeerNode class with the test environment
        PeerNode peerNode = new PeerNode("files", 8000, "latency.csv", "192.168.0.1", 9000);
//        peerNode.setLatencyTable(latencyTable);

        // Run the selectBestPeer() method with different loadWeight values and compare the results
        double[] loadWeights = {0.0, 0.25, 0.5, 0.75, 1.0};

        for (double loadWeight : loadWeights) {
            String bestPeer = peerNode.selectBestPeer(peerList, loadWeight);
            System.out.println("Best peer with loadWeight " + loadWeight + ": " + bestPeer);
        }
    }
}

