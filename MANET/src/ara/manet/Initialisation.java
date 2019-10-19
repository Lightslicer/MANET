package ara.manet;


import ara.manet.detection.NeighborProtocolImpl;
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
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.initialiseCurrentPosition(src);
			pp.processEvent(src, position_pid, "LOOPEVENT");
			//Ca a l'air de marche
			
		} 
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);			
			NeighborProtocolImpl np = (NeighborProtocolImpl) src.getProtocol(neighbor_pid);
			np.heartbeat(src);
		}
		
		return false;
	}
	
}
