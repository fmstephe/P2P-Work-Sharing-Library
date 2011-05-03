package org.francis.p2p.worksharing.smp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.francis.p2p.worksharing.network.message.NetworkMessage;
import org.francis.p2p.worksharing.network.message.SatResult;
import org.francis.p2p.worksharing.network.message.ShutDownNetwork;

public class SMPMessageManager {
    
    private final Map<SMPWorkerId,LinkedBlockingQueue<NetworkMessage>> messageQueues;
    private final LinkedBlockingQueue<NetworkMessage> resultQueue;
    
    public SMPMessageManager(SMPWorkerId[] workers) {
        messageQueues = new HashMap<SMPWorkerId,LinkedBlockingQueue<NetworkMessage>>();
        for (SMPWorkerId worker : workers) {
            messageQueues.put(worker, new LinkedBlockingQueue<NetworkMessage>());
        }
        resultQueue = new LinkedBlockingQueue<NetworkMessage>();
    }
    
    public void sendResult(SatResult result) {
        try {
            resultQueue.put(result);
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // Right now there is no reason this should be interrupted - nor should it block
        }
    }
    
    public SatResult receiveResult() {
        try {
            return (SatResult)resultQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // Right now there is no reason this should be interrupted
        }
    }
    
    public SatResult receiveResult(long timeout) {
        try {
            return (SatResult)resultQueue.poll(timeout, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public SatResult receiveResultOrShutDown(long timeout) {
        SatResult result = null;
        try {
            result = (SatResult)resultQueue.poll(timeout, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            this.shutDownNetwork();
            throw new RuntimeException(e);
        }
        if (result == null) shutDownNetwork();
        return result;
    }
    
    public void shutDownNetwork() {
        ShutDownNetwork msg = new ShutDownNetwork();
        for (SMPWorkerId worker : messageQueues.keySet()) {
            send(worker,msg);
        }
    }
    
    public void send(SMPWorkerId worker, NetworkMessage msg) {
        LinkedBlockingQueue<NetworkMessage> queue = retrieveQueue(worker);
        queue.offer(msg);
    }
    
    public NetworkMessage nonblockingReceive(SMPWorkerId worker) {
        LinkedBlockingQueue<NetworkMessage> queue = retrieveQueue(worker);
        return queue.poll();
    }
    
    public NetworkMessage blockingReceive(SMPWorkerId worker) {
        LinkedBlockingQueue<NetworkMessage> queue = retrieveQueue(worker);
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // Right now there is no reason this should be interrupted
        }
    }
    
    public NetworkMessage timedReceive(SMPWorkerId worker, long timeout) {
        LinkedBlockingQueue<NetworkMessage> queue = retrieveQueue(worker);
        try {
            return queue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // Right now there is no reason this should be interrupted
        }
    }

    private LinkedBlockingQueue<NetworkMessage> retrieveQueue(SMPWorkerId worker) {
        LinkedBlockingQueue<NetworkMessage> queue = messageQueues.get(worker);
        if (queue == null) throw new IllegalArgumentException("worker must be previously registered with this SMPMessageManager.");
        return queue;
    }
}
