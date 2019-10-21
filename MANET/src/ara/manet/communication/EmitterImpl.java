package ara.manet.communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ara.manet.detection.HeartBeatMessage;
import ara.manet.detection.NeighborProtocolImpl;
import ara.manet.detection.NeighborhoodListener;
import ara.manet.detection.ProbeMessage;
import ara.manet.detection.RemoveMessage;
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
	
	private Map<Long, Long> map; //map pour se souvenir de ses voisins
	private List<NeighborhoodListener> list; //liste contenant les listener
	
	public EmitterImpl(String prefix) {
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.latency = Configuration.getInt(prefix + "." + PAR_LATENCY);
		this.scope = Configuration.getInt(prefix + "." + PAR_SCOPE);
		this.variance = Configuration.getBoolean(prefix + "." + PAR_VARIANCE);
		map = new HashMap<>();
		list = new ArrayList<>();
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		// TODO Auto-generated method stub
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if (event instanceof ProbeMessage) { // A la réception de heartbeat
			ProbeMessage m = (ProbeMessage) event;
			NeighborProtocolImpl np = (NeighborProtocolImpl) node.getProtocol(m.getPid());
			if(!np.getNeighbors().contains(m.getIdSrc()))
			if(m.getIdDest() == Emitter.ALL || m.getIdDest() == node.getID()) {
					np.getNeighbors().add(m.getIdSrc());
					notifyAddListener(node, m.getIdSrc());
			}
			EDSimulator.add(m.getTimer(), new RemoveMessage(node.getID(),node.getID(),my_pid,m.getPid(),m.getIdSrc()), node, pid);
			map.put(m.getIdSrc(), CommonState.getTime()+m.getTimer());
		
		}
		if (event instanceof HeartBeatMessage) {
			HeartBeatMessage m = (HeartBeatMessage) event;
			emit(node,m.getMessage());
		}
		if (event instanceof RemoveMessage) {//remove ne marche pas
			
			RemoveMessage m = (RemoveMessage) event;
			NeighborProtocolImpl np = (NeighborProtocolImpl) node.getProtocol(m.getNeighborPid());

			if(map.containsKey(m.getTargetId())){
				if(map.get(m.getTargetId()) <= CommonState.getTime()) {
					//le voisin devrait être supprimé
					np.getNeighbors().remove(m.getTargetId());
					notifyRemoveListener(node, m.getTargetId());
				}				
			}
		}

	}

	@Override
	public void emit(Node host, Message msg) {
		// TODO Auto-generated method stub
		Message m = (Message) msg;
		int position_pid=Configuration.lookupPid("position");
		PositionProtocolImpl p = (PositionProtocolImpl) host.getProtocol(position_pid);
		Position pos = p.getCurrentPosition();
		for(int i = 0 ; i< Network.size();i++){
			Node dst = Network.get(i);
			PositionProtocolImpl pp = (PositionProtocolImpl) dst.getProtocol(position_pid);
			if((pp.getCurrentPosition().distance(pos)) <= this.getScope()) {
				if(variance) {
					EDSimulator.add(CommonState.r.nextPoisson(latency),msg, dst, my_pid);
				}else {
					EDSimulator.add(this.latency,msg, dst, my_pid);
				}			
			}
		}
		EDSimulator.add(m.getPid(), new HeartBeatMessage(host.getID(),host.getID(),my_pid,m), host, my_pid);
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
			em.map = new HashMap<>();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return em;
	}

	@Override
	public void attach(NeighborhoodListener nl) {
		list.add(nl);
		
	}

	@Override
	public void detach(NeighborhoodListener nl) {
		list.remove(nl);
		
	}

	@Override
	public void notifyAddListener(Node node, Long newId) {
		for(int i=0; i<list.size(); i++) {
			System.out.println(list.size());
			list.get(i).newNeighborDetected(node, newId);
		}
		
	}
	
	@Override
	public void notifyRemoveListener(Node node, Long newId) {
		for(int i=0; i<list.size(); i++) {
			list.get(i).lostNeighborDetected(node, newId);
		}
	}

}
