package src.ara.manet.algorithm.election;

import src.ara.util.Message;

public class connectedMessage extends Message{

	public connectedMessage(long idsrc, long iddest, int pid) {
		super(idsrc, iddest, pid);
	}

}
