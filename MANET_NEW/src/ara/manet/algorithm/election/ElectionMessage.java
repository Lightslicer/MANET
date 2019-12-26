package ara.manet.algorithm.election;

import ara.util.Message;

public class ElectionMessage extends Message {
	
	private final long source;
	private Pair<Integer, Long> computation_index;

	public ElectionMessage(long idsrc, long iddest, int pid, long source) {
		super(idsrc, iddest, pid);
		this.source = source;
		// TODO Auto-generated constructor stub
	}
	public ElectionMessage(long idsrc, long iddest, int pid, long source, Pair<Integer, Long> computation_index) {
		this(idsrc,iddest,pid,source);
		this.computation_index = computation_index;
	}
	public long getSource() {
		return source;
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
