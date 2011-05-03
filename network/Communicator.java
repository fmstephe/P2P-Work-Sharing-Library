package org.francis.sat.network;

import java.util.List;

import org.francis.sat.network.message.NetworkMessage;
import org.francis.sat.network.message.PropagatableMessage;
import org.francis.sat.network.message.SatResult;
import org.francis.sat.network.message.WorkRequest;

public interface Communicator {

    public abstract NetworkMessage receive();

    public abstract NetworkMessage receive(long timeout);

    public abstract void sendWorkResponse(WorkRequest request, List<?> workstack);
    
    public abstract void broadcastNetworkIncrease();
    
    public abstract void broadcastNetworkDecrease();
    
    public abstract void broadcastShutDownNetwork();
    
    public abstract void broadcastWorkRequest();

    public abstract void propagateMessage(PropagatableMessage msg);

    public abstract void sendResult(SatResult result);
    
    public abstract boolean isPoisonWorkRequest(WorkRequest message);

    public abstract String printSelf();
}