import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackingServer extends UnicastRemoteObject implements TrackingServerInterface {
    private Map<String, PeerInterface> peers;

    public TrackingServer() throws RemoteException {
        peers = new HashMap<>();
    }

    // Implement the methods from TrackingServerInterface
    public void registerPeer(PeerInterface peer) throws RemoteException {
        peers.put(peer.getMachID(), peer);
    }

    public List<PeerInterface> getPeerList(String filename) throws RemoteException {
        List<PeerInterface> peerList = new ArrayList<>();
        for (PeerInterface peer : peers.values()) {
            for (File file : peer.getFileList()) {
                if (file.getName().equals(filename)) {
                    peerList.add(peer);
                    break;
                }
            }
        }
        return peerList;
    }

    public void updatePeerList(String machID, List<File> fileList) throws RemoteException {
        PeerInterface peer = peers.get(machID);
        peer.updateList(fileList);
    }

}
