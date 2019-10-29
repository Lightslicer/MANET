package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.List;

import ara.manet.Monitorable;
import ara.manet.communication.Emitter;
import ara.manet.detection.NeighborProtocol;
import ara.manet.detection.NeighborhoodListener;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;


public class VKT04Statique implements ElectionProtocol, Monitorable, NeighborProtocol{

	public enum Etat {
		NOTKNOWN,KNOWN,LEADER
	}
	
	private static final String PAR_SCOPE = "scope";
	private static final String PAR_LATENCY = "latency";
	public static final String init_value_event = "INITEVENT";
	
	private int value;
	private long leaderId;
	
	private int leaderValue;
	private long leaderIdInformation;
	private long parent;
	private int childrenCount;
	private int ackCount;
	private Etat state;
	
	private static final String PAR_PROBE = "probe";
	private static final String PAR_TIMER = "timer";
//	private static final String PAR_EMITTER = "emit";
	
	private final int my_pid;
	private final int probe;
	private final int timer;
//	private final Emitter emitter;
	private List<Long> neighbors ;
	
	public VKT04Statique(String prefix) {
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.probe = Configuration.getInt(prefix + "." + PAR_PROBE);
		this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
//		this.emitter = (Emitter) Configuration.getInstance("protocol." + PAR_EMITTER);
		this.neighbors = new ArrayList<Long>();
		this.leaderId = -1;
		this.parent = -1;
		leaderValue = -1;
		childrenCount = 0;
		state = Etat.NOTKNOWN;
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		// TODO Auto-generated method stub
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if (event instanceof ElectionMessage) { // A la réception de heartbeat
			//EXO 2 Q1 hypothése 5 à résaliser pour test scope à réception
			ElectionMessage m = (ElectionMessage) event;
			if(parent == -1) { //parent not defined : either source or alrdy received electionmessage from parent
				if(m.getSource() != node.getID()) {// si Noeud node n'est pas la source d'éléction
					parent = m.getIdSrc();
				}
				for(long neighbor : this.getNeighbors()) {
					Node dest = Network.get((int) neighbor);
					if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
						continue; 
					}
					ElectionMessage msg = new ElectionMessage(node.getID(), dest.getID(), my_pid,m.getSource());
					EDSimulator.add(Configuration.getInt("protocol.vkt." + PAR_LATENCY),msg, dest, my_pid);
					childrenCount++;
				}
			}else {
				Node dest = Network.get((int) m.getIdSrc());
				VKT04Statique vktp = (VKT04Statique) dest.getProtocol(my_pid);
				AckMessage msg = new AckMessage(node.getID(), dest.getID(), my_pid,-1,-1);
				//vktp.processEvent(dest, my_pid, msg);
				EDSimulator.add(Configuration.getInt("protocol.vkt." + PAR_LATENCY),msg, dest, my_pid);
			}
		}
		if (event instanceof AckMessage) { // A la réception de heartbeat
			//EXO 2 Q1 hypothése 5 à réaliser pour test scope à réception
			AckMessage m = (AckMessage) event;
			if(m.getValue() > leaderValue) {
				leaderValue = m.getValue();
				leaderIdInformation = m.getIdLeader();
			}
			ackCount++;
			if(childrenCount == ackCount) {
				if(parent == -1) {//parent of election receive all ack
					leaderId = leaderIdInformation; // vu que head a recu tt ack donc il connait le leaderId 
					for(long neighbor : this.getNeighbors()) {
						Node dest = Network.get((int) neighbor);
						if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
							continue; 
						}
						LeaderMessage msg = new LeaderMessage(node.getID(), dest.getID(), my_pid, leaderId,leaderValue);
						//vktp.processEvent(dest, my_pid, msg); better use EDSimulator.add to simulate 
						if(leaderValue == value) {
							state = Etat.LEADER;
						}else {
							state = Etat.KNOWN;
						}
						EDSimulator.add(Configuration.getInt("protocol.vkt." + PAR_LATENCY),msg, dest, my_pid);
					}
				}else {
					Node dest = Network.get((int)parent);
					AckMessage msg = new AckMessage(node.getID(), parent, my_pid, leaderIdInformation,leaderValue);
					EDSimulator.add(Configuration.getInt("protocol.vkt." + PAR_LATENCY),msg, dest, my_pid);
					//send msg ack to parent;
				}
			}
		}
		if (event instanceof LeaderMessage) { // A la réception de heartbeat
			//EXO 2 Q1 hypothése 5 à réaliser pour test scope à réception
			LeaderMessage m = (LeaderMessage) event;
			if(state == Etat.NOTKNOWN) {
				for(long neighbor : this.getNeighbors()) {
					Node dest = Network.get((int) neighbor);
					if(dest.equals(node)) {// éviter d'ajouter soimeme
						continue;
					}
					//vktp.processEvent(dest, my_pid, msg); better use EDSimulator.add to simulate 
					if(m.getLeaderValue() == value) {
						state = Etat.LEADER;
					}else {
						state = Etat.KNOWN;
					}
					EDSimulator.add(Configuration.getInt("protocol.vkt." + PAR_LATENCY),m, dest, my_pid);
				}
			}
		}
		//  init process to configure node value
		if (event instanceof String) {
			String ev = (String) event;
			if (ev.equals(init_value_event)) {
				value = (int) node.getID();
				leaderValue = value;
				return;
			}
		}
	}

	@Override
	public List<Long> getNeighbors() {
		// TODO Auto-generated method stub
		int position_pid=Configuration.lookupPid("position");
		PositionProtocolImpl p = (PositionProtocolImpl) Network.get(value).getProtocol(position_pid);
		Position pos = p.getCurrentPosition();
		for(int i = 0 ; i< Network.size();i++){
			Node dst = Network.get(i);
			if(dst.equals(Network.get(value))) {// éviter d'ajouter soi-meme
				continue;
			}
			PositionProtocolImpl pp = (PositionProtocolImpl) dst.getProtocol(position_pid);
			if((pp.getCurrentPosition().distance(pos)) <= Configuration.getInt("protocol.vkt." + PAR_SCOPE)) {
				neighbors.add(dst.getID());
			}
		}
		return neighbors;
	}

	@Override
	public long getIDLeader() {
		// TODO Auto-generated method stub
		if(leaderId != -1) {
			return leaderId;
		}else {
			return -1;
		}
	}

	@Override
	public int getValue() {
		// TODO Auto-generated method stub
		return value;
	}
	
	
	/* permet d'obtenir le nombre d'état applicatif du noeud */
	public int nbState() {
		return 3;
	}

	/* permet d'obtenir l'état courant du noeud */
	public int getState(Node host) {
		if(state == Etat.NOTKNOWN) {
			return 0;
		}else if (state == Etat.KNOWN) {
			return 1;
		}else { //être le leader
			return 2;
		}
	}

	@Override
	public VKT04Statique clone() {
		VKT04Statique vkt = null;
		try {
			vkt = (VKT04Statique) super.clone();
			vkt.neighbors = new ArrayList<Long>();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return vkt;
	}


}
