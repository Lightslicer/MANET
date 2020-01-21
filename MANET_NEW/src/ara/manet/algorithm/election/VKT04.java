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
	private static final String PAR_BEACON_INTERVAL = "beaconinterval";
	private static final String PAR_MAX_BEACON_LOSS = "maxbeaconloss";


	private final int emitter_pid;
	private final int latency;
	private final int scope;
	private final int my_pid;
	private final Emitter emitter;
	private final int probe;
	private final int timer;
	private final int beacon_interval;
	private final int max_beacon_loss;

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
	private Pair<Integer,Long> computation_index;
	private int computation_num; //Tuple <num,node ID>
	private Long computation_id;
	private int beacon_loss_count;
	private int arrived_beacon_counter;
	private long oldLostLeaderId;

	public VKT04(String prefix) {
		String tmp[] = prefix.split("\\.");
		my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTER);
		this.latency = Configuration.getInt("protocol.emit." + PAR_LATENCY);
		this.scope = Configuration.getInt("protocol.emit." + PAR_SCOPE);
		this.probe = Configuration.getInt(prefix + "." + PAR_PROBE);
		this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
		this.beacon_interval = Configuration.getInt(prefix + "." + PAR_BEACON_INTERVAL);
		this.max_beacon_loss = Configuration.getInt(prefix + "." + PAR_MAX_BEACON_LOSS);
		this.emitter = (Emitter) Configuration.getInstance("protocol." + PAR_EMITTER);
		this.neighbors = new ArrayList<Long>();
		this.leaderId = -1;
		this.parent = -1;
		inElection = false;
		leaderValue = -1;
		state = Etat.LEADER;
		ackHeard = new HashSet<>();
		timeout_map = new HashMap<>();
		computation_index = new Pair<Integer, Long>(0, -1L);
		computation_num = 0;
		computation_id = -1L;
		beacon_loss_count = 0;
		arrived_beacon_counter = 0;
		oldLostLeaderId = -1;
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
			if(inElection == false & leaderId == -1) { //parent not defined : either source or first time receiving electionmessage from parent
				if(node.getID() == 9){
					System.out.println(node.getID()+" elec 1 sender : "+ m.getIdSrc()+" source "+m.getSource()+" time "+CommonState.getIntTime());
				}
				if(m.getSource() != node.getID()) {// si Noeud node n'est pas la source d'éléction
					parent = m.getIdSrc();
					ackHeard.remove(parent);
					computation_index = m.getComputationIndex();
				}
				else {
					computation_num = m.getComputationIndex().getNum()+1;
					computation_id = m.getSource();
					computation_index = new Pair<Integer, Long>(computation_num, computation_id);

					//					System.out.println(computation_index.getNum()+" "+computation_index.getId());
				}
				ackHeard.clear();
				ackHeard.addAll(neighbors);
				ackHeard.remove(parent);
				inElection = true;
				ackParentDone = false;
				if(neighbors.size() == 1 & parent != -1){ // un seul voisin et c le parent donc pas de elec mais ack vers parent
					if(node.getID() == 9){
						System.out.println(node.getID()+" elec 1 ack to unique neighbor "+m.getSource()+" time "+CommonState.getIntTime());
					}
					Node dest = Network.get((int) parent);
					AckMessage amsg = new AckMessage(node.getID(), dest.getID(), my_pid, node.getID(), value,computation_index);//feuille donc il renvoie ses propres valeurs
					emitter.emit(node, amsg);
				}else if (neighbors.size() == 1 & parent == -1) {
					if(node.getID() == 9){
						System.out.println(node.getID()+" elec 1 init éléct im the parent"+m.getSource()+" time "+CommonState.getIntTime());
					}
					for(long neighbor : this.getNeighbors()) {
						Node dest = Network.get((int) neighbor);
						if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
							continue; 
						}
						if(node.getID() == 9){
							System.out.println(node.getID()+" elec 1 send to "+dest.getID()+" time "+CommonState.getIntTime());
						}
						msg = new ElectionMessage(node.getID(), dest.getID(), my_pid,m.getSource(),computation_index);
						emitter.emit(node, msg);
						//						EDSimulator.add(latency,msg, dest, my_pid);
					}
				}
				else if(neighbors.size() > 1) {
					if(node.getID() == 9){
						System.out.println(node.getID()+" elec 1 got neighbor"+m.getSource()+" time "+CommonState.getIntTime());
					}
					for(long neighbor : this.getNeighbors()) {
						Node dest = Network.get((int) neighbor);
						if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
							continue; 
						}
						msg = new ElectionMessage(node.getID(), dest.getID(), my_pid,m.getSource(),computation_index);
						emitter.emit(node, msg);
						//						EDSimulator.add(latency,msg, dest, my_pid);
					}
				}else {//neighbor size 0
					if(node.getID() == 9){
						System.out.println(node.getID()+" elec 1 no neighbor self elected leader "+m.getSource()+" time "+CommonState.getIntTime());
					}
					inElection=false;
					state = Etat.LEADER;
					leaderId = value;
					leaderValue = value;
					leaderIdInformation = value;
				}
			}else if(inElection == true & leaderId == -1){//conflict need to test computation-index
				Node dest = Network.get((int) m.getIdSrc());
				VKT04 vktp = (VKT04) dest.getProtocol(my_pid);
				if(node.getID() == 9){
					System.out.println(node.getID()+" elec 2 sender: " + m.getIdSrc()+" source : "+m.getSource()+" time "+CommonState.getIntTime());
				}
				//				if(m.getSource() == node.getID()) {
				//					//here case if elec trigger of node himself
				//					//arrived after another elec from other nodes
				//					if(node.getID() == 9){
				//						System.out.println(node.getID()+" elec 2 trigger late : IGNORED");
				//					}
				//				}else { => other bug

				if( (m.getComputationNum() > computation_index.getNum()) || (m.getComputationNum()==computation_index.getNum() && m.getComputationId() > computation_index.getId()) ) {
					//System.out.println("noeaud "+node.getID()+" a recu un computation index sup de "+m.getIdSrc()+" "+computation_num+" "+m.getComputationNum()+" "+m.getComputationId()+" "+computation_id);
					if(node.getID() == 9){
						System.out.println(node.getID()+" source " + m.getIdSrc()+ " posséde un computation supérieur");
					}
					// 35 send elec to himself after 20, so this execution and name
					//himself parent
					if(m.getSource() != node.getID()) {
						parent = m.getIdSrc();
					}else {
						parent = -1;
					}
					computation_index = m.getComputationIndex();
					ackHeard.clear();
					ackHeard.addAll(neighbors);
					ackHeard.remove(parent);
					if(neighbors.size() == 1 & parent != -1){ // un seul voisin et c le parent donc pas de elec mais ack vers parent
						dest = Network.get((int) parent);
						if(node.getID() == 9){
							System.out.println(node.getID()+" send ack to " + dest.getID());
						}
						AckMessage amsg = new AckMessage(node.getID(), dest.getID(), my_pid, node.getID(), value,computation_index);//feuille donc il renvoie ses propres valeurs
						ackParentDone = true;
						//emitter.emit(node, amsg);
						EDSimulator.add(latency+5,amsg, dest, my_pid);

					}else if (neighbors.size() == 1 & parent == -1) {
						for(long neighbor : this.getNeighbors()) {
							dest = Network.get((int) neighbor);
							if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
								continue; 
							}
							if(node.getID() == 9){
								System.out.println(node.getID()+" elec source " + m.getIdSrc()+ " posséde un computation supérieur");
							}
							msg = new ElectionMessage(node.getID(), dest.getID(), my_pid,m.getSource(),computation_index);
							emitter.emit(node, msg);
						}

					}
					else {//(neighbors.size() > 1) 
						for(long neighbor : this.getNeighbors()) {
							dest = Network.get((int) neighbor);
							if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
								continue; 
							}
							if(node.getID() == 9){
								System.out.println(node.getID()+" send elec to " + dest.getID());
							}
							msg = new ElectionMessage(node.getID(), dest.getID(), my_pid,m.getSource(),computation_index);
							emitter.emit(node, msg);
						}
					}

				}else {
					if(node.getID() == 9){
						System.out.println(node.getID()+" elec 2 send null ack to dest "+dest.getID());
					}
					AckMessage amsg = new AckMessage(node.getID(), dest.getID(), my_pid,-1,-1,computation_index);
					emitter.emit(node, amsg);	

				}


			}else if(inElection == false & leaderId != -1){// inElectionfalse et connait le leader
				if(node.getID() == 9){
					System.out.println(node.getID()+" elec connait son leader src " + m.getIdSrc()+" source "+m.getSource()+" inElection "+inElection+" leaderId "+leaderId+" local "+computation_index.getNum()+" "+computation_index.getId()+" "+m.getComputationNum()+" "+m.getComputationId());
				}
				if( (m.getComputationNum() == computation_index.getNum()) && m.getComputationId() == computation_index.getId())  {
					//nothing
					if(node.getID() == 9){
						System.out.println(node.getID()+" nothing happens src " + m.getIdSrc()+" source "+m.getSource()+" inElection "+inElection+" leaderId "+leaderId);
					}
				}else {
					if(m.getSource() != node.getID()) {// si Noeud node n'est pas la source d'éléction
						parent = m.getIdSrc();
						ackHeard.remove(parent);
						oldLostLeaderId = leaderId;
						leaderIdInformation = value;
						leaderValue = value;					
						leaderId = -1;
						state=Etat.NOTKNOWN;
						computation_index = m.getComputationIndex();
					}
					//				Node dest = Network.get((int) m.getIdSrc());
					//				AckMessage amsg = new AckMessage(node.getID(), dest.getID(), my_pid,leaderId, leaderValue);
					//				emitter.emit(node, amsg);
					inElection = true;
					ackParentDone = false;
					if(neighbors.size() == 1 & parent != -1){ // un seul voisin et c le parent donc pas de elec mais ack vers parent
						Node dest = Network.get((int) parent);
						AckMessage amsg = new AckMessage(node.getID(), dest.getID(), my_pid, node.getID(), value,computation_index);//feuille donc il renvoie ses propres valeurs
						ackParentDone = true;
						emitter.emit(node, amsg);
					}else if(neighbors.size() > 1) {
						for(long neighbor : this.getNeighbors()) {
							Node dest = Network.get((int) neighbor);
							if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
								continue; 
							}
							msg = new ElectionMessage(node.getID(), dest.getID(), my_pid,m.getSource(),computation_index);
							emitter.emit(node, msg);
							//						EDSimulator.add(latency,msg, dest, my_pid);
						}
					}else {//neighbor size 0				
						inElection=false;
						state = Etat.LEADER;
						leaderId = value;
						leaderValue = value;					
					}
				}
			}else {//inElection true et connait son leader
				inElection=false;
			}

		}
		if (event instanceof AckMessage) { // A la réception de heartbeat
			AckMessage m = (AckMessage) event;
			if(node.getID() == 9){
				System.out.println(node.getID()+" recu ackMessage de " +m.getIdSrc()+" message value: "+m.getValue()+" present value "+leaderValue+"leader Id proposed: "+m.getIdLeader()+" time "+CommonState.getIntTime());
			}			
			if(m.getComputationNum() == computation_index.getNum() && m.getComputationId() == computation_index.getId()) {
				if(m.getValue() >= leaderValue) {

					leaderValue = m.getValue();
					leaderIdInformation = m.getIdLeader();
					if(node.getID() == 9){
						System.out.println(node.getID()+" leaderIdInfo changed now: " +leaderIdInformation);
					}
				}
				ackHeard.remove(m.getIdSrc());
				if(ackHeard.isEmpty()) {
					if(node.getID() == 9){
						System.out.println(node.getID()+" ackMessage empty time "+CommonState.getIntTime());
					}
					if(parent == -1) {//parent of election receive all ack
						leaderId = leaderIdInformation; // vu que head a recu tt ack donc il connait le leaderId 
						inElection = false;
						if(node.getID() == 9){
							System.out.println(node.getID()+" ackMessage empty decide leader "+leaderId+ " leaderValue "+leaderValue+" value "+value);
						}

						if(leaderValue == value) {
							state = Etat.LEADER;
							if(node.getID() == 9){
								System.out.println(node.getID()+" become Leader et envoie beacon");
							}
							emitter.emit(node,new BeaconMessage(node.getID(),Emitter.ALL,my_pid, 1));
						}else {
							if(node.getID() == 9){
								System.out.println(node.getID()+" éléction valid leader known");
							}
							LeaderMessage msg = new LeaderMessage(node.getID(), node.getID(), my_pid, leaderId,leaderValue);

							arrived_beacon_counter = 0;
							emitter.emit(node, msg);
							state = Etat.KNOWN;
							//							BeaconExpiredMessage bem = new BeaconExpiredMessage(node.getID(), node.getID(), my_pid, leaderId,arrived_beacon_counter);
							//							EDSimulator.add(beacon_interval*max_beacon_loss, bem, node, my_pid);
						}
						for(long neighbor : this.getNeighbors()) {
							Node dest = Network.get((int) neighbor);
							if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
								continue; 
							}
							if(node.getID() == 9){
								System.out.println(node.getID()+" envoie leaderMessage à " + dest.getID()+" avec leaderId "+leaderId+" time "+CommonState.getIntTime());
							}
							LeaderMessage msg = new LeaderMessage(node.getID(), dest.getID(), my_pid, leaderId,leaderValue);

							arrived_beacon_counter = 0;
							emitter.emit(node, msg);
						}
					}else {
						Node dest = Network.get((int)parent);
						if(node.getID() == 9){
							System.out.println(node.getID()+" envoie ackMessage vers " +dest.getID()+" leader Id last proposed: "+m.getIdLeader()+" value proposed "+leaderValue+"leaderInfo "+leaderIdInformation);
						}
						AckMessage msg = new AckMessage(node.getID(), dest.getID(), my_pid, leaderIdInformation,leaderValue,computation_index);
						EDSimulator.add(latency,msg, dest, my_pid);
						ackParentDone = true;
						//send msg ack to parent;
					}
				}
			}
		}
		if (event instanceof LeaderMessage) { // A la réception de heartbeat
			//EXO 2 Q1 hypothése 5 à réaliser pour test scope à réception
			LeaderMessage m = (LeaderMessage) event;
			if(node.getID() == 9){
				System.out.println(node.getID()+" recu leaderMessage de "+m.getIdSrc()+ " etat "+state+" ackParentDone :"+ackParentDone+ " parent "+parent);
			}

			if(state == Etat.NOTKNOWN) {

				if(ackParentDone) {
					leaderValue = m.getLeaderValue();
					leaderId = m.getIdLeader();
					arrived_beacon_counter = 0;
					if(m.getLeaderValue() == value) {
						if(node.getID() == 9){
							System.out.println(node.getID()+" émission beacon");
						}
						state = Etat.LEADER;
						emitter.emit(node,new BeaconMessage(node.getID(),Emitter.ALL,my_pid, 1));
					}else {
						state = Etat.KNOWN;
						BeaconExpiredMessage bem = new BeaconExpiredMessage(node.getID(), node.getID(), my_pid,  leaderId,1);
						EDSimulator.add(beacon_interval*max_beacon_loss, bem, node, my_pid);
						if(node.getID() == 9){
							System.out.println(node.getID()+" armement beaconexpired");
						}
						if(parent == -1 && inElection == true) {
							parent = m.getIdSrc();
							inElection = false;
						}
					}

					for(long neighbor : this.getNeighbors()) {
						Node dest = Network.get((int) neighbor);
						//					if(dest.equals(node)) {// éviter d'ajouter soimeme
						//						continue;
						//					}
						if(dest.getID() == parent) { //this neighbor is parent dont propagete leaderMessage
							continue; 
						}
						LeaderMessage lmsg = new LeaderMessage(node.getID(), dest.getID(), my_pid, leaderId,leaderValue);
						emitter.emit(node, lmsg);
						//						EDSimulator.add(latency,m, dest, my_pid);
					}
					inElection = false;
				}
			}else if (state != Etat.NOTKNOWN) {
				if(leaderValue < m.getLeaderValue()) {//if local leaderValue < m.value
					leaderValue = m.getLeaderValue();
					leaderId = m.getIdLeader();
					state = Etat.KNOWN;
					parent = m.getIdSrc();
					//					arrived_beacon_counter = 0;

					for(long neighbor : this.getNeighbors()) {
						Node dest = Network.get((int) neighbor);
						if(dest.equals(node) || neighbor == parent) {// éviter d'ajouter soimeme
							continue;
						}
						LeaderMessage lmsg = new LeaderMessage(node.getID(), dest.getID(), my_pid, leaderId,leaderValue);
						emitter.emit(node, lmsg);
					}
					BeaconExpiredMessage bem = new BeaconExpiredMessage(node.getID(), node.getID(), my_pid, leaderId,arrived_beacon_counter);
					EDSimulator.add(beacon_interval*max_beacon_loss, bem, node, my_pid);
					if(node.getID() == 9){
						System.out.println(node.getID()+" armement beaconexpired 2 source"+m.getIdSrc()+"value "+ leaderId);
					}
				}else if (leaderValue == m.getLeaderValue()) {
					//leaderMessage issu d'un broadcast d'un voisin
					//même valeur donc ignorer
				}else {//leader
					if(node.getID() == 9){
						System.out.println("leader treatement here");
					}
					emitter.emit(node,new BeaconMessage(node.getID(),Emitter.ALL,my_pid, arrived_beacon_counter+1));
				}
				inElection = false;
				/*
				 * Idée implémenter if else sur if idSrc == parent
				 */
			}else {

			}

		}
		if(event instanceof BeaconMessage) {
			BeaconMessage m = (BeaconMessage) event;
			if(node.getID() == 9){
				//System.out.println(node.getID()+" BeaconMessage src: "+m.getIdSrc()+" beaconcounter "+m.getTimeStamp()+" awaited "+arrived_beacon_counter);
			}
			if(state == Etat.LEADER) {
				//
				if(m.getTimeStamp() == arrived_beacon_counter+1) {
					arrived_beacon_counter++;
					BeaconMessage msg = new BeaconMessage(node.getID(),Emitter.ALL,my_pid,m.getTimeStamp()+1);
					emitter.emit(node,msg);
				}
			}else {
				//check awaitedcounter and received value
				if(m.getTimeStamp() >= arrived_beacon_counter+1) {
					arrived_beacon_counter = m.getTimeStamp();
					emitter.emit(node,m);
				}

			}

		}
		if(event instanceof BeaconExpiredMessage) {
			//			System.out.println(node.getID() + " reception d'un beacon expired");

			BeaconExpiredMessage m = (BeaconExpiredMessage) event;

			if(m.getLeaderId() != leaderId) {
				//not the same alarm for the current leader ignore it
				return;
			}
			if(m.getAwaitedCounter() > arrived_beacon_counter) {// soit les expired arrive trop vite, loss=6 avant l'arrivé
				// soit valeur getawaited error, 
				//awaited beaconmessage not arrived
				//				beacon_loss_count++;
				//				EDSimulator.add(beacon_interval, m, node, my_pid);
				//				if(beacon_loss_count == max_beacon_loss) {
				//trigger election & get riede of ex leader

				if(node.getID() == 9){
					System.out.println(node.getID() + " beacon expired");
				}
				leaderValue = -1;
				leaderId = -1;
				state = Etat.NOTKNOWN;	
				leaderId = -1;
				oldLostLeaderId = parent;
				leaderIdInformation = value;//reset leaderIfInformation to self
				leaderValue = value;
				//			}
				//			parent = -1;
				//			state = Etat.NOTKNOWN;
				// / à enlever après pour debug
				if(inElection == false) {
					// here error : always if(m.getAwaitedCounter() > arrived_beacon_counter) 
					parent = -1;
					ElectionMessage emsg = new ElectionMessage(node.getID(),node.getID(),my_pid, node.getID(), computation_index);
					if(node.getID() == 9){
						System.out.println(node.getID() + " beacon expired éléction msg :"+ computation_num +" " + computation_id+" computaion index "+computation_index.getNum()+","+computation_index.getId()+" time "+CommonState.getIntTime());
					}
					emitter.emit(node, emsg);
				}
				//				}
			}else {
				//awaited beaconmessage arrived, reset loss count
				beacon_loss_count = 0;
				BeaconExpiredMessage bem = new BeaconExpiredMessage(node.getID(), node.getID(), my_pid, leaderId,arrived_beacon_counter+1);
				EDSimulator.add(beacon_interval*max_beacon_loss, bem, node, my_pid);
			}

		}
		if (event instanceof ProbeMessage) {
			ProbeMessage pbmsg = (ProbeMessage) event;
			int idsrc = (int) pbmsg.getIdSrc();
			if (idsrc == node.getID()) {//Si je recois mon ProbeMessage relancer heartbeat dans probe tps
				emitter.emit(node, new ProbeMessage(idsrc,Emitter.ALL,my_pid));
			}else {
				emitter.emit(node, new ReplyMessage(node.getID(),idsrc,my_pid, leaderId));
			}
		}
		if (event instanceof ReplyMessage) {
			ReplyMessage m = (ReplyMessage) event;
			int idsrc = (int) m.getIdSrc();
			if (!neighbors.contains((long) idsrc)) {
				neighbors.add(m.getIdSrc());
				if(!inElection) {
					ackHeard.add(m.getIdSrc());
				}
				//new neighor Listener
				newNeighborDetected(node, idsrc, m.getIdLeader());
			}
			EDSimulator.add(timer, new RemoveMessage(node.getID(),node.getID(),my_pid, idsrc), node, my_pid); // timer+49 to adjust visual effect
			timeout_map.put(m.getIdSrc(), CommonState.getTime()+timer);
		}
		if (event instanceof HeartBeatMessage) {
			HeartBeatMessage msg = (HeartBeatMessage) event;
			emitter.emit(node,new ProbeMessage(node.getID(),Emitter.ALL,my_pid));
		}
		if (event instanceof RemoveMessage) {//remove ne marche pas
			RemoveMessage m = (RemoveMessage) event;
			if(timeout_map.containsKey(m.getTargetId())){
				if(timeout_map.get(m.getTargetId()) <= CommonState.getTime()) {
					neighbors.remove(m.getTargetId());
					ackHeard.remove(m.getTargetId());
					lostNeighborDetected(node, m.getTargetId());
				}
			}
		}
		if (event instanceof String) {
			String ev = (String) event;
			if (ev.equals(init_value_event)) {
				value = (int) node.getID();
				leaderValue = value;
				//				computation_id = node.getID();
				//				computation_index = new Pair<Integer, Long>(computation_num, computation_id);
				//				int position_pid=Configuration.lookupPid("position");
				//				PositionProtocolImpl p = (PositionProtocolImpl) Network.get((int) node.getID()).getProtocol(position_pid);
				//				Position pos = p.getCurrentPosition();
				//				for(int i = 0 ; i< Network.size();i++){
				//					Node dst = Network.get(i);
				//					if(dst.equals(Network.get(value))) {// éviter d'ajouter soi-meme
				//						continue;
				//					}
				//					PositionProtocolImpl pp = (PositionProtocolImpl) dst.getProtocol(position_pid);
				//					if((pp.getCurrentPosition().distance(pos)) <= scope) {
				//						neighbors.add(dst.getID());
				//						ackHeard.add(dst.getID());
				//					}
				//				}
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
			return 2;
		}else if (state == Etat.KNOWN) {
			return 1;
		}else { //être le leader
			return 0; //vert
		}
	}

	public void newNeighborDetected(Node host, long id_new_neighbor, long idLeader_of_new_neighbor) {
		//
		if(host.getID() == 9){
			System.out.println("Ajout du voisin "+id_new_neighbor+" dans la liste des voisins de "+host.getID()+" leaderId "+leaderId+" new voisin's leader "+idLeader_of_new_neighbor);
		}
		if(idLeader_of_new_neighbor != leaderId) {// node not in my sphere before
			if(!inElection) {
				if(host.getID() == 9){
					System.out.println(host.getID()+" : NEW LEADER voisin : "+id_new_neighbor+" envoie de leaderId "+leaderId+" et leaderValue "+leaderValue);
				}
				LeaderMessage lmsg = new LeaderMessage(host.getID(), id_new_neighbor, my_pid, leaderId,leaderValue);
				emitter.emit(host, lmsg);
			}else {//bug ???? 
				if(host.getID() == 9){
					System.out.println(host.getID()+" : NEW voisin => Election src : "+id_new_neighbor+" idLeaderOfNewNeighbor "+idLeader_of_new_neighbor+" oldLostLeader "+oldLostLeaderId );
				}	
				//				if(idLeader_of_new_neighbor != oldLostLeaderId) {
				// inElection et leader -1 donc 
				ElectionMessage emsg = new ElectionMessage(host.getID(),id_new_neighbor,my_pid, host.getID(), computation_index);
				emitter.emit(host, emsg);
				//				}
			}


		}

		//lancer un Election message sur lui
	}

	/* appelé lorsque le noeud host détecte la perte d'un voisin */
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {
		//System.out.println("Suppresion du voisin "+id_lost_neighbor+" dans la liste des voisins de "+host.getID());
		if(inElection) {
			if(parent != id_lost_neighbor) { //cas où il a perdu un voisin non parent
				//check if ackheard.empty
				if(ackHeard.isEmpty()){
					//check if je suis le démareur déléction
					if(parent == -1) {//parent of election receive all ack
						leaderId = leaderIdInformation; // vu que head a recu tt ack donc il connait le leaderId 
						inElection = false;
						if(host.getID() == 9){
							System.out.println(host.getID()+" ackMessage empty "+leaderId);
						}
						if(neighbors.size() == 0) {
							inElection=false;
							state = Etat.LEADER;
							leaderId = value;
							leaderValue = value;
							leaderIdInformation = value;
						}else {
							for(long neighbor : this.getNeighbors()) {
								Node dest = Network.get((int) neighbor);
								if(dest.getID() == parent) { //this neighbor is parent dont propagete ElectionMessage
									continue; 
								}
								if(leaderValue == value) {
									state = Etat.LEADER;
									emitter.emit(host,new BeaconMessage(host.getID(),Emitter.ALL,my_pid, 1));
								}else {
									if(host.getID() == 9){
										System.out.println(host.getID()+" éléction valid leader known");
									}
									state = Etat.KNOWN;
								}
								if(host.getID() == 9){
									System.out.println(host.getID()+" envoie leaderMessage à " + dest.getID()+" avec leaderId "+leaderId);
								}
								LeaderMessage msg = new LeaderMessage(host.getID(), dest.getID(), my_pid, leaderId,leaderValue);

								arrived_beacon_counter = 0;
								emitter.emit(host, msg);
							}
						}
					}else {
						Node dest = Network.get((int)parent);
						if(host.getID() == 9){
							System.out.println(host.getID()+" lost neighbor ack " +dest.getID());
						}
						AckMessage msg = new AckMessage(host.getID(), dest.getID(), my_pid, leaderIdInformation,leaderValue, computation_index);
						EDSimulator.add(latency,msg, dest, my_pid);
						ackParentDone = true;
						//send msg ack to parent;
					}
				}
			}else {// si parent perdu pd election
				if(host.getID() == 9){
					System.out.println(host.getID()+" perdu parent ");
				}
				parent = -1;
				inElection = false;
				state = Etat.NOTKNOWN;
//				ackParentDone = true;
				ElectionMessage emsg = new ElectionMessage(host.getID(),host.getID(),my_pid, host.getID(), computation_index);
				emitter.emit(host, emsg);
			}
		}
		else {
			if(parent == id_lost_neighbor) {
				//				if(host.getID() == 12 || host.getID()== 8 || host.getID()== 35 || host.getID()== 15 || host.getID()== 14 || host.getID()==45 ) {
				//					System.out.println(host.getID()+" lost neighbor new éléction " +id_lost_neighbor);
				//				}
				//				if(parent == leaderId) {//le parent perdu est le leader
				//					leaderId = -1;
				//					oldLostLeaderId = parent;
				//					leaderIdInformation = value;//reset leaderIfInformation to self
				//					leaderValue = value;
				//				}
				//				parent = -1;
				//				state = Etat.NOTKNOWN;
				//				ElectionMessage emsg = new ElectionMessage(host.getID(),host.getID(),my_pid, host.getID(), computation_index);
				//				emitter.emit(host, emsg);	
				//nothing
			}
			//			if(neighbors.size() == 0) {
			//				inElection=false;
			//				state = Etat.LEADER;
			//				leaderId = value;
			//				leaderValue = value;
			//				leaderIdInformation = value;
			//			}
		}
		//lorsqu'on perd un parent quand l'éléction fini,on s'en fiche
		//car le beaconexpiredmessage va s'en occuper
		//si la perte de parent => perte leader => beaconexpired
		//sinon rien


	}

	@Override
	public VKT04 clone() {
		VKT04 vkt = null;
		try {
			vkt = (VKT04) super.clone();
			vkt.neighbors = new ArrayList<Long>();
			vkt.leaderId = -1;
			vkt.parent = -1;
			vkt.inElection = false;
			vkt.leaderValue = -1;
			vkt.state = Etat.NOTKNOWN;
			vkt.ackHeard = new HashSet<>();
			vkt.timeout_map = new HashMap<>();
			vkt.computation_num = 0;
			vkt.computation_id = -1L;
			vkt.computation_index = new Pair<Integer, Long>(0, -1L);
			vkt.beacon_loss_count = 0;
			vkt.arrived_beacon_counter = 0;
			vkt.oldLostLeaderId = -1;
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

	@Override
	public List<String> infos(Node host) {
		List<String> res = new ArrayList<String>();
		res.add(""+host.getID());
		//		res.add("V " + neighbors);
//		res.add("L " + leaderId);
						res.add("i " + computation_index.getNum()+":"+computation_index.getId());
						res.add("n : "+computation_num);
						res.add("id : "+computation_id);
//		res.add("P : "+parent);
		//		res.add("        ackH :"+ackHeard);
		//		res.add(""+host.getID());
		//		res.add("Voisins " + neighbors);
		//		res.add("Leader " + leaderId);
		//		res.add("index " + computation_index.getNum()+":"+computation_index.getId());
		//		res.add("num : "+computation_num);
		//		res.add("id : "+computation_id);
		//		res.add("parent : "+parent);
		//		res.add("beacon counter : "+ arrived_beacon_counter);
		return res;
	}


}
