package src.ara.manet.algorithm.election;

import src.ara.manet.algorithm.election.GlobalViewLeader.View;
import src.ara.util.Message;

public class knowledgeMessage extends Message{
	View[] knowledge;
	int srcClock;

	public knowledgeMessage(long idsrc, long iddest, int pid, View[] knowledge, int srcClock) {
		super(idsrc, iddest, pid);
		this.knowledge = knowledge;
		this.srcClock = srcClock;
	}

}
