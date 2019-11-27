package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ara.manet.Monitorable;
import ara.manet.communication.Emitter;
import ara.manet.communication.EmitterImpl;
import ara.manet.detection.HeartBeatMessage;
import ara.manet.detection.NeighborProtocol;
import ara.manet.detection.NeighborhoodListener;
import ara.manet.detection.ProbeMessage;
import ara.manet.detection.RemoveMessage;
import ara.manet.detection.ReplyMessage;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocolImpl;
import ara.util.Message;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;


public class VKT04 implements ElectionProtocol, Monitorable, NeighborProtocol, NeighborhoodListener{

	public enum Etat {
		NOTKNOWN,KNOWN,LEADER
	}
	private static final String PAR_PROBE = "probe";
	private static final String PAR_TIMER = "timer";
	private static final String PAR_SCOPE = "scope";
	private static final String PAR_LATENCY = "latency";
	public static final String init_value_event = "INITEVENT";
	private static final String PAR_EMITTER = "emit";

	
	private final int emitter_pid;
	private final int latency;
	private final int scope;
	private final int my_pid;
	private final Emitter emitter;
	private final int probe;
	private final int timer;
	
	private int value;
	private Map<Long, Long> timeout_map; //map pour se souvenir de ses voisins et leur timeout
	
	private int leaderValue;
	private long leaderIdInformation;	
	private Etat state;	
	private boolean inElection;
	private long parent;
	private boolean ackParentDone;
	private long leaderId;	
	private List<Long> neighbors;
	private Set<Long> ackHeard;
	private int computation_num; //Tuple <num,node ID>
	private Long computation_id;
	
