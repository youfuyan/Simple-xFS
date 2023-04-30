# 5105-P3: Project 3: Simple xFS

## Tema Member:

Youfu Yan, Bin Hu

## 1 Design Documentation:

### Class and Feature Documentation

We'll use TCP for communication for our simple xFS system. All files are assume cannot be modified. Files with same name should have same content and checksum. A peer selction algorithm is implemented to select the best peer to download a file based on the peer's load index and the latency value from the static latency table. The algorithm take into account both load and latency to make a decision on which peer to select for downloading the file. The system can handle various failure scenarios, including file corruption, tracking server crashes, peer crashes, and special cases. Implements mechanisms for retries, timeouts, and handling checksum mismatches.

### 1.1 Class: PeerNode

- Represents a peer node in the xFS system.
- Maintains a list of shared files along with their checksums.
- Communicates with the tracking server to report the list of files, update the list, and request file locations.
- Communicates with other peers to handle download request based on the peer selection algorithm.

**Methods:**

- `initialize()`: Scans the specific directory, computes file checksums, and sends the list of files to the tracking server.
- `getRemotePeerLoad()`: Returns the current load index (number of concurrent downloads/uploads) for this peer.
- `findFile(filename)`: Sends a request to the tracking server to get a `peerList` of peer nodes that store the specified file.
- `downloadFile(filename, peerList, loadweight)`: Downloads a file from the calcuated best peer(algorithm take loadweight and latency into account) on the peerList, checks for checksum mismatch, and retries if necessary.
- `updateFileList()`: Updates the list of files stored in the specific directory and sends the updated list to the tracking server.
- `listFiles()`: Lists the files stored in the specific directory.

### 1.2. Class: TrackingServer

- Represents the tracking server in the xFS system.
- Maintains a hashmap of available files, their checksums, and the list of peers storing each file.
- Handles requests from peer nodes for file locations and updates to file lists.

**Methods:**

- `find(String filename)`: Returns the list of peers that store the specified file and the file's checksum. It retrieves the file information from the file registry.
- `receiveFileList(String ipAddress, int port, Map<String, String> fileList)`: Receives and updates the file list and checksums for the specified peer. It updates the file registry with the received information and registers the peer if it is not already registered.
- `updateList(String ipAddress, int port, Map<String, String> fileList)`: Updates the list of files stored by the specified peer. It removes the old file list associated with the peer and adds the updated file list.
- `registerPeer(String ipAddress, int port)`: Registers a peer with the tracking server by adding its IP address and port to the known peers set.
- `broadcastRequest()`: Sends a "RECOVER_SERVER" request to all known peers to recover the server state after a crash. It waits for responses from the peers and handles the received data accordingly. If a peer does not respond within a specified timeout period, it removes the unresponsive peer from the known peers list and updates the file registry accordingly.
- `recoverServer()`: Initiates the recovery process by broadcasting the "RECOVER_SERVER" request to known peers and waiting for responses. It removes unresponsive peers if necessary.

### 1.3. Class: LatencyTable

- Represents the static latency table that contains latency values between peer nodes.
- Read at boot-time by the peers.
- Can also generate random latency for simulation purpose

**Methods:**

- `readLatencyFile(latencyFilePath)`: Reads the latency file and populates the latency table with the values.
- `addOrUpdateLatency(node1, node2, latency)`: Adds or updates the latency value between two specified nodes in the table.
- `getLatency(node1, node2)`: Returns the latency value between two specified nodes.
- `removeLatency(node1, node2)`: Removes the latency entry for the specified nodes.
- `generateFakeLatencyFile(latencyFilePath, startPort, endPort, seed)`: Generates a fake latency file with random latency values between the given range for each pair of ports.
- `getLatencyMap()`: Returns the latency map containing the latency values.

### 1.4. Class: Checksum

The `Checksum` class provides methods for computing the `SHA-256` checksum of a file.

**Methods:**

- `computeChecksum(filePath)`: Computes the SHA-256 checksum of the file specified by the given filePath.
- `bytesToHex(bytes)`: Converts an array of bytes to a hexadecimal string representation.

The Checksum class utilizes the `java.nio.file.Files` class and `java.security.MessageDigest` class to compute the checksum using the `SHA-256` algorithm.

