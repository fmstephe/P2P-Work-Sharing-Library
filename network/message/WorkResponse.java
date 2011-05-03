package org.francis.sat.network.message;

import java.io.Serializable;
import java.util.List;

import org.francis.sat.network.WorkerId;

/**
 * -record(work_response,{workstack,responder_pid}).
 * 
 * @author Francis
 */
public class WorkResponse implements NetworkMessage, Serializable {
    
    private static final long serialVersionUID = 2354145961649578849L;

    public final WorkerId respondingWorker;
    public final List<?> workstack;
    
    public WorkResponse(WorkerId respondingWorker, List<?> workstack) {
        this.respondingWorker = respondingWorker;
        this.workstack = workstack;
    }
}