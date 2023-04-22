import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PeerInterface extends Remote {
    String getMachID() throws RemoteException;

    String getIP() throws RemoteException;

    int getPort() throws RemoteException;

    List<File> getFileList() throws RemoteException;

    int getLoad() throws RemoteException;

    File download(String filename) throws RemoteException;

    void updateList(List<File> fileList) throws RemoteException;
}
