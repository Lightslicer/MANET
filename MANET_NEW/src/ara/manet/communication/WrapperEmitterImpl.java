package src.ara.manet.communication;

import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import src.ara.manet.detection.ProbeMessage;
import src.ara.manet.detection.RemoveMessage;
import src.ara.manet.detection.ReplyMessage;
import src.ara.manet.positioning.Position;
import src.ara.manet.positioning.PositionProtocolImpl;
import src.ara.util.Message;

public class WrapperEmitterImpl implements WrapperEmitter{
	

	
	private int nbMsg;
	private Emitter emitter;	
	public WrapperEmitterImpl(Emitter emitter) {
		this.emitter = emitter;
	}

	// lorsque reception d'un message, node destinataire = moi
	@Override
	// appele par le simulateur lorqu'un evenement lui est adresse 
	public void processEvent(Node node, int pid, Object event) {
		emitter.processEvent(node, pid, event);
	}

	// envoie du message vers la file d'evenement que si les noeuds sont dans le scope du noeud 
	// emetteur 
	@Override
	public void emit(Node node, Message msg) {
		if(msg instanceof ProbeMessage || msg instanceof ReplyMessage || msg instanceof RemoveMessage) {
			
		}else {
			Position prc = ((PositionProtocolImpl) node.getProtocol(emitter.getPositionProtocolPid())).getCurrentPosition();
			// si le noeud envoie un message à soi même 
			if (msg.getIdDest() == node.getID()) {
				nbMsg++;
				// sinon envoie du message à tout le monde donc calcul si voisin ou non 
			} else if (msg.getIdDest() == Emitter.ALL) { // si c'est un broadcast pour tout le monde
				for (int i = 0; i < Network.size(); i++) {
					Node voisin = Network.get(i);
					Position drc = ((PositionProtocolImpl) voisin.getProtocol(emitter.getPositionProtocolPid())).getCurrentPosition();
					// envoie de message ssi les deux noeuds sont dans le scope
					if (prc.distance(drc) <= emitter.getScope()) {
						// envoie au processEvent de pidMess
						nbMsg++;
					}
				}
			} else {
				for (int i = 0; i < Network.size(); i++) {
					Node voisin = Network.get(i);
					if (msg.getIdDest() == voisin.getID()) {
						Position drc = ((PositionProtocolImpl) voisin.getProtocol(emitter.getPositionProtocolPid())).getCurrentPosition();
						if (prc.distance(drc) <= emitter.getScope()) {
							nbMsg++;
							break;
						}
					}
				}
			}
		}
		emitter.emit(node, msg);
		
	}

	@Override
	public int getLatency() {
		return emitter.getLatency();
	}

	@Override
	public int getScope() {
		return emitter.getScope();
	}

	public Object clone() {
		WrapperEmitterImpl res = null;
		try {
			res = (WrapperEmitterImpl) super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return res;
	}

	@Override
	public int getPositionProtocolPid() {
		// TODO Auto-generated method stub
		return emitter.getPositionProtocolPid();
	}


}
