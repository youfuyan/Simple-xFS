# 5105-P3: Project 3: Simple xFS

## Tema Member:

Youfu Yan, Bin Hu

## 1 Class and Feature Documentation

We'll use TCP for communication for our simple xFS system. All files are assume cannot be modified. File with same file should have same content and checksum. 

#### 1. Class: PeerNode

- Represents a peer node in the xFS system.
- Maintains a list of shared files along with their checksums.
- Communicates with the tracking server to report the list of files, update the list, and request file locations.
- Communicates with other peers to handle download request.

**Methods:**

- `initialize()`: Scans the specific directory, computes file checksums, and sends the list of files to the tracking server.
- `getLoad()`: Returns the current load index (number of concurrent downloads/uploads) for this peer.
- `findFile(filename)`: Sends a request to the tracking server to get a list of nodes that store the specified file.
- `downloadFile(filename, peer, loadweight)`: Downloads a file from the specified peer, checks for checksum mismatch, and retries if necessary.
- `updateFileList()`: Updates the list of files stored in the specific directory and sends the updated list to the tracking server.

#### 2. Class: TrackingServer

- Represents the tracking server in the xFS system.
- Maintains a hashmap of available files, their checksums, and the list of peers storing each file.
- Handles requests from peer nodes for file locations and updates to file lists.

**Methods:**

- `receiveFileList(peer, fileList, checksums)`: Receives and updates the file list and checksums for the specified peer.
- `find(filename)`: Returns the list of peers that store the specified file and the file's checksum.
- `updateList(peer, fileList)`: Updates the list of files stored by the specified peer.

#### 3. Class: LatencyTable

- Represents the static latency table that contains latency values between peer nodes.
- Read at boot-time by the peers.
- Can also generate random latency for simulation purpose

**Methods:**

- `getLatency(peer1, peer2)`: Returns the latency value between two specified peers.

#### 4. Class: Checksum

