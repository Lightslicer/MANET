package src.ara.manet.communication;

import peersim.core.Node;
import peersim.edsim.EDProtocol;
import src.ara.util.Message;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */

public interface Emitter extends EDProtocol {

	/* désigne tous les voisins accessibles */
	public static final int ALL = -2;

	/* permet la simulation de l'envoie d'un message msg depuis host */
	public void emit(Node host, Message msg);

	/*
	 * Renvoie la latence entre le moment où un site envoi un message et le moment
	 * où un voisin direct le reçoit
	 */
	public int getLatency();

	/* Renvoie le rayon d'emission/réception d'un noeud */
	public int getScope();

	public int getPositionProtocolPid();
	
	
}
