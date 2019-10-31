package ara.manet.algorithm.election;

import ara.util.Message;

public class LeaderMessage extends Message {
	private final long idLeader;
	private final int leaderValue;
	public LeaderMessage(long idsrc, long iddest, int pid, long idLeader, int leaderValue) {
		super(idsrc, iddest, pid);
		this.idLeader = idLeader;
		this.leaderValue = leaderValue;
		// TODO Auto-generated constructor stub
	}

	public long getIdLeader() {
		return idLeader;
	}
	
	public int getLeaderValue() {
		return leaderValue;
	}
}