The checksum code in this project is based on the code from the following source:  [Java SHA-256 Checksum](https://www.baeldung.com/sha-256-hashing-java) The code was modified to fit the specific needs of this project.

## 2. Feature: Peer Selection Algorithm

We have Implemented as a function that selects a peer for downloading a file based on the peer's load index and the latency value from the static latency table. The algorithm take into account both load and latency to make a decision on which peer to select for downloading the file. 

#### Peer selection algorithm:

1. For each peer in the list, call the `getLoad()` method to determine the current load of the peer.
2. Calculate a weighted score for each peer using a combination of load and latency. For example, the peer use the formula `score = (1 - loadWeight) * latency + loadWeight * load`, where `loadWeight` is a value between 0 and 1 that determines the importance of load in the score calculation. User can adjust this value to prioritize either load or latency.
3. Select the peer with the **lowest score** as the best peer for downloading the file.

#### 5. Feature: Fault Tolerance

Our simple xFS system can handle various failure scenarios, including file corruption, tracking server crashes, peer crashes, and special cases. Implements mechanisms for retries, timeouts, and handling checksum mismatches.

* File Corruption: If a downloaded file is corrupted (detected by checksum mismatch), the system should automatically re-fetch the file from that peer a specific number of times before trying another peer.
* Tracking Server Crashes: Handle tracking server crashes using soft state. The server should recover file sharing information from the peers when it comes back online. When the server recovers from a crash, it sends a  RECOVER_SERVER request to all the peers. It is expected that the peers will respond in a timely manner, after a specific time, the server should have a timeout mechanism. Peers are not allow to continue sharing files with each other until server recover since they cannot get checksum information from server.
* Peer Crashes: In the case of a sender node going down, the receiver node will try to download from another node. There is a maximum number of retries that be applied to this operation.
* Handling Special Cases: In situations where all available peers are down or a file is not found on any peer, the system will return a "file not found" message to the client. there Is other information that the client should receive in this case, if all avaliable peers down, it will return "all peers are offline", if a file not found on any peer, it will return "file not found" and plus the tried peers list

### Command Line Instructions:

1. set up Java version to 11(macOS)

   ```bash
   export JAVA_HOME=`/usr/libexec/java_home -v 11`
   export PATH=/usr/local/bin:$PATH
   
   ```

(Linux: CSE lab machine)

```bash
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

2. check Maven version

   ```bash
   mvn -version
   ```

Double check  if the java version is **11.0.18** and Maven is **3.6.3**. (Linux CSE lab machine's default JDK is 17 which is not compatible with Maven 3.6.3, see bug reported:https://bugs.launchpad.net/ubuntu/+source/maven/+bug/1930541)

3. How to Compile
   ```bash
   mvn clean test
   ```

   The project will complied and tests will be run automatically.

3. how to tar to tar.gz

   ```bash
   $ cd ..
   ~/Desktop/UMN
   $ tar -czf test.tar.gz cpt
   ```

   4. Run java peer interfaceOpen a terminal.
      Navigate to the directory containing the compiled .class files. In your case, this would be the target/classes directory.

      ```
      cd target/classes
      ```

      Run your program using the java command, providing the fully qualified class name and any command line arguments. 

      For example: 

      (1) How to run server:

      Command: Default server port is 8080.

      ```bash
      java -cp . edu.umn.server.TrackingServer 
      java -cp . edu.umn.server.TrackingServer [PORT]
      ```

      Output:

      ```
      ~/Desktop/UMN/2023Spring/CSCI5105/5105-P3/simplexfs/target/classes on simplexfs*
      $ java -cp . edu.umn.server.TrackingServer 
      Starting server on port 8080
      ~/Desktop/UMN/2023Spring/CSCI5105/5105-P3/simplexfs/target/classes on simplexfs*
      $ java -cp . edu.umn.server.TrackingServer 8081
      Starting server on port 8081
      ```

      (2) How to run PeerNode client:

      Command: Run peer on : <peerId> <peerPort> <trackingServerIp> <trackingServerPort>
      <peerId> is used to refer the folder directory of resoucese, the resourcs for complied classes is located at `simplxfs/target/classes/files/peer+[peerID]`, currently there are preset 5 folders for peers from peer1 to peer5. 

      ```bash
      java -cp . edu.umn.peer.PeerNodeCLI 1 8001 127.0.0.1 8080
      ```

      It will start the Peer node client using `simplxfs/target/classes/files/lantency.txt` as global latency file, and `simplxfs/target/classes/files/peer1` as its own shared folder.

​			(3) Basic operation example:

​				Step 1: Durning `simplxfs/target/classes` folder, start Server on default port in a terminal:

```bash
java -cp . edu.umn.server.TrackingServer 
```

​	Step 2: Durning `simplxfs/target/classes` folder, start peer1 and peer5 on different terminals:

```bash
java -cp . edu.umn.peer.PeerNodeCLI 1 8001 127.0.0.1 8080
(Another terminal)
java -cp . edu.umn.peer.PeerNodeCLI 5 8005 127.0.0.1 8080
```

Step3: Try `list` and `download sample5.txt` in peer1 interface.

```bash
list
download sample5.txt
```


Result:

```
~/Desktop/UMN/2023Spring/CSCI5105/5105-P3/simplexfs/target/classes on simplexfs*
$ java -cp . edu.umn.peer.PeerNodeCLI 1 8001 127.0.0.1 8080
File checksums: {test10Mb.db=e5b844cc57f57094ea4585e235f36c78c1cd222262bb89d53c94dcb4d6b3e55d, sample1.txt=19aa3194b9ceaa6a35e04daae78e8ecf512813c508d0da04c6d648063e340408}
File names: [test10Mb.db, sample1.txt]
Starting peer node on port 8001
Server response: UPDATE_LIST_SUCCESS:8001
Enter a command:
list
Files in this peer:
  test10Mb.db
  sample1.txt
Enter a command:
download sample5.txt
Latency: 3546ms
Computed checksum: b1a70ae5bd5dadba0d73756e629e34fb6c75d605f70e5ebb85fc4cfa3dad913e
Original checksum: b1a70ae5bd5dadba0d73756e629e34fb6c75d605f70e5ebb85fc4cfa3dad913e
File download successful.
File downloaded from: 127.0.0.1:8005:b1a70ae5bd5dadba0d73756e629e34fb6c75d605f70e5ebb85fc4cfa3dad913e
Server response: UPDATE_LIST_SUCCESS:8001
Enter a command:
list
Files in this peer:
  test10Mb.db
  sample1.txt
  sample5.txt

```

