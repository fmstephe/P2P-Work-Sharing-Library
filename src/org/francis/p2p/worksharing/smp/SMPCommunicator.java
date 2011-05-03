package org.francis.p2p.worksharing.smp;

import org.francis.p2p.worksharing.network.Communicator;
import org.francis.p2p.worksharing.network.NetworkManager.Direction;
import org.francis.p2p.worksharing.network.message.NetworkChange;
import org.francis.p2p.worksharing.network.message.NetworkChange.ChangeType;
import org.francis.p2p.worksharing.network.message.NetworkMessage;
import org.francis.p2p.worksharing.network.message.PropagatableMessage;
import org.francis.p2p.worksharing.network.message.SatResult;
import org.francis.p2p.worksharing.network.message.WorkRequest;
import org.francis.p2p.worksharing.network.message.WorkResponse;

public class SMPCommunicator implements Communicator {

    private final SMPMessageManager messageManager;
    private final SMPWorkerId topWorker;
    private final SMPWorkerId bottomWorker;
    private final SMPWorkerId self;

    public SMPCommunicator(SMPMessageManager messageManager, SMPWorkerId topWorker, SMPWorkerId bottomWorker, SMPWorkerId self) {
        super();
        this.messageManager = messageManager;
        this.topWorker = topWorker;
        this.bottomWorker = bottomWorker;
        this.self = self;
    }

    @Override
    public NetworkMessage receive() {
        return messageManager.blockingReceive(self);
    }

    @Override
    public NetworkMessage receive(long timeout) {
        if (timeout == 0)
            return messageManager.nonblockingReceive(self);
        else
            return messageManager.timedReceive(self, timeout);
    }

    @Override
    public void sendWorkResponse(WorkRequest request, Object workstack) {
        WorkResponse response = new WorkResponse(self, workstack);
        messageManager.send((SMPWorkerId)request.requestingWorker, response);
    }
    
    private void sendToFinalDestination(PropagatableMessage networkMessage) {
        if (networkMessage.finalDestination != null)
            messageManager.send((SMPWorkerId)networkMessage.finalDestination, networkMessage);
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
        messageManager.shutDownNetwork();
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
                messageManager.send((SMPWorkerId)message.upWorker, message);
                return;
            }
            if (message.downWorker != null) {
                messageManager.send((SMPWorkerId)message.downWorker, message);
                return;
            }
        }
        if (direction == Direction.DOWN) {
            if (message.downWorker != null) {
                messageManager.send((SMPWorkerId)message.downWorker, message);
                return;
            }
            if (message.upWorker != null) {
                messageManager.send((SMPWorkerId)message.upWorker, message);
                return;
            }
        }
        if (message.finalDestination != null) {
            messageManager.send((SMPWorkerId)message.finalDestination, message);
            return;
        }
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.network.Communicator#sendResult(org.francis.sat.network.message.SatResult)
     */
    @Override
    public void sendResult(SatResult result) {
        messageManager.sendResult(result);
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.network.Communicator#printSelf()
     */
    @Override
    public String printSelf() {
        return self.toString();
    }
    
    public String toString() {
        return topWorker + "<" + self + ">" + bottomWorker;
    }
}