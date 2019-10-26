package ara.manet;


import ara.manet.communication.EmitterImpl;
import ara.manet.detection.NeighborProtocolImpl;
import ara.manet.detection.NeighborhoodListenerImpl;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class Initialisation implements Control{
	
	public  Initialisation(String prefix) {}
	
	@Override
	public boolean execute() {
				
		
		int position_pid=Configuration.lookupPid("position");
		int neighbor_pid=Configuration.lookupPid("neighbor");
		int emitter_pid=Configuration.lookupPid("emit");
		
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.initialiseCurrentPosition(src);
			//Ca a l'air de marche
			
		} 
		NeighborhoodListenerImpl listener = new NeighborhoodListenerImpl();
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);			
			NeighborProtocolImpl np = (NeighborProtocolImpl) src.getProtocol(neighbor_pid);
			np.heartbeat(src);
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.processEvent(src, position_pid, "LOOPEVENT");
			
		}
		Node src = Network.get(0);
		EmitterImpl ep = (EmitterImpl) src.getProtocol((emitter_pid));
		ep.attach(listener);
		return false;
	}
	
}
