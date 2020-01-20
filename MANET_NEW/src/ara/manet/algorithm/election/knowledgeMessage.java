package ara.manet.algorithm.election;

import java.util.List;

import ara.manet.algorithm.election.GlobalViewLeader.View;
import ara.util.Message;

public class knowledgeMessage extends Message{
	View[] knowledge;
	int srcClock;

	public knowledgeMessage(long idsrc, long iddest, int pid, View[] knowledge, int srcClock) {
		super(idsrc, iddest, pid);
		this.knowledge = knowledge;
		this.srcClock = srcClock;
	}

}
