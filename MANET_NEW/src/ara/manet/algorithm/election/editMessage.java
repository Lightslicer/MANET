package ara.manet.algorithm.election;

import java.util.List;

import ara.manet.algorithm.election.GlobalViewLeader.Peer;
import ara.util.Message;

public class editMessage{
	
	public long source;
	public List<Peer> added;
	public List<Peer> removed;
	public int old_clock;
	public int new_clock;
	
	public editMessage(long source, List<Peer> added, List<Peer> removed, int old_clock, int new_clock) {
		this.source=source;
		this.added=added;
		this.removed=removed;
		this.old_clock=old_clock;
		this.new_clock=new_clock;
	}

}
