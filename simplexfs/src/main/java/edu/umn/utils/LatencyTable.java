package edu.umn.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class LatencyTable {
    private final Map<String, Integer> latencyMap;

    public LatencyTable(String latencyFilePath) {
        this.latencyMap = new HashMap<>();
        readLatencyFile(latencyFilePath);
    }



    private void readLatencyFile(String latencyFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(latencyFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    int node1 = Integer.parseInt(parts[0]);
                    int node2 = Integer.parseInt(parts[1]);
                    int latency = Integer.parseInt(parts[2]);
                    // Store latency values in both directions
                    latencyMap.put(node1 + "-" + node2, latency);
                    latencyMap.put(node2 + "-" + node1, latency);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add or update latency between current peer and another peer
    public synchronized void addOrUpdateLatency(String peerIpAddress, int latency) {
        latencyMap.put(peerIpAddress, latency);
    }

    // Get latency between two ports
    public synchronized int getLatency(int port1, int port2) {
        return latencyMap.getOrDefault(port1 + "-" + port2, Integer.MAX_VALUE);
    }

    // Remove latency entry for a peer
    public synchronized void removeLatency(String peerIpAddress) {
        latencyMap.remove(peerIpAddress);
    }

    // Get the peer with the lowest latency
    public synchronized String getPeerWithLowestLatency() {
        String lowestLatencyPeer = null;
        int minLatency = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : latencyMap.entrySet()) {
            if (entry.getValue() < minLatency) {
                minLatency = entry.getValue();
                lowestLatencyPeer = entry.getKey();
            }
        }
        return lowestLatencyPeer;
    }

    // Generate a fake latency file with random latency values between [100-5000] ms for each pair of ports
    public static void generateFakeLatencyFile(String latencyFilePath, int startPort, int endPort, int seed) {
        try (FileWriter writer = new FileWriter(latencyFilePath)) {
            Random random = new Random(seed);
            for (int i = startPort; i <= endPort; i++) {
                for (int j = i + 1; j <= endPort; j++) {
                    int latency = 100 + random.nextInt(4901); // Random latency value in range [100-5000] ms
                    String line = String.format("%d,%d,%d%n", i, j, latency);
                    writer.write(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //save file to src/main/resources/latency.txt
        String latencyFilePath = "src/main/resources/files/latency.txt";
        //set random seed to keep the same random values
        int seed = 5105;
        //generate fake latency file
        generateFakeLatencyFile(latencyFilePath, 8001, 8005, seed);
        //create a latency table
        LatencyTable latencyTable = new LatencyTable(latencyFilePath);
        //print latency table
        System.out.println(latencyTable.latencyMap);
    }
}


