package ara.manet;

import ara.manet.algorithm.election.GlobalViewLeader;
import ara.manet.algorithm.election.VKT04Statique;
import ara.manet.communication.EmitterImpl;
import ara.manet.detection.HeartBeatMessage;
import ara.manet.detection.NeighborProtocolImpl;
import ara.manet.detection.NeighborhoodListenerImpl;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitialisationGlobalViewLeader implements Control{

public  InitialisationGlobalViewLeader(String prefix) {}
	
	@Override
	public boolean execute() {
				
		
		int position_pid=Configuration.lookupPid("position");
		//int neighbor_pid=Configuration.lookupPid("neighbor");
		//int emitter_pid=Configuration.lookupPid("emit");
		int gvl_pid=Configuration.lookupPid("gvl");
		
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.initialiseCurrentPosition(src);
			//Ca a l'air de marche
			
		} 
		NeighborhoodListenerImpl listener = new NeighborhoodListenerImpl();
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);			
			//NeighborProtocolImpl np = (NeighborProtocolImpl) src.getProtocol(neighbor_pid);
			//np.heartbeat(src);
			//np.processEvent(src, neighbor_pid, new HeartBeatMessage(src.getID(),src.getID(),neighbor_pid));
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.processEvent(src, position_pid, "LOOPEVENT");
			GlobalViewLeader gvl = (GlobalViewLeader) src.getProtocol(gvl_pid);
			gvl.processEvent(src, gvl_pid, "INITEVENT");
			
		}
		Node src = Network.get(0);
		//EmitterImpl ep = (EmitterImpl) src.getProtocol((emitter_pid));
		//ep.attach(listener);
		return false;
	}
}
