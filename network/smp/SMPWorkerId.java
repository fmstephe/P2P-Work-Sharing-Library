package org.francis.sat.network.smp;

import org.francis.sat.network.WorkerId;

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