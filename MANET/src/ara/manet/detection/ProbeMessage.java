package ara.manet.detection;

import ara.util.Message;

public class ProbeMessage extends Message {

	//private final Message message;
	
	private final int timer;
	private final int probe;
	
	public ProbeMessage(long idsrc, long iddest, int pid, int timer,int probe) {
		super(idsrc, iddest, pid);
		this.timer = timer;
		this.probe = probe;
		// TODO Auto-generated constructor stub
		//this.message = m;
	}

	/*public Message getMessage() {
		return message;
	}*/
	public int getTimer() {
		return timer;
	}
	
	public int getProbe() {
		return probe;
	}
	
}
