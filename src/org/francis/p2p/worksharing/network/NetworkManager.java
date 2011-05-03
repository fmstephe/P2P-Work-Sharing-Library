package org.francis.p2p.worksharing.network;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.francis.p2p.worksharing.network.message.NetworkChange;
import org.francis.p2p.worksharing.network.message.NetworkChange.ChangeType;
import org.francis.p2p.worksharing.network.message.NetworkMessage;
import org.francis.p2p.worksharing.network.message.SatResult;
import org.francis.p2p.worksharing.network.message.ShutDownNetwork;
import org.francis.p2p.worksharing.network.message.WorkRequest;
import org.francis.p2p.worksharing.network.message.WorkResponse;

/**
 * -record(distributed_network_info,{top_process,bottom_process,process_count,
 * network_config}).
 * 
 * @author Francis Stephens
 */
public class NetworkManager {

    public enum NetworkState {
        AWAKE, HIBERNATING, SHUT_DOWN, POISONED
    };

    public enum Direction {
        UP, DOWN
    };

    private static final long TIME_LOG_THREASHOLD = 10;

    private final Communicator comm;
    private final int workSharingThreshold;
    private final int initHibernate;    
    private final int maxHibernate;
    private int networkSize;
    private int hibernationTime;
    private NetworkState state;
    private BufferedWriter logFileWriter;
    private long managementCount;
    
    public NetworkManager(Communicator comm, int networkSize, int workSharingThreshold, int initHibernate, int maxHibernate, String logFilePath) {
        super();
        this.comm = comm;
        this.networkSize = networkSize;
        this.hibernationTime = initHibernate;
        this.state = NetworkState.AWAKE;
        this.workSharingThreshold = workSharingThreshold;
        this.managementCount = 0;
        this.initHibernate = initHibernate;
        this.maxHibernate = maxHibernate;
        this.logFileWriter = generateLogFile(logFilePath, comm.printSelf()+".log");
        log(comm.toString()+"\n"+"Pause time in milliseconds\n",true);
    }
    
    public NetworkManager(Communicator comm, int networkSize, int workSharingThreshold, String logFilePath) {
        // initial hibernation time is defaulted to 2 millis
        // max hibernation time is defaulted to 1024 millis
        this(comm,networkSize,workSharingThreshold,2,1024,logFilePath);
    }

    private BufferedWriter generateLogFile(String logFilePath, String logName) {
        if (logFilePath == null) return null;
        File newLogFile = new File(logFilePath + "/" + logName);
        if (newLogFile.exists())
            newLogFile.delete();
        try {
            newLogFile.createNewFile();
            return new BufferedWriter(new FileWriter(newLogFile));
        } catch (IOException e) {
            System.out.println(newLogFile.getAbsolutePath());
            e.printStackTrace();
            return null;
        }
    }

    private void log(String msg) {
        log(msg, false);
    }

