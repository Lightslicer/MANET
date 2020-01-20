package ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ara.manet.Monitorable;
import ara.manet.algorithm.election.GlobalViewLeader.Peer;
import ara.manet.communication.Emitter;
import ara.manet.detection.NeighborProtocol;
import ara.manet.detection.NeighborhoodListener;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class GlobalViewLeader{
	
	public class Peer {
		int id;
		int value;
		public Peer(int id, int value) {
			this.id = id;
			this.value = value;
		}
	}
	public class View{
		int clock;
		List<Peer> neighbors;
		public View(int clock, List<Peer> neighbors) {
			this.clock = clock;
			this.neighbors = neighbors; // instancier direct ?
		}
	}
	
	private static final String PAR_SCOPE = "scope";
	private static final String PAR_LATENCY = "latency";
	public static final String init_value_event = "INITEVENT";
	
	private static final String PAR_PROBE = "probe";
	private static final String PAR_TIMER = "timer";
	
	private int pid;
	private int value;
	private int clock;
	private List<Peer> neighbors;
	private Peer peer;
	private View[] knowledge;
	
	private int probe;
	private int timer;
	private int leader;
	
	
	public GlobalViewLeader(String prefix) {
		View view = new View(clock, neighbors);
		knowledge = new View[Network.size()];
		String tmp[] = prefix.split("\\.");
		pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.probe = Configuration.getInt(prefix + "." + PAR_PROBE);
		this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
		neighbors = new ArrayList<>();
		value = pid;
		peer = new Peer(pid, value);
		
		//initialisation
		neighbors.add(peer);
		clock = 0;
	}
	

	
	public void processEvent(Node node, int pid, Object event) {
		
		//upon connected peer j
		if(event instanceof connectedMessage) {
			connectedMessage m = (connectedMessage) event;
			Peer tmp = new Peer(m.getPid(), m.getPid());
			neighbors.add(tmp);
			clock ++;
			knowledge[pid].clock = clock;
			knowledge[pid].neighbors = neighbors;
			//broadcast knowledge
		}
		
		//upon disconnected peer j
		if(event instanceof disconnectedMessage) {
			disconnectedMessage m = (disconnectedMessage) event;
			editMessage edit = new editMessage(pid, null, m.getIdSrc(), clock, clock+1); //changer disconnected
			for(Peer p : neighbors) {
				if(p.id == m.getPid()) {
					neighbors.remove(p);
					break;
				}
			}
			clock ++;
			knowledge[pid].clock = clock;
			//broadcast edit
		}
		
		//upon reception of knowledge from peer j
		if(event instanceof knowledgeMessage) {
			knowledgeMessage m = (knowledgeMessage) event;
			editMessage edit;
			for(Peer p : m.knowledge[m.getPid()].neighbors) {
				if(knowledge[p.id] == null) {
					edit = new editMessage(p.id, m.knowledge[p.id].neighbors, null, 0, m.knowledge[p.id].clock);
					knowledge[p.id].neighbors = m.knowledge[p.id].neighbors;
					knowledge[p.id].clock = m.knowledge[p.id].clock;
				}else{
					if(m.knowledge[p.id].clock > knowledge[p.id].clock) {
						List<Peer> added = new ArrayList<>(m.knowledge[p.id].neighbors);
						List<Peer> removed = new ArrayList<>(knowledge[p.id].neighbors);
						added.remove(knowledge[p.id].neighbors);
						removed.remove(m.knowledge[p.id].neighbors);
						edit = new editMessage(p.id, added, removed, knowledge[p.id].clock, m.knowledge[p.id].clock);
						knowledge[p.id].neighbors = m.knowledge[p.id].neighbors;
						knowledge[p.id].clock = m.knowledge[p.id].clock;
					}
					
				}
			}
			if(edit != null) {
				//broadcast edit
			}
		}
		
		if(event instanceof editMessage) {
			editMessage m = (editMessage) event;
			int update = 0;
			if(m.added != null) {
				if(knowledge[(int) m.source] == null) {
					if(m.old_clock == 0) {
						update = 1;
						for(int i=0; i< m.added.size(); i++) {
							knowledge[(int) m.source].neighbors.add(m.added.get(i));
						}
					}
				}else {
					if(m.old_clock == knowledge[(int) m.source].clock) {
						update = 1;
						for(int i=0; i< m.added.size(); i++) {
							if(knowledge[(int) m.source].neighbors.contains(m.added.get(i))) {
								knowledge[(int) m.source].neighbors.add(m.added.get(i));
							}
						}
					}
				}
			}
			if(m.removed != null) {
				if(knowledge[(int) m.source] != null) {
					if(m.old_clock == knowledge[(int) m.source].clock) {
						update = 1;
						for(int i=0; i< m.added.size(); i++) {
							knowledge[(int) m.source].neighbors.remove(m.removed.get(i));
						}
					}
				}
			}
			if(knowledge[(int) m.source] != null) {
				if(update>0) {
					knowledge[(int) m.source].clock=m.new_clock;
				}
			}
			if(update>0) {
				//broadcast edit
			}
		}
	}
	
	public void leader(Peer peer) {
		List<Peer> voisins = new ArrayList<Peer>(knowledge[peer.id].neighbors);
		for(Peer p : voisins) {
			
		}
	}


	
	public List<Peer> getNeighbors() {
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
				int dstID = (int) dst.getID();
				int dstVal = (int) dst.getID();
				Peer dstPeer = new Peer(dstID, dstVal);
				neighbors.add(dstPeer);
			}
		}
		return neighbors;
	}




	
	public long getIDLeader() {
		return pid;
	}







	
	public int getValue() {
		return value;
	}

}
