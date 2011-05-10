package org.francis.p2p.worksharing.network;

import org.francis.p2p.worksharing.network.message.NetworkMessage;
import org.francis.p2p.worksharing.network.message.PropagatableMessage;
import org.francis.p2p.worksharing.network.message.ResultMessage;
import org.francis.p2p.worksharing.network.message.WorkRequest;

public interface Communicator {

    public abstract NetworkMessage receive();

    public abstract NetworkMessage receive(long timeout);

    public abstract void sendWorkResponse(WorkRequest request, Object sharedWork);
    
    public abstract void broadcastNetworkIncrease();
    
    public abstract void broadcastNetworkDecrease();
    
    public abstract void broadcastShutDownNetwork();
    
    public abstract void broadcastWorkRequest();

    public abstract void propagateMessage(PropagatableMessage msg);

    public abstract void sendResult(ResultMessage result);
    
    public abstract boolean isPoisonWorkRequest(WorkRequest message);

    public abstract String printSelf();
}