package src.ara.manet.detection;

import peersim.core.Node;

public class NeighborhoodListenerImpl implements NeighborhoodListener {

	public NeighborhoodListenerImpl() {
		// TODO Auto-generated constructor stub
	}
	
	public void newNeighborDetected(Node host, long id_new_neighbor) {
		System.out.println("Ajout du voisin "+id_new_neighbor+"dans la liste des voisins de "+host.getID());
	}

	/* appelé lorsque le noeud host détecte la perte d'un voisin */
	public void lostNeighborDetected(Node host, long id_lost_neighbor) {
		System.out.println("Suppresion du voisin "+id_lost_neighbor+"dans la liste des voisins de "+host.getID());
	}
	
	@Override
	public NeighborhoodListenerImpl clone() {
		NeighborhoodListenerImpl em = null;
		try {
			em = (NeighborhoodListenerImpl) super.clone();
			
		}
		catch( CloneNotSupportedException e ) {} // never happens
		return em;
	}
}
