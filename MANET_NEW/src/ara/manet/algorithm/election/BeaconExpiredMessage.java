package src.ara.manet.algorithm.election;

import src.ara.util.Message;

public class BeaconExpiredMessage extends Message {

	private int awaited_counter;
	private long leaderId;
	public BeaconExpiredMessage(long idsrc, long iddest, int pid, long leaderId,int awaited_counter) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
		this.leaderId = leaderId;
		this.awaited_counter = awaited_counter;
	}
	
	public long getLeaderId() {
		return leaderId;
	}
	public int getAwaitedCounter() {
		return awaited_counter;
	}

}
