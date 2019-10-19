package ara.manet.communication;

import ara.manet.detection.ProbeMessage;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocolImpl;
import ara.util.Message;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class EmitterImpl implements Emitter {

	private static final String PAR_LATENCY = "latency";
	private static final String PAR_SCOPE = "scope";
	private static final String PAR_VARIANCE = "variance";
	
	private final int my_pid;
	private final int latency;
	private final int scope;
	private final Boolean variance;
	
	public EmitterImpl(String prefix) {
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.latency = Configuration.getInt(prefix + "." + PAR_LATENCY);
		this.scope = Configuration.getInt(prefix + "." + PAR_SCOPE);
		this.variance = Configuration.getBoolean(prefix + "." + PAR_VARIANCE);
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		// TODO Auto-generated method stub
		PositionProtocolImpl p = (PositionProtocolImpl) node.getProtocol(my_pid);
		Position pos = p.getCurrentPosition();
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if (event instanceof ProbeMessage) {
			ProbeMessage m = (ProbeMessage) event;
			if(m.getIdDest() == Emitter.ALL || m.getIdDest() == node.getID()) {
				/*ajout de idsrc dans la liste voisin soit deliver mais comment*/
			}
		}
		throw new RuntimeException("Receive unknown Event");

	}

	@Override
	public void emit(Node host, Message msg) {
		// TODO Auto-generated method stub
		PositionProtocolImpl p = (PositionProtocolImpl) host.getProtocol(my_pid);
		Position pos = p.getCurrentPosition();
		EDSimulator.add(1, msg, host, my_pid);
		for(int i = 0 ; i< Network.size();i++){
			Node dst = Network.get(i);
			PositionProtocolImpl pp = (PositionProtocolImpl) dst.getProtocol(my_pid);
			if((pp.getCurrentPosition().distance(pos)) <= this.getScope()) {
				if(variance) {
					EDSimulator.add(CommonState.r.nextPoisson(latency),"event", dst, my_pid);	
				}else {
					EDSimulator.add(this.latency,"notloop", dst, my_pid);
				}			
			}
		}
	}

	@Override
	public int getLatency() {
		// TODO Auto-generated method stub
		return latency;
	}

	@Override
	public int getScope() {
		// TODO Auto-generated method stub
		return scope;
	}
	
	@Override
	public EmitterImpl clone() {
		EmitterImpl em = null;
		try {
			em = (EmitterImpl) super.clone();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return em;
	}

}
