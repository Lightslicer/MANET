package src.ara.manet.algorithm.election;

import src.ara.util.Message;

public class BeaconMessage extends Message {

	private final int timestamp;

	public BeaconMessage(long idsrc, long iddest, int pid, int timestamp) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
		this.timestamp = timestamp;
	}

	public int getTimeStamp() {
		return timestamp;
	}
}

