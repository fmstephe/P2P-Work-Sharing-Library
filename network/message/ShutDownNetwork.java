package org.francis.sat.network.message;



/**
 * This message prompts each receiving 'node' to stop working and wait for the network to wind down before terminating.
 * Although sending this message implies that the sender has stopped working a separate NetworkChange message is expected
 * to notify that the 'node' shutting down the network has also stopped working.
 * 
 * @author Francis
 */
public class ShutDownNetwork implements NetworkMessage {
    private static final long serialVersionUID = -5281340303215466220L;
}
