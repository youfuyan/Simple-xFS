# 5105-P3

## Project 3: Simple xFS

### Step 1: Define classes and interfaces

We'll use RMI (Remote Method Invocation) for communication as it allows for easy remote object invocation in Java.

Define a `Peer` class with attributes like `machID, IP, port, fileList, load`, etc., and methods like `find, download, getLoad, updateList`, and any other methods required.
Define a TrackingServer class with attributes like peers, and methods like registerPeer, getPeerList, updatePeerList, and any other methods required.
Define a File class with attributes like name, checksum, and any other necessary attributes.

### Step 2: Initialize the file system

Read the latency configuration file and store the latency values for each pair of peers.
Each peer should read its local directory and create a File object for each file found. Calculate the checksum for each file.
When a peer starts, it should register itself with the TrackingServer by providing its machID, IP, port, fileList, and checksums.

### Step 3: Implement find, download, getLoad, and updateList operations

Implement the find operation in the TrackingServer class. It should return a list of peers containing the requested file along with the file's checksum.
Implement the download operation in the Peer class. It should download the file from the selected peer and add it to its local directory. Update the file's checksum and notify the TrackingServer of the updated file list.
Implement the getLoad operation in the Peer class. It should return the current load of the peer.
Implement the updateList operation in the TrackingServer class. It should update the server's information about a peer's file list.

### Step 4: Implement the peer selection algorithm

When a peer wants to download a file, it should first call the find operation on the TrackingServer to get the list of peers containing the file.
Implement the peer selection algorithm that takes both load and latency into account. You can use the getLoad operation to determine the current load of a peer, and the latency values from the configuration file.
Explain the rationale behind your algorithm and how the average file download time changes as the number of peers hosting a file increases.

### Step 5: Implement fault tolerance

Handle file corruption by checking the checksum after the file is downloaded. If there's a mismatch, re-fetch the file from the same peer or try another peer.
Handle tracking server crashes using soft state. The server should recover file sharing information from the peers when it comes back online. Peers should be blocked waiting for the server to come back up or serve files to other peers if they cached file location information earlier.
Handle peer crashes by trying to download from another node in case of failure. When a node recovers, it should rejoin the network and supply file sharing information to the server.

### Step 6: Test the system

Test your implementation with various scenarios, including file corruption, server crashes, and peer crashes.
Make sure to test edge cases, such as all available peers being down and failing to find a file.
