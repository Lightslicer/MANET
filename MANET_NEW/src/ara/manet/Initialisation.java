package src.ara.manet;


import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import src.ara.manet.detection.HeartBeatMessage;
import src.ara.manet.detection.NeighborProtocolImpl;
import src.ara.manet.detection.NeighborhoodListenerImpl;
import src.ara.manet.positioning.PositionProtocolImpl;

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
		} 
		NeighborhoodListenerImpl listener = new NeighborhoodListenerImpl();
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);			
			NeighborProtocolImpl np = (NeighborProtocolImpl) src.getProtocol(neighbor_pid);
//			heartbeat
			np.processEvent(src, neighbor_pid, new HeartBeatMessage(src.getID(),src.getID(),neighbor_pid));
			np.attach(listener);
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.processEvent(src, position_pid, "LOOPEVENT");
			
		}
		return false;
	}
	
}
