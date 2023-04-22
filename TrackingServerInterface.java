import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface TrackingServerInterface extends Remote {
    void registerPeer(PeerInterface peer) throws RemoteException;

    List<PeerInterface> getPeerList(String filename) throws RemoteException;

    void updatePeerList(String machID, List<File> fileList) throws RemoteException;
}
