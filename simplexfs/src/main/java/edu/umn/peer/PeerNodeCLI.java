package edu.umn.peer;

import java.util.List;
import java.util.Scanner;

import java.net.URL;


public class PeerNodeCLI {
    private final PeerNode peerNode;
    private final Scanner scanner;

    public PeerNodeCLI(String peerId, int peerPort, String trackingServerIp, int trackingServerPort) {
        // Get the path to the resources directory, which contains the peer directories
        URL resourceUrl = PeerNodeCLI.class.getClassLoader().getResource("files");
        if (resourceUrl == null) {
            throw new RuntimeException("Resources not found");
        }
        String resourcePath = resourceUrl.getPath();
        // Update this path as needed to point to the latency file
        String latencyFilePath = resourcePath + "/latency.txt";

        // Initialize peer nodes using the resources directory
        this.peerNode = new PeerNode(resourcePath + "/peer" + peerId, peerPort, latencyFilePath, trackingServerIp, trackingServerPort);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        peerNode.initialize();
        new Thread(() -> peerNode.start()).start();
        // Allow time for the peer node to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        peerNode.updateFileList();
        while (true) {
            System.out.println("Enter a command:");
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("exit")) {
                peerNode.stop();
                break;
            } else if (command.startsWith("download ")) {
                String[] parts = command.split(" ", 2);
                if (parts.length < 2) {
                    System.out.println("You must specify a filename to download.");
                } else {
                    List<String> peerList = peerNode.findFile(parts[1]);
                    double loadWeight = 0.5;  // replace this with the actual loadWeight
                    peerNode.downloadFile(parts[1], peerList, loadWeight);
                }
            } else if (command.equalsIgnoreCase("list")) {
                List<String> files = peerNode.listFiles();
                System.out.println("Files in this peer:");
                for (String file : files) {
                    System.out.println("  " + file);
                }
            } else {
                System.out.println("Unknown command: " + command);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: <peerId> <peerPort> <trackingServerIp> <trackingServerPort>");
            return;
        }
        String peerId = args[0];
        int peerPort = Integer.parseInt(args[1]);
        String trackingServerIp = args[2];
        int trackingServerPort = Integer.parseInt(args[3]);

        PeerNodeCLI cli = new PeerNodeCLI(peerId, peerPort, trackingServerIp, trackingServerPort);
        cli.start();
    }
}

