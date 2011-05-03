package org.francis.p2p.worksharing.network.message;

import org.francis.p2p.worksharing.network.WorkerId;


/**
 * -record(work_request,{requester_pid}).
 * 
 * @author Francis
 */
public class WorkRequest extends PropagatableMessage {
    
    private static final long serialVersionUID = 7686768874096040365L;
    
    public final WorkerId requestingWorker;

    public WorkRequest(WorkerId upWorker, WorkerId downWorker, WorkerId finalDestination) {
        super(upWorker,downWorker,finalDestination);
        this.requestingWorker = finalDestination;
    }

    @Override
    public PropagatableMessage constructNew(WorkerId upWorker, WorkerId downWorker, WorkerId finalDestination) {
        return new WorkRequest(upWorker,downWorker,finalDestination);
    }
}
