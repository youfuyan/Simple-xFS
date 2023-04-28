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

###

### Class and Feature Documentation

#### 1. Class: PeerNode

- Represents a peer node in the xFS system.
- Maintains a list of shared files along with their checksums.
- Communicates with the tracking server to report the list of files, update the list, and request file locations.

**Methods:**

- `initialize()`: Scans the specific directory, computes file checksums, and sends the list of files to the tracking server.
- `getLoad()`: Returns the current load index (number of concurrent downloads/uploads) for this peer.
- `findFile(filename)`: Sends a request to the tracking server to get a list of nodes that store the specified file.
- `downloadFile(filename, peer)`: Downloads a file from the specified peer, checks for checksum mismatch, and retries if necessary.
- `updateFileList()`: Updates the list of files stored in the specific directory and sends the updated list to the tracking server.

#### 2. Class: TrackingServer

- Represents the tracking server in the xFS system.
- Maintains a hashmap of available files, their checksums, and the list of peers storing each file.
- Handles requests from peer nodes for file locations and updates to file lists.

**Methods:**

- `sendInfoRequest()`: Sends a SEND_INFO request to all the peers to request their file lists and checksums.
- `receiveFileList(peer, fileList, checksums)`: Receives and updates the file list and checksums for the specified peer.
- `find(filename)`: Returns the list of peers that store the specified file and the file's checksum.
- `updateList(peer, fileList)`: Updates the list of files stored by the specified peer.

#### 3. Class: LatencyTable

- Represents the static latency table that contains latency values between peer nodes.
- Read at boot-time by the peers.

**Methods:**

- `getLatency(peer1, peer2)`: Returns the latency value between two specified peers.

#### 4. Feature: Peer Selection Algorithm

- Implemented as a function that selects a peer for downloading a file based on the peer's load index and the latency value from the static latency table.
- The algorithm should take into account both load and latency to make a decision on which peer to select for downloading the file.

#### 5. Feature: Fault Tolerance

- Handles various failure scenarios, including file corruption, tracking server crashes, peer crashes, and special cases.
- Implements mechanisms for retries, timeouts, and handling checksum mismatches.

* File Corruption: If a downloaded file is corrupted (detected by checksum mismatch), the system should automatically re-fetch the file from that peer a specific number of times before trying another peer.
* Tracking Server Crashes: Handle tracking server crashes using soft state. The server should recover file sharing information from the peers when it comes back online. When the server recovers from a crash, it sends a SEND_INFO request to all the peers. It is expected that the peers will respond in a timely manner, after a specific time, the server should have a timeout mechanism. Peers should be blocked waiting for the server to come back up or serve files to other peers if they cached file location information earlier.
* Peer Crashes: In the case of a sender node going down, the receiver node will try to download from another node. There is a maximum number of retries that be applied to this operation.
* Handling Special Cases: In situations where all available peers are down or a file is not found on any peer, the system will return a "file not found" message to the client. there Is other information that the client should receive in this case, if all avaliable peers down, it will return "all peers are offline", if a file not found on any peer, it will return "file not found" and plus the tried peers list

### Adds on:

1. set up JDK version to 11(macOS)

   ```bash
   export JAVA_HOME=`/usr/libexec/java_home -v 11`

   ```

(Linux CSE lab machine)

```bash
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

2. check maven version

   ```bash
   mvn -version
   ```

To see if the java version is 11.0.18 and mvn is 3.6.3.
