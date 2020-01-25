package src.ara.manet.detection;

import src.ara.util.Message;

public class ReplyMessage extends Message {

	private long idLeader;
	public ReplyMessage(long idsrc, long iddest, int pid, long idLeader) {
		super(idsrc, iddest, pid);
		this.idLeader = idLeader;
		// TODO Auto-generated constructor stub
	}
	
	public long getIdLeader() {
		return idLeader;
	}

}
