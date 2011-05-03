package org.francis.sat.network.erlang;

import java.io.Serializable;

import org.francis.sat.network.WorkerId;

import com.ericsson.otp.erlang.OtpErlangPid;

public class ErlangWorkerId implements Serializable, WorkerId {
    
    private static final long serialVersionUID = 2153157344513640152L;
    
    public final OtpErlangPid remoteId;
    
    public ErlangWorkerId(OtpErlangPid remoteId) {
        if (remoteId == null) throw new IllegalArgumentException("remoteId must not be null");
        this.remoteId = remoteId;
    }
    
    public String toString() {
        if (remoteId == null) {
            return "nullPid";
        }
        String pidString = remoteId.toString();
        int beginIndex = pidString.indexOf("<");
        int endIndex = pidString.indexOf(">");
        return pidString.substring(beginIndex+1, endIndex);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((remoteId == null) ? 0 : remoteId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ErlangWorkerId other = (ErlangWorkerId) obj;
        if (remoteId == null) {
            if (other.remoteId != null)
                return false;
        } else if (!remoteId.equals(other.remoteId))
            return false;
        return true;
    }
}