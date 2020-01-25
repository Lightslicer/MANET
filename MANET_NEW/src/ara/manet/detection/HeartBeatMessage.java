package src.ara.manet.detection;

import src.ara.util.Message;

public class HeartBeatMessage extends Message {

//	private final int neighbor_pid;

	public HeartBeatMessage(long idsrc, long iddest, int pid) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
//		this.neighbor_pid = neighbor_pid;
		//System.out.println(cpt);
	}


//	public int getNeighborPid() {
//		return neighbor_pid;
//	}
	
}
