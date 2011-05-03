package org.francis.sat.network.message;

public class SatResult implements NetworkMessage {

    private static final long serialVersionUID = -897856362215972750L;
    public final boolean result;

    public SatResult(boolean result) {
        super();
        this.result = result;
    }
}
