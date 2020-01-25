package src.ara.manet.algorithm.election;

import src.ara.util.Message;

public class AckMessage extends Message {

	private final long idLeader;
	private final int value;
	private Pair<Integer, Long> computation_index;
	
	
	public AckMessage(long idsrc, long iddest, int pid, long idLeader, int value) {
		super(idsrc, iddest, pid);
		// TODO Auto-generated constructor stub
		this.idLeader = idLeader;
		this.value = value;
	}
	
	public AckMessage(long idsrc, long iddest, int pid, long idLeader, int value , Pair<Integer, Long> computation_index) {
		this(idsrc,iddest,pid,idLeader,value);
		this.computation_index = computation_index;
	}
	
	public long getIdLeader() {
		return idLeader;
	}
	
	public int getValue() {
		return value;
	}
	public int getComputationNum() {
		return computation_index.getNum();
	}
	
	public long getComputationId() {
		return computation_index.getId();
	}
	
	public Pair<Integer, Long> getComputationIndex(){
		return computation_index;
	}

}
