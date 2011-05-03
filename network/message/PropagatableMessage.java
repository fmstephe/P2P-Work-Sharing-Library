package org.francis.sat.network.message;

import java.io.Serializable;

import org.francis.sat.network.WorkerId;

/**
 * -record(list_network_message,{top_process,bottom_process,final_destination,message}).
 * 
 * @author Francis
 */
public abstract class PropagatableMessage implements NetworkMessage, Serializable {

    private static final long serialVersionUID = -1618838340209295651L;
    
    public final WorkerId upWorker;
    public final WorkerId downWorker;
    public final WorkerId finalDestination;
    
    public PropagatableMessage(WorkerId upWorker, WorkerId downWorker, WorkerId finalDestination) {
      super();
      this.upWorker = upWorker;
      this.downWorker = downWorker;
      this.finalDestination = finalDestination;
    }
    
    public abstract PropagatableMessage constructNew(WorkerId upWorker, WorkerId downWorker, WorkerId finalDestination);
}