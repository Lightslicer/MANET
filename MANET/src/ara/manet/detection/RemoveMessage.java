package ara.manet.detection;

import ara.util.Message;

public class RemoveMessage extends Message {

	private final int neighbor_pid;
	private final long targetId;

	public RemoveMessage(long idsrc, long iddest, int pid, int neighbor_pid, long targetId) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
		this.neighbor_pid = neighbor_pid;
		this.targetId = targetId;
	}


	public int getNeighborPid() {
		return neighbor_pid;
	}
	
	public long getTargetId() {
		return targetId;
	}
	
}
