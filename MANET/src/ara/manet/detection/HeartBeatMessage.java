package ara.manet.detection;

import ara.util.Message;

public class HeartBeatMessage extends Message {

//	private final int neighbor_pid;
	private final Message m;

	public HeartBeatMessage(long idsrc, long iddest, int pid,Message m) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
//		this.neighbor_pid = neighbor_pid;
		this.m = m;
	}


//	public int getNeighborPid() {
//		return neighbor_pid;
//	}
	
	public Message getMessage() {
		return m;
	}
}
