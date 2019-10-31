package ara.manet.algorithm.election;

import ara.util.Message;

public class ElectionMessage extends Message {
	
	private final long source;

	public ElectionMessage(long idsrc, long iddest, int pid, long source) {
		super(idsrc, iddest, pid);
		this.source = source;
		// TODO Auto-generated constructor stub
	}
	
	public long getSource() {
		return source;
	}

}
