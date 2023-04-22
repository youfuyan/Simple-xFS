import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class Peer extends UnicastRemoteObject implements PeerInterface {
    private String machID;
    private String IP;
    private int port;
    private List<File> fileList;
    private int load;

    public Peer(String machID, String IP, int port) throws RemoteException {
        this.machID = machID;
        this.IP = IP;
        this.port = port;
    }

    // Implement the methods from PeerInterface
    public String getMachID() throws RemoteException {
        return machID;
    }

    public String getIP() throws RemoteException {
        return IP;
    }

    public int getPort() throws RemoteException {
        return port;
    }

    public List<File> getFileList() throws RemoteException {
        return fileList;
    }

    public int getLoad() throws RemoteException {
        return load;
    }

    public File download(String filename) throws RemoteException {
        for (File file : fileList) {
            if (file.getName().equals(filename)) {
                return file;
            }
        }
        return null;
    }

    public void updateList(List<File> fileList) throws RemoteException {
        this.fileList = fileList;
    }

}
