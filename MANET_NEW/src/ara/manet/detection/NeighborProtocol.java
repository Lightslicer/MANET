package ara.manet.detection;

import java.util.List;

import peersim.core.Node;
import peersim.core.Protocol;

/**
 * @author jonathan.lejeune@lip6.fr
 *
 */
public interface NeighborProtocol extends Protocol {

	/* Renvoie la liste courante des Id des voisins directs */
	public List<Long> getNeighbors();
	
	public void attach(NeighborhoodListener nl);
	
	public void detach(NeighborhoodListener nl);
	
	public void notifyAddListener(Node node, Long newId);
	
	public void notifyRemoveListener(Node node, Long newId);

}
