package org.francis.p2p.worksharing.network;


public interface WorkSharer {

    public abstract Object giveWork();

    public abstract void receiveWork(Object stack);

    public abstract int sharableWork();
    
    public abstract boolean needsWork();
    
    public abstract boolean isComplete();
    
    public abstract Object getSuccessMessage();
    
    public abstract Object getFailureMessage();
}