	public VKT04(String prefix) {
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTER);
		this.latency = Configuration.getInt("protocol.emit." + PAR_LATENCY);
		this.scope = Configuration.getInt("protocol.emit." + PAR_SCOPE);
		this.probe = Configuration.getInt(prefix + "." + PAR_PROBE);
		this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
		this.emitter = (Emitter) Configuration.getInstance("protocol." + PAR_EMITTER);
		this.neighbors = new ArrayList<Long>();
		this.leaderId = -1;
		this.parent = -1;
		inElection = false;
		leaderValue = -1;
		state = Etat.NOTKNOWN;
		ackHeard = new HashSet<>();
		timeout_map = new HashMap<>();
		computation_num = 0;
		computation_id = -1L;
	}
	
	// Ajout de message de type "broadcast" dans la fil d'execution de emitter
	public void emit(Node n, int pid, Message event) {
		EmitterImpl e = ((EmitterImpl) n.getProtocol(emitter_pid));
		e.emit(n, event);
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		// TODO Auto-generated method stub
		if (pid != my_pid) {
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if (event instanceof ElectionMessage) {
			ElectionMessage m = (ElectionMessage) event;
			ElectionMessage msg ;		
			
			if(parent ==-1 && inElection == false) { //parent not defined : either source or first time receiving electionmessage from parent
				if(m.getSource() != node.getID()) {// si Noeud node n'est pas la source d'éléction
					parent = m.getIdSrc();
					ackHeard.remove(parent);
					computation_num = m.getComputationNum();
					computation_id = m.getComputationId();
				}
				else {//si je suis l'initiateur d'élection
					computation_num++;
					computation_id = m.getComputationId();
				}
				inElection = true;
				for(long neighbor : this.getNeighbors()) {
					Node dest = Network.get((int) neighbor);
					if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
						continue; 
					}
					msg = new ElectionMessage(node.getID(), dest.getID(), my_pid,m.getSource(),computation_num,computation_id);
					emitter.emit(node, msg);
//					EDSimulator.add(latency,msg, dest, my_pid);
				}
			}else {//conflict need to test computation-index
				Node dest = Network.get((int) m.getIdSrc());
				VKT04 vktp = (VKT04) dest.getProtocol(my_pid);
				if( (m.getComputationNum() > computation_num) || (m.getComputationNum()==computation_num && m.getComputationId() > computation_id) ) {
					//System.out.println("noeaud "+node.getID()+" a recu un computation index sup de "+m.getIdSrc()+" "+computation_num+" "+m.getComputationNum()+" "+m.getComputationId()+" "+computation_id);
					parent = m.getIdSrc();
					computation_num = m.getComputationNum();
					computation_id = m.getComputationId();
					ackHeard.clear();
					ackHeard.addAll(neighbors);
					ackHeard.remove(parent);
					for(long neighbor : this.getNeighbors()) {
						dest = Network.get((int) neighbor);
						if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
							continue; 
						}
						msg = new ElectionMessage(node.getID(), dest.getID(), my_pid,m.getSource(),computation_num,computation_id);
						emitter.emit(node, msg);
//						EDSimulator.add(latency,msg, dest, my_pid);
					}
				}else {
					AckMessage amsg = new AckMessage(node.getID(), dest.getID(), my_pid,-1,-1);
					//EDSimulator.add(latency,amsg, dest, my_pid);
					emitter.emit(node, amsg);
				}
			}
		}
		if (event instanceof AckMessage) { // A la réception de heartbeat
			//EXO 2 Q1 hypothése 5 à réaliser pour test scope à réception
			AckMessage m = (AckMessage) event;
			if(m.getValue() > leaderValue) {
				leaderValue = m.getValue();
				leaderIdInformation = m.getIdLeader();
			}
			ackHeard.remove(m.getIdSrc());
			if(ackHeard.isEmpty()) {
				if(parent == -1) {//parent of election receive all ack
					leaderId = leaderIdInformation; // vu que head a recu tt ack donc il connait le leaderId 
					inElection = false;
					for(long neighbor : this.getNeighbors()) {
						Node dest = Network.get((int) neighbor);
						if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
							continue; 
						}
						LeaderMessage msg = new LeaderMessage(node.getID(), dest.getID(), my_pid, leaderId,leaderValue);
						if(leaderValue == value) {
							state = Etat.LEADER;
						}else {
							state = Etat.KNOWN;
						}
						emitter.emit(node, msg);
						//EDSimulator.add(latency,msg, dest, my_pid);
					}
				}else {
					Node dest = Network.get((int)parent);
					AckMessage msg = new AckMessage(node.getID(), parent, my_pid, leaderIdInformation,leaderValue);
					EDSimulator.add( latency,msg, dest, my_pid);
					ackParentDone = true;
					//send msg ack to parent;
				}
			}
		}
		if (event instanceof LeaderMessage) { // A la réception de heartbeat
			//EXO 2 Q1 hypothése 5 à réaliser pour test scope à réception
			LeaderMessage m = (LeaderMessage) event;
			inElection = false;
			if(state == Etat.NOTKNOWN) {
				leaderValue = m.getLeaderValue();
				leaderId = m.getIdLeader();
				if(m.getLeaderValue() == value) {
					state = Etat.LEADER;
				}else {
					state = Etat.KNOWN;
				}
				for(long neighbor : this.getNeighbors()) {
					Node dest = Network.get((int) neighbor);
					if(dest.equals(node)) {// éviter d'ajouter soimeme
						continue;
					}
					EDSimulator.add(latency,m, dest, my_pid);
				}
			}else if (state != Etat.NOTKNOWN) {
				if(leaderValue < m.getLeaderValue()) {
					leaderValue = m.getLeaderValue();
					leaderId = m.getIdLeader();
					for(long neighbor : this.getNeighbors()) {
						Node dest = Network.get((int) neighbor);
						if(dest.equals(node)) {// éviter d'ajouter soimeme
							continue;
						}
						LeaderMessage lmsg = new LeaderMessage(node.getID(), dest.getID(), my_pid, leaderId,leaderValue);
						emitter.emit(node, lmsg);
					}
				}
			}
		}
		if (event instanceof ProbeMessage) {
			ProbeMessage pbmsg = (ProbeMessage) event;
			int idsrc = (int) pbmsg.getIdSrc();
			if (idsrc == node.getID()) {//Si je recois mon ProbeMessage relancer heartbeat dans probe tps
				//emit(node, my_pid,new HeartBeatMessage(node.getID(), node.getID(), my_pid));
				//processEvent(node,my_pid,new HeartBeatMessage(idsrc,idsrc,my_pid));
				EDSimulator.add(probe, new HeartBeatMessage(idsrc,idsrc,my_pid), node, my_pid);
			}else {
				emitter.emit(node, new ReplyMessage(node.getID(),idsrc,my_pid));
				
			}
		}
		if (event instanceof ReplyMessage) {
			ReplyMessage m = (ReplyMessage) event;
			int idsrc = (int) m.getIdSrc();
			if (!neighbors.contains((long) idsrc)) {
				neighbors.add(m.getIdSrc());
				ackHeard.add(m.getIdSrc());
				//new neighor Listener
				newNeighborDetected(node, idsrc);
			}
			EDSimulator.add(timer+49, new RemoveMessage(node.getID(),node.getID(),my_pid, idsrc), node, my_pid); // timer+49 to adjust visual effect
			timeout_map.put(m.getIdSrc(), CommonState.getTime()+timer);
		}
		if (event instanceof HeartBeatMessage) {
			HeartBeatMessage msg = (HeartBeatMessage) event;
			emitter.emit(node,new ProbeMessage(node.getID(),Emitter.ALL,my_pid));
			//emit(node,my_pid,new ProbeMessage(node.getID(),Emitter.ALL,my_pid));
		}
		if (event instanceof RemoveMessage) {//remove ne marche pas
			RemoveMessage m = (RemoveMessage) event;
			if(timeout_map.containsKey(m.getTargetId())){
				if(timeout_map.get(m.getTargetId()) <= CommonState.getTime()) {
					neighbors.remove(m.getTargetId());
					// lost neighbor Listener
					lostNeighborDetected(node, m.getTargetId());
				}
			}
		}
		if (event instanceof String) {
			String ev = (String) event;
			if (ev.equals(init_value_event)) {
				value = (int) node.getID();
				leaderValue = value;
				computation_id = node.getID();
				int position_pid=Configuration.lookupPid("position");
				PositionProtocolImpl p = (PositionProtocolImpl) Network.get((int) node.getID()).getProtocol(position_pid);
				Position pos = p.getCurrentPosition();
				for(int i = 0 ; i< Network.size();i++){
					Node dst = Network.get(i);
					if(dst.equals(Network.get(value))) {// éviter d'ajouter soi-meme
						continue;
					}
					PositionProtocolImpl pp = (PositionProtocolImpl) dst.getProtocol(position_pid);
					if((pp.getCurrentPosition().distance(pos)) <= scope) {
						neighbors.add(dst.getID());
						ackHeard.add(dst.getID());
					}
				}
				return;
			}
		}
	}

	@Override
	public List<Long> getNeighbors() {
		// TODO Auto-generated method stub		
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
	
	public void newNeighborDetected(Node host, long id_new_neighbor) {
		//System.out.println("Ajout du voisin "+id_new_neighbor+" dans la liste des voisins de "+host.getID());
		ackHeard.add(id_new_neighbor);
		if(!inElection) {
			LeaderMessage lmsg = new LeaderMessage(host.getID(), id_new_neighbor, my_pid, leaderId,leaderValue);
			emitter.emit(host, lmsg);
		}else {
			
		}
			
		//lancer un Election message sur lui
	}

	/* appelé lorsque le noeud host détecte la perte d'un voisin */
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {
		//System.out.println("Suppresion du voisin "+id_lost_neighbor+" dans la liste des voisins de "+host.getID());
		if(inElection) {
			if(ackHeard.contains(id_lost_neighbor)) {
				ackHeard.remove(id_lost_neighbor);
			}//????
		}else {
			ElectionMessage emsg = new ElectionMessage(host.getID(),host.getID(),my_pid, host.getID(), computation_num,value);
			emitter.emit(host, emsg);
			
		}
		
	}

	@Override
	public VKT04 clone() {
		VKT04 vkt = null;
		try {
			vkt = (VKT04) super.clone();
			vkt.neighbors = new ArrayList<Long>();
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return vkt;
	}

	@Override
	public void attach(NeighborhoodListener nl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detach(NeighborhoodListener nl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyAddListener(Node node, Long newId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyRemoveListener(Node node, Long newId) {
		// TODO Auto-generated method stub
		
	}


}
