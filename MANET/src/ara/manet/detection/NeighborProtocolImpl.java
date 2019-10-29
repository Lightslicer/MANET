package ara.manet.detection;

import java.util.ArrayList;
import java.util.List;

import ara.manet.communication.Emitter;
import peersim.config.Configuration;
import peersim.core.Node;

public class NeighborProtocolImpl implements NeighborProtocol {
	
	private static final String PAR_PROBE = "probe";
	private static final String PAR_TIMER = "timer";
	private static final String PAR_EMITTER = "emit";
	
	private final int my_pid;
	private final int probe;
	private final int timer;
	private final Emitter emitter;
	
	
	private List<Long> neighbors ;
	public NeighborProtocolImpl(String prefix) {
		
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.probe = Configuration.getInt(prefix + "." + PAR_PROBE);
		this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
		this.emitter = (Emitter) Configuration.getInstance("protocol." + PAR_EMITTER);
		this.neighbors = new ArrayList<Long>();
	}
	
	
	@Override
	public List<Long> getNeighbors() {
		// TODO Auto-generated method stub
		return neighbors;
	}
	
	@Override
	public NeighborProtocolImpl clone() {
		NeighborProtocolImpl em = null;
		try {
			em = (NeighborProtocolImpl) super.clone();
			em.neighbors = new ArrayList<Long>();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return em;
	}

	public void heartbeat(Node host){
			long idSrc = host.getID();
			ProbeMessage m = new ProbeMessage(idSrc,(long)-2,my_pid,timer,probe);
			emitter.emit(host, m);
	}
}
