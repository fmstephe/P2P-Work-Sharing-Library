package org.francis.p2p.worksharing.network.message;

public class ResultMessage implements NetworkMessage {

    private static final long serialVersionUID = -897856362215972750L;
    public final Object result;

    public ResultMessage(Object result) {
        super();
        this.result = result;
    }
}
