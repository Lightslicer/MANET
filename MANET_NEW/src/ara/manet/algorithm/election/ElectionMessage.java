package ara.manet.algorithm.election;

import ara.util.Message;

public class ElectionMessage extends Message {
	
	private final long source;
	private int computation_num;
	private long computation_id;

	public ElectionMessage(long idsrc, long iddest, int pid, long source) {
		super(idsrc, iddest, pid);
		this.source = source;
		// TODO Auto-generated constructor stub
	}
	public ElectionMessage(long idsrc, long iddest, int pid, long source, int computation_num, long computation_id) {
		this(idsrc,iddest,pid,source);
		this.computation_num = computation_num;
		this.computation_id = computation_id;
	}
	public long getSource() {
		return source;
	}
	
	public int getComputationNum() {
		return computation_num;
	}
	
	public long getComputationId() {
		return computation_id;
	}

}
