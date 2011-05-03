package org.francis.sat.network.erlang;

import java.util.List;

import org.francis.sat.network.Communicator;
import org.francis.sat.network.NetworkManager.Direction;
import org.francis.sat.network.WorkerId;
import org.francis.sat.network.message.NetworkChange;
import org.francis.sat.network.message.NetworkChange.ChangeType;
import org.francis.sat.network.message.NetworkMessage;
import org.francis.sat.network.message.PropagatableMessage;
import org.francis.sat.network.message.SatResult;
import org.francis.sat.network.message.ShutDownNetwork;
import org.francis.sat.network.message.WorkRequest;
import org.francis.sat.network.message.WorkResponse;

import com.ericsson.otp.erlang.OtpErlangBitstr;
import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpErlangExit;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpMbox;

public class ErlangCommunicator implements Communicator {

    private OtpMbox mbox;
    private final WorkerId self;
    private final ErlangWorkerId topWorker;
    private final ErlangWorkerId bottomWorker;
    private final OtpErlangPid listeningProcess; // This is likely to be an
                                                // Erlang node - so don't send
                                                // it serialized java objects
    public ErlangCommunicator(OtpMbox mbox, OtpErlangPid topPid, OtpErlangPid bottomPid, OtpErlangPid listeningProcess) {
        super();
        this.mbox = mbox;
        this.self = new ErlangWorkerId(mbox.self());
        this.topWorker = topPid == null ? null : new ErlangWorkerId(topPid);
        this.bottomWorker = bottomPid == null ? null : new ErlangWorkerId(bottomPid);
        this.listeningProcess = listeningProcess;
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.network.Communicator#receive()
     */
    @Override
    public NetworkMessage receive() {
        try {
            // Pretty cast happy
            OtpErlangBitstr msg = (OtpErlangBitstr)mbox.receive();
            if (msg == null) return null; 
            return (NetworkMessage)msg.getObject();
        } catch (OtpErlangExit e) {
            throw new RuntimeException(e);
        } catch (OtpErlangDecodeException e) {
            throw new RuntimeException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.network.Communicator#receive(long)
     */
    @Override
    public NetworkMessage receive(long timeout) {
        try {
            // Pretty cast happy
            OtpErlangBitstr msg = (OtpErlangBitstr)mbox.receive(timeout);
            if (msg == null) return null; 
            return (NetworkMessage)msg.getObject();
        } catch (OtpErlangExit e) {
            throw new RuntimeException(e);
        } catch (OtpErlangDecodeException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void sendWorkResponse(WorkRequest request, List<?> workstack) {
        WorkResponse response = new WorkResponse(self, workstack);
        mbox.send(((ErlangWorkerId)request.requestingWorker).remoteId, new OtpErlangBitstr(response));
    }
    
    private void sendToFinalDestination(PropagatableMessage networkMessage) {
        if (networkMessage.finalDestination != null)
            mbox.send(((ErlangWorkerId)networkMessage.finalDestination).remoteId, new OtpErlangBitstr(networkMessage));
    }
    
    @Override
    public void broadcastNetworkIncrease() {
        NetworkChange msg = new NetworkChange(topWorker, bottomWorker, null, ChangeType.INC, self);
        propagate(msg,Direction.UP);
    }
    
    @Override
    public void broadcastNetworkDecrease() {
        NetworkChange msg = new NetworkChange(topWorker, bottomWorker, null, ChangeType.DEC, self);
        propagate(msg,Direction.UP);
    }
    
    @Override
    public void broadcastShutDownNetwork() {
        ShutDownNetwork msg = new ShutDownNetwork(topWorker, bottomWorker, null, self);
        propagate(msg,Direction.UP);
    }
    
    @Override
    public void broadcastWorkRequest() {
        WorkRequest msg = new WorkRequest(topWorker, bottomWorker, self);
        propagate(msg,Direction.UP);
    }
    
    @Override
    public boolean isPoisonWorkRequest(WorkRequest message) {
        return (this.self.equals(message.requestingWorker));
    }

    @Override
    public void propagateMessage(PropagatableMessage msg) {
        if (self.equals(msg.downWorker)) { // This means that the message
                                               // just came from above
            PropagatableMessage upNetworkMessage = msg.constructNew(msg.upWorker, bottomWorker, msg.finalDestination);
            propagate(upNetworkMessage, Direction.UP);
        } else if (self.equals(msg.upWorker)) { // This means that the
                                                    // message just came from
                                                    // below
            PropagatableMessage downNetworkMessage = msg.constructNew(topWorker, msg.downWorker, msg.finalDestination);
            propagate(downNetworkMessage, Direction.DOWN);
        } else {
            sendToFinalDestination(msg.constructNew(null, null, msg.finalDestination));
        }
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.network.Communicator#propogate(org.francis.sat.network.message.PropagatableMessage, org.francis.sat.network.NetworkManager.Direction)
     */
    private void propagate(PropagatableMessage message, Direction direction) {
        if (direction == Direction.UP) {
            if (message.upWorker != null) {
                mbox.send(((ErlangWorkerId)message.upWorker).remoteId, new OtpErlangBitstr(message));
                return;
            }
            if (message.downWorker != null) {
                mbox.send(((ErlangWorkerId)message.downWorker).remoteId, new OtpErlangBitstr(message));
                return;
            }
        }
        if (direction == Direction.DOWN) {
            if (message.downWorker != null) {
                mbox.send(((ErlangWorkerId)message.downWorker).remoteId, new OtpErlangBitstr(message));
                return;
            }
            if (message.upWorker != null) {
                mbox.send(((ErlangWorkerId)message.upWorker).remoteId, new OtpErlangBitstr(message));
                return;
            }
        }
        if (message.finalDestination != null) {
            mbox.send(((ErlangWorkerId)message.finalDestination).remoteId, new OtpErlangBitstr(message));
            return;
        }
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.network.Communicator#sendResult(org.francis.sat.network.message.SatResult)
     */
    @Override
    public void sendResult(SatResult result) {
        mbox.send(listeningProcess, new OtpErlangBitstr(result));
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.network.Communicator#printSelf()
     */
    @Override
    public String printSelf() {
        return self.toString();
    }
    
    public String toString() {
        return bottomWorker + "\n<-\n" + self + "\n->\n" + topWorker;
    }
}
