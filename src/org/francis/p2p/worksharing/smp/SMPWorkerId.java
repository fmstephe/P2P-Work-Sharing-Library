package org.francis.p2p.worksharing.smp;

import org.francis.p2p.worksharing.network.WorkerId;

public class SMPWorkerId implements WorkerId {

    private final int id;
    
    public SMPWorkerId (int id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return Integer.toString(id);
    }
}