    private void log(String msg, boolean timeStamped) {
        if (state == NetworkState.SHUT_DOWN) return;
        if (logFileWriter == null) return;
        try {
            Date date = new Date();
            if (timeStamped) {
                logFileWriter.append(Long.toString(date.getTime()));
                logFileWriter.append(" : ");
            }
            logFileWriter.append(msg);
            if (timeStamped)
                logFileWriter.newLine();
            logFileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                logFileWriter.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }
    
    private boolean isHibernating(NetworkState checkingState) {
        return checkingState == NetworkState.HIBERNATING;
    }
    
    private boolean isPoisoned(NetworkState checkingState) {
        return checkingState == NetworkState.POISONED;
    }
    
    private boolean isShutDown(NetworkState checkingState) {
        return checkingState == NetworkState.SHUT_DOWN;
    }
    
    private boolean isAwake(NetworkState checkingState) {
        return checkingState == NetworkState.AWAKE;
    }

    public boolean manageNetwork(WorkSharer workSharer) {
        long startTime = System.currentTimeMillis();
        managementCount++;
        NetworkState newState = manageNetworkInt(workSharer, this.state);
        this.state = newState;
        long stopTime = System.currentTimeMillis();
        logManagementTime(startTime,stopTime);
        assert !isHibernating(state);
        assert !(workSharer.isComplete() && !isShutDown(state));
        return !isShutDown(state);
    }
    
    private void logManagementTime(long startTime, long stopTime) {
        long elapsedTime = stopTime - startTime;
        if (elapsedTime > TIME_LOG_THREASHOLD) {
            log(Long.toString(elapsedTime)+"\n",true);
        }
    }

    private NetworkState manageNetworkInt(WorkSharer workSharer, NetworkState previousState) {
        assert previousState == NetworkState.AWAKE;
        if (workSharer.isComplete()) return modelFound();
        NetworkState checkMailState = checkMailbox(workSharer, previousState);
        assert !isHibernating(checkMailState);
        if (isShutDown(checkMailState)) return checkMailState;
        NetworkState workRequestState = makeWorkRequest(workSharer, checkMailState);
        if (workSharer.isComplete()) return modelFound();
        assert !(workSharer.needsWork() && isAwake(workRequestState));
        if (isHibernating(workRequestState)) {
            NetworkState afterHibernateState = hibernate(workSharer,workRequestState);
            if (workSharer.isComplete()) return modelFound();
            assert !isHibernating(afterHibernateState);
            assert !(workSharer.needsWork() && isAwake(afterHibernateState));
            return afterHibernateState;
        }
        else {
            return workRequestState;
        }
    }
    
    private void logStartTime() {
        long startTime = System.currentTimeMillis();
        log("start: "+startTime+"\n");
    }
    
    private void logStopTime() {
        long stopTime = System.currentTimeMillis();
        log("stop: "+stopTime+"\n");
    }

    private NetworkState checkMailbox(WorkSharer workSharer, NetworkState previousState) {
        assert previousState == NetworkState.AWAKE;
        while (true) {
            NetworkMessage message = comm.receive(0);
            if (message == null)
                return previousState;
            NetworkState handleState = handleMessage(message, workSharer, previousState);
            assert handleState != NetworkState.HIBERNATING;
            assert handleState != NetworkState.POISONED;
            if (isShutDown(handleState)) return handleState;
        }
    }
    
    private NetworkState makeWorkRequest(WorkSharer workSharer, NetworkState previousState) {
        assert previousState == NetworkState.AWAKE || previousState == NetworkState.HIBERNATING;
        if (!workSharer.needsWork()) return NetworkState.AWAKE;
        NetworkState newState = previousState;
        while (true) {
            log("Making work request", true);
            comm.broadcastWorkRequest();
            newState = awaitWorkResponse(workSharer, previousState);
            if (!workSharer.needsWork())
                return newState;
            if (newState != previousState)
                return newState;
        }
    }

    private NetworkState awaitWorkResponse(WorkSharer workSharer, NetworkState previousState) {
        assert previousState == NetworkState.AWAKE || previousState == NetworkState.HIBERNATING;
        while (true) {
            NetworkMessage message = comm.receive();
            NetworkState handledState = handleMessage(message, workSharer, previousState);
            if (isPoisoned(handledState)) return NetworkState.HIBERNATING;
            if (message instanceof WorkResponse || isShutDown(handledState)) return handledState;
        }
    }

    private NetworkState handleMessage(NetworkMessage message, WorkSharer workSharer, NetworkState previousState) {
        if (message instanceof NetworkChange) {
            return handleNetworkChange((NetworkChange) message, workSharer, previousState);
        }
        if (message instanceof ShutDownNetwork) {
            return handleShutDownNetwork((ShutDownNetwork) message, previousState);
        }
        if (message instanceof WorkRequest) {
            return handleWorkRequest((WorkRequest) message, workSharer, previousState);
        }
        if (message instanceof WorkResponse) {
            return handleWorkResponse((WorkResponse) message, workSharer, previousState);
        }
        throw new IllegalStateException("We have received a message "+message+" that we don't know what to do with.");
    }

    private NetworkState handleNetworkChange(NetworkChange message, WorkSharer workSharer, NetworkState previousState) {
        comm.propagateMessage(message); // We propogate first just in case the network size
                            // drops to zero and we exit this jvm
        if (message.changeType == ChangeType.INC) {
            this.incNetworkSize();
            return previousState;
        }
        else {
            return decNetworkSize(previousState);
        }
    }

    private NetworkState handleShutDownNetwork(ShutDownNetwork message, NetworkState previousState) {
        return shutdown();
    }
    
    // It should be noted that there is an assumption made that once this network 'node' goes into the TERMINATED state 
    // there is no need for it to receive or propagate any more messages
    private NetworkState shutdown() {
        try {
            if (logFileWriter != null) logFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return NetworkState.SHUT_DOWN;
    }

    private NetworkState handleWorkRequest(WorkRequest message, WorkSharer workSharer, NetworkState previousState) {
        if (comm.isPoisonWorkRequest(message)) { // This message has come full circle
            log("Received poison work request",true);
            if (previousState == NetworkState.AWAKE) { // Here we check to see if we are
                                                       // making a transition from awake
                                                       // to hibernating
                comm.broadcastNetworkDecrease();
                NetworkState decState = decNetworkSize(previousState);
                return decState == NetworkState.SHUT_DOWN ? NetworkState.SHUT_DOWN : NetworkState.POISONED;
            }
            else {
                return NetworkState.POISONED;
            }
        } else if (workSharer.sharableWork() >= workSharingThreshold) {
            Object sharedWork = workSharer.giveWork();
            comm.sendWorkResponse(message, sharedWork);
            return previousState;
        } else {
            comm.propagateMessage(message);
            return previousState;
        }
    }

    private NetworkState handleWorkResponse(WorkResponse message, WorkSharer workSharer, NetworkState previousState) {
        assert !isShutDown(previousState);
        log(message.respondingWorker+"->"+comm.printSelf()+"\n",true);
        workSharer.receiveWork(message.workstack);
        return previousState; // If you are awake - you remain awake. If you are hibernating you remain hibernating
    }

    public int incNetworkSize() {
        networkSize++;
        return networkSize;
    }

    public NetworkState decNetworkSize(NetworkState previousState) {
        networkSize--;
        if (networkSize == 0) {
            return networkExhausted(previousState);
        }
        return previousState;
    }

    public NetworkState hibernate(WorkSharer workSharer, NetworkState previousState) {
        assert previousState == NetworkState.HIBERNATING;
        while (true) {
            NetworkState requestState = makeWorkRequest(workSharer,previousState);
            assert !isAwake(requestState);
            if (isShutDown(requestState)) {
                return requestState;
            }
            if (!workSharer.needsWork()) {
                resetHibernation();
                return NetworkState.AWAKE;
            }
            NetworkState processState = processMessagesInHibernation(workSharer, previousState);
            assert !isAwake(processState);
            if (isShutDown(processState)) {
                return processState;
            }
        }
    }

    private NetworkState processMessagesInHibernation(WorkSharer workSharer, NetworkState previousState) {
        assert previousState == NetworkState.HIBERNATING;
        long startTime = System.currentTimeMillis();
        long currentTime = -1;
        do {
            NetworkMessage message = comm.receive(hibernationTime);
            if (message != null) {
                NetworkState handleState = handleMessage(message, workSharer, previousState);
                assert handleState != NetworkState.AWAKE;
                assert handleState != NetworkState.POISONED;
                if (isShutDown(handleState)) return handleState;
            }
            currentTime = System.currentTimeMillis();
        } while (currentTime - startTime < hibernationTime);
        hibernationTime = Math.min(hibernationTime * 2, maxHibernate);
        return previousState;
    }
    
    public void triviallyUnsat() {
        comm.sendResult(new SatResult(false));
        comm.broadcastShutDownNetwork();
        shutdown();
    }

    public NetworkState networkExhausted(NetworkState previousState) {
        comm.sendResult(new SatResult(false));
        comm.broadcastShutDownNetwork();
        return shutdown();
    }

    private void resetHibernation() {
        comm.broadcastNetworkIncrease();
        incNetworkSize();
        hibernationTime = initHibernate;
    }

    private NetworkState modelFound() {
        log("Satisfying model found");
        comm.sendResult(new SatResult(true));
        comm.broadcastShutDownNetwork();
        return shutdown();
    }
    
    @Override
    public String toString() {
        return comm.toString();
    }
}
