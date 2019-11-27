package ara.manet.detection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ara.manet.communication.Emitter;
import ara.manet.communication.EmitterImpl;
import ara.util.Message;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class NeighborProtocolImpl implements NeighborProtocol, EDProtocol {
	private static final String PERIODE = "probe";
	private static final String TIMEOUT_INIT = "timer";
	private static final String PAR_EMITTER = "emitter";
	private static final String NLISTENER = "listener";

	private final int my_pid;
	private final int emitter_pid;
	private final int listener;
	
	private final int periodeToSendProbe; // periode d'emission des messages de type Probe a ses voisins
	private int timeoutToSendProbe;
	
	private final int timeoutInit; // timeout initial
	private List<Long> timeout; // tableau de timeout des voisins
	
	private List<Long> idNeighbor; // identifiant des voisins
	private int nbNeighbor; // nb de voisins

	private Map<Long, Long> map; //map pour se souvenir de ses voisins
	private List<NeighborhoodListener> list; //liste contenant les listener
	
	public NeighborProtocolImpl(String prefix) {
		String tmp[] = prefix.split("\\.");
		this.my_pid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.emitter_pid = Configuration.getPid(prefix + "." + PAR_EMITTER);
		this.listener = Configuration.getInt(prefix + "." + NLISTENER, -1);
		this.periodeToSendProbe = Configuration.getInt(prefix + "." + PERIODE);
		this.timeoutToSendProbe = periodeToSendProbe;
		this.timeoutInit = Configuration.getInt(prefix + "." + TIMEOUT_INIT);
		this.timeout = new ArrayList<Long>();
		this.idNeighbor = new ArrayList<Long>();
		this.nbNeighbor = 0;
		map = new HashMap<>();
		list = new ArrayList<>();
	}

	@Override
	public List<Long> getNeighbors() {
		return idNeighbor;
	}

	public Object clone() {
		NeighborProtocolImpl res = null;
		try {
			res = (NeighborProtocolImpl) super.clone();
			res.idNeighbor = new ArrayList<Long>(idNeighbor);
			res.timeout = new ArrayList<Long>(timeout);
			res.timeoutToSendProbe = periodeToSendProbe;
			res.map = new HashMap<>();
			res.list = new ArrayList<>();
		} catch (CloneNotSupportedException e) {
		}
		return res;
	}

	// Ajout de message de type "broadcast" dans la fil d'execution de emitter
	public void emit(Node n, int pid, Message event) {
		EmitterImpl e = ((EmitterImpl) n.getProtocol(emitter_pid));
		e.emit(n, event);
	}

	// A la reception d'un message
	@Override
	public void processEvent(Node node, int pid, Object event) {
		if (pid != my_pid)
			throw new RuntimeException("Receive Event for wrong protocol");

		// si reception d'un probeMessage, on suppose qu'il est encore a cote de moi
		// alors il est toujours dans ma liste des voisins
		
		if (event instanceof ProbeMessage) {
			ProbeMessage pbmsg = (ProbeMessage) event;
			int idsrc = (int) pbmsg.getIdSrc();
			//Si je recois mon ProbeMessage relancer heartbeat dans probe tps
			if (idsrc == node.getID()) {
				//emit(node, my_pid,new HeartBeatMessage(node.getID(), node.getID(), my_pid));
				//processEvent(node,my_pid,new HeartBeatMessage(idsrc,idsrc,my_pid));
				EDSimulator.add(periodeToSendProbe, new HeartBeatMessage(idsrc,idsrc,my_pid), node, my_pid);
			}else {
				if (!idNeighbor.contains((long) idsrc)) {
					idNeighbor.add(pbmsg.getIdSrc());					
					nbNeighbor++;					
				}
				EDSimulator.add(150, new RemoveMessage(node.getID(),node.getID(),my_pid, idsrc), node, my_pid);
				map.put(pbmsg.getIdSrc(), CommonState.getTime()+timeoutInit);
			}
		} else if (event instanceof HeartBeatMessage) {
			// pour detecter si il y a des noeuds a cote de moi
			HeartBeatMessage msg = (HeartBeatMessage) event;
			emit(node,my_pid,new ProbeMessage(node.getID(),Emitter.ALL,my_pid));
		} else if (event instanceof RemoveMessage) {//remove ne marche pas
			RemoveMessage m = (RemoveMessage) event;
			if(map.containsKey(m.getTargetId())){
				if(map.get(m.getTargetId()) <= CommonState.getTime()) {
					//le voisin devrait être supprimé
					idNeighbor.remove(m.getTargetId());
					notifyRemoveListener(node, m.getTargetId());
				}
			}
		}
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
