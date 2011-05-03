package org.francis.sat.network.message;

import org.francis.sat.network.WorkerId;


/**
 * -record(network_change,{notification,notifier_pid}).
 *
 * @author Francis
 */
public class NetworkChange extends PropagatableMessage {
    
    private static final long serialVersionUID = 2542769912721144120L;

    public enum ChangeType {INC,DEC};
    
    public final ChangeType changeType;
    public final WorkerId notifyingWorker;
    
    public NetworkChange(WorkerId upWorker, WorkerId downWorker, WorkerId finalDestination, ChangeType notification, WorkerId notifyingWorker) {
        super(upWorker,downWorker,finalDestination);
        this.changeType = notification;
        this.notifyingWorker = notifyingWorker;
    }

    @Override
    public PropagatableMessage constructNew(WorkerId upWorker, WorkerId downWorker, WorkerId finalDestination) {
        return new NetworkChange(upWorker,downWorker,finalDestination,this.changeType,this.notifyingWorker);
    }
}