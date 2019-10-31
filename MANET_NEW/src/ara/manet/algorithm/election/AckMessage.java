package ara.manet.algorithm.election;

import ara.util.Message;

public class AckMessage extends Message {

	private final long idLeader;
	private final int value;
	
	public AckMessage(long idsrc, long iddest, int pid, long idLeader, int value) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
		this.idLeader = idLeader;
		this.value = value;
	}
	
	public long getIdLeader() {
		return idLeader;
	}
	
	public int getValue() {
		return value;
	}

}
