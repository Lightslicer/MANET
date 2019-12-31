package ara.manet.algorithm.election;

import ara.util.Message;

public class BeaconExpiredMessage extends Message {

	private int awaited_counter;
	public BeaconExpiredMessage(long idsrc, long iddest, int pid, int awaited_counter) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
		this.awaited_counter = awaited_counter;
	}
	
	public int getAwaitedCounter() {
		return awaited_counter;
	}

}
