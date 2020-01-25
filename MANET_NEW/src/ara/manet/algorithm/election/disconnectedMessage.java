package src.ara.manet.algorithm.election;

import src.ara.util.Message;

public class disconnectedMessage extends Message{

	public disconnectedMessage(long idsrc, long iddest, int pid) {
		super(idsrc, iddest, pid);
	}

}