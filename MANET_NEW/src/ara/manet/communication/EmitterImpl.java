package ara.manet.communication;

import java.util.List;

import ara.manet.detection.NeighborhoodListener;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocolImpl;
import ara.util.Message;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class EmitterImpl implements Emitter {

	private static final String LATENCY = "latency";
	private static final String SCOPE = "scope";
	private static final String PAR_POSITIONPID = "positionProtocol";
	
	private final int mypid;
	private final int latency;
	private final int scope;
	private final int position_pid;

	
	public EmitterImpl(String prefix) {
		String tmp[] = prefix.split("\\.");
		this.mypid = Configuration.lookupPid(tmp[tmp.length - 1]);
		this.latency = Configuration.getInt(prefix + "." + LATENCY);
		this.scope = Configuration.getInt(prefix + "." + SCOPE);
		this.position_pid = Configuration.getPid(prefix + "." + PAR_POSITIONPID);
		
	}

	// lorsque reception d'un message, node destinataire = moi
	@Override
	// appele par le simulateur lorqu'un evenement lui est adresse 
	public void processEvent(Node node, int pid, Object event) {
		if (pid != mypid)
			throw new RuntimeException("Receive Event for wrong protocol");
		
		if (event instanceof Message) {
			Message msg = (Message) event;
			int pidMess = msg.getPid();
			// Si je suis le destinataire, 
			if (msg.getIdDest() == node.getID() || msg.getIdDest() == Emitter.ALL) {
				((EDProtocol) node.getProtocol(pidMess)).processEvent(node, pidMess, msg);
			} else {
				System.out.println("Receive the wrong message, I am not the receiver");
			}
		}
	}

	// envoie du message vers la file d'evenement que si les noeuds sont dans le scope du noeud 
	// emetteur 
	@Override
	public void emit(Node node, Message msg) {
		Position prc = ((PositionProtocolImpl) node.getProtocol(position_pid)).getCurrentPosition();
		// si le noeud envoie un message à soi même 
		if (msg.getIdDest() == node.getID()) {
			EDSimulator.add(latency, msg, node, mypid);
		// sinon envoie du message à tout le monde donc calcul si voisin ou non 
		} else if (msg.getIdDest() == Emitter.ALL) { // si c'est un broadcast pour tout le monde
			for (int i = 0; i < Network.size(); i++) {
				Node voisin = Network.get(i);
				Position drc = ((PositionProtocolImpl) voisin.getProtocol(position_pid)).getCurrentPosition();
				// envoie de message ssi les deux noeuds sont dans le scope
				if (prc.distance(drc) <= scope) {
					// envoie au processEvent de pidMess
					EDSimulator.add(latency, msg, voisin, mypid);
				}
			}
		} else {
			for (int i = 0; i < Network.size(); i++) {
				Node voisin = Network.get(i);
				if (msg.getIdDest() == voisin.getID()) {
					Position drc = ((PositionProtocolImpl) voisin.getProtocol(position_pid)).getCurrentPosition();
					if (prc.distance(drc) <= scope) {
						EDSimulator.add(latency, msg, voisin, mypid);
						break;
					}
				}
			}
		}
	}

	@Override
	public int getLatency() {
		return latency;
	}

	@Override
	public int getScope() {
		return scope;
	}

	public Object clone() {
		EmitterImpl res = null;
		try {
			res = (EmitterImpl) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return res;
	}

	
}