The checksum code in this project is based on the code from the following source: [Java SHA-256 Checksum](https://www.baeldung.com/sha-256-hashing-java) The code was modified to fit the specific needs of this project.

## 2. Feature: Peer Selection Algorithm

We have Implemented as a function that selects a peer for downloading a file based on the peer's load index and the latency value from the static latency table. The algorithm take into account both load and latency to make a decision on which peer to select for downloading the file.

#### Peer selection algorithm:

1. For each peer in the list, call the `getRemotePeerLoad()` method to determine the current load of the peer.
2. Calculate a weighted score for each peer using a combination of load and latency. For example, the peer use the formula `score = (1 - loadWeight) * latency + loadWeight * load`, where `loadWeight` is a value between 0 and 1 that determines the importance of load in the score calculation. User can adjust this value to prioritize either load or latency.
3. Select the peer with the **lowest score** as the best peer for downloading the file.

## 3. Feature: Fault Tolerance

Our simple xFS system can handle various failure scenarios, including file corruption, tracking server crashes, peer crashes, and special cases. Implements mechanisms for retries, timeouts, and handling checksum mismatches.

- File Corruption: If a downloaded file is corrupted (detected by checksum mismatch), the system should automatically re-fetch the file from that peer a specific number of times before trying another peer.
- Tracking Server Crashes: Handle tracking server crashes using soft state. The server should recover file sharing information from the peers when it comes back online. When the server recovers from a crash, it sends a RECOVER_SERVER request to all the peers. It is expected that the peers will respond in a timely manner, after a specific time, the server should have a timeout mechanism. Peers are not allow to continue sharing files with each other until server recover since they cannot get checksum information from server.
- Peer Crashes: In the case of a sender node going down, the receiver node will try to download from another node. There is a maximum number of retries that be applied to this operation.
- Handling Special Cases: In situations where all available peers are down or a file is not found on any peer, the system will return a "file not found" message to the client. there Is other information that the client should receive in this case, if all avaliable peers down, it will return "all peers are offline", if a file not found on any peer, it will return "file not found" and plus the tried peers list.

## 3 How to compile

The project is built using Maven 3.6.3 and Java 11(both installed in CSE lab machine). The project can be compiled using the following command:

### Command Line Instructions:

1. Set up Java version to 11 (Linux: CSE lab machine)

   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
   ```

   (macOs) set up Java version to 11

   ```bash
   export JAVA_HOME=`/usr/libexec/java_home -v 11`
   ```

2. Check Maven version, make sure it is 3.6.3 and above.

   ```bash
   mvn -version
   ```

   Double check if the java version is **11.0.18** and Maven is **3.6.3**. (Linux CSE lab machine's default JDK is 17 which is not compatible with Maven 3.6.3, you will need to set java to 11 to complie maven, see bug reported: https://bugs.launchpad.net/ubuntu/+source/maven/+bug/1930541)

3. How to Compile the Maven Project without run test

   ```bash
   mvn clean compile
   ```

   (Another way): How to run test while compiling the Maven Project

   ```
   mvn clean test
   ```

   The project will complied and tests will be run automatically.

4. (Optional) If you wish to run the Maven throught IntelliJ (installed in CSE machine), here is the procedures:

   4.1 Open IntelliJ, click `File` -> `Open`, select the `pom.xml` file in the project folder, click `Open as Project`.

   4.2 After the project is opened, click `Run` -> `Edit Configurations`, click `+` on the top left corner, select `Maven`, in the `Command line` field, type `clean compile`, click `OK`.

   4.3 Click `Run` -> `Run 'Maven'`, the project will be compiled and tests will be run automatically.
   ![step1](/fig/step1.png)
   ![step2](/fig/step2.png)
   ![step3](/fig/step3.png)
   ![step4](/fig/step4.png)
   ![step5](/fig/step5.png)
   ![step6](/fig/step6.png)

## 4 How to Run

Here is the instruction of run `java` class through a terminal.

Navigate to the directory containing the compiled `.class` files, which would be the `simplexfs/target/classe` directory.

```
cd target/classes
```

**Note:** Please run the server first, then run the peer node client. Run your program using the `java` command, providing the fully qualified class name and any command line arguments.

### 4.1 How to run `TrackingServer`:

Command: Default server port is 8080. User can specify a port number as the first argument.

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

### 4.2 How to run `PeerNode CLI`:

**Note**: If you opened another terminal after running server, please make sure you change the Java version to `11` again!

```bash
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

**Note:** Before running the peer node client, please make sure the `latency.txt` file is in the same folder as the complied class files. The `latency.txt` file is located at `simplxfs/target/classes/files/lantency.txt`. And the `peer1` folder is located at `simplxfs/target/classes/files/peer1`.

**Command**: Run `PeerNodeCLI` on: `<peerId> <peerPort> <trackingServerIp> <trackingServerPort>`, where
`<peerId>` is used to refer the folder directory of resoucese, the resourcs for complied classes is located at `simplxfs/target/classes/files/peer+[peerID]`, currently there are preset 5 folders for peers from peer1 to peer5.

```bash
java -cp . edu.umn.peer.PeerNodeCLI 1 8001 127.0.0.1 8080
```

It will start the Peer node client using `simplxfs/target/classes/files/lantency.txt` as global latency file, and `simplxfs/target/classes/files/peer1` as its own shared folder.

### 4.3 Basic operations example:

Step 1: Open terminal in `simplxfs/target/classes` folder, start `TrackingServer` on default port in a terminal:

```bash
java -cp . edu.umn.server.TrackingServer
```

Step 2: Durning `simplxfs/target/classes` folder, start peer1 and peer5 on different terminals:

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

4.4 How to run Simulation files:
In this project, we proivde `DataAnalysis`, `SimulateBasicOperation`, `SimulateMultipleDownloadSend` and `SimulateRecover` for simulation purpose in the `edu.umn` package.

For example, to run `SimulateBasicOperation` (Suppose you open the terminal in `simplxfs/target/classes` folder) :

```bash
java -cp . edu.umn.SimulateBasicOperation
```

4.5 (Optional) Run through IntelliJ:
Here is the optional way to run the project through IntelliJ.

Step 1: Open IntelliJ, click `File` -> `Open`, select the `pom.xml` file in the project folder, click `Open as Project`.

Step 2: After the project is opened, click `Run` -> `Edit Configurations`, click `+` on the top left corner, select `Application`, in the `Main class` field, type `edu.umn.server.TrackingServer`, in the `Program arguments` field, type `8080`, click `OK`.

Step 3: Click `Run` -> `Run 'TrackingServer'`, the server will start running.

Step 4: Repeat step 2 and 3, but this time, in the `Main class` field, type `edu.umn.peer.PeerNodeCLI`, in the `Program arguments` field, type `1 8001 127.0.0.1 8080`, click `OK`. Click `Run` -> `Run 'PeerNodeCLI'`, the peer node client will start running.

## 5. Download Time vs Latency Anaylsis

We conducted an analysis of download time versus latency in our system. To evaluate this, we performed simulations where a 10MB file was downloaded. One peer will try download the file from the four other peers. We ran the simulation for 20 iterations and then repeated it for 100 iterations. The results indicate a linear relationship between download time and latency. On average, the download time was approximately 2 times the latency. However, we observed some outliers in both simulations, where the running time was lower than the lowest latency recorded. One possible explanation for these outliers is that the `Thread.sleep(latency)` operation in the try-catch block failed to accurately simulate the network delay. More detailed analysis is needed to determine the cause of these outliers. Code for the simulation can be found in the `edu.umn.DownloadAnalysis` class.

![downloadtime_20](/fig/download_time_vs_latency20.png)
Figure 1: Download time vs latency with 20 iterations
![downloadtime_100](/fig/download_time_vs_latency_100.png)
Figure 2: Download time vs latency with 100 iterations

**Note**: The provided plots were generated using Python and the matplotlib library. It's important to note that the Python file are included for reference purposes and do not require compilation or execution as part of this project. The Python script responsible for generating the plots can be found in the `data` folder.

## 6. Test Description

The test are located at `simplexfs/src/test/java/edu/umn/`.
The test are run automatically when the project is compiled using Maven command:

```bash
mvn clean test
```

The test cases are described below:

### 6.1 TestBestPeer

We test the peer selection algorithm by creating a list of peers with different latency values and preset loadweight to 0.5. We then call the `download()` method on the peer and check if the peer with the lowest score is selected to complete the download.

**_Test Result: Pass_**

### 6.2 TestChecksumAndCommunication

This test validates the checksum calculation and communication between the peer nodes and the tracking server. It includes updating the file list, searching for specific files, and comparing the computed checksums with the actual checksums of the files.

**_Test Result: Pass_**

### 6.3 TestDownload (Negative Test case included)

The testDownload() method was executed to validate the file download functionality. The test involved updating the file list, searching for a specific file, downloading the file from one peer to another, and verifying the successful download by comparing checksums.

The test also included validating the handling of non-existent files, which resulted in the expected exception being thrown.

**_Test Result: Pass_**

### 6.4 TestFindAndUpdateFileList

The testFindAndUpdateFileList() method was executed to verify the functionality of finding and updating the file list. The test involved updating the file list on the tracking server, searching for specific files, and checking the correctness of the results. Additionally, the test ensured that the tracking server correctly updated its file list.

**_Test Result: Pass_**

### 6.5 TestMultipleDownload

The testMultipleDownload() method was executed to test the functionality of multiple file downloads. The test involved updating the file list on the tracking server, searching for specific files, and initiating downloads from multiple peers simultaneously. The test utilized an ExecutorService to manage the download threads and allowed time for the downloads to complete before performing assertions. The test confirmed the successful completion of the downloads by verifying the existence and checksums of the downloaded files. Assertions were made to ensure that the downloaded files matched the corresponding original files in each peer node.

**_Test Result: Pass_**

### 6.6 TestRecover

The testDebugRecover() method was executed to test the recovery process in the xFS system. The test involved updating the file list on the tracking server, simulating a crash by stopping the server, updating the file list for a peer, restarting the server, and verifying the recovery process.

**_Test Result: Pass_**

## Test Running Result(from Maven)

[INFO] Results:
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0

[INFO] ------------------------------------------------------------------------

[INFO] BUILD SUCCESS

[INFO] ------------------------------------------------------------------------

[INFO] Total time: 40.392 s

[INFO] Finished at: 2023-04-29T20:44:26-05:00

[INFO] ------------------------------------------------------------------------
