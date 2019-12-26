package ara.manet;

import java.util.Random;

import ara.manet.algorithm.election.ElectionMessage;
import ara.manet.algorithm.election.Pair;
import ara.manet.algorithm.election.VKT04;
import ara.manet.communication.Emitter;
import ara.manet.detection.ProbeMessage;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitialisationVTK implements Control{
	
	public  InitialisationVTK(String prefix) {}
	
	@Override
	public boolean execute() {
				
		Random r = new Random();
		int position_pid=Configuration.lookupPid("position");
		int vkt_pid=Configuration.lookupPid("vkt");
		
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.initialiseCurrentPosition(src);
			//Ca a l'air de marche
			
		} 
		//NeighborhoodListenerImpl listener = new NeighborhoodListenerImpl();
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);			
			VKT04 vktp = (VKT04) src.getProtocol(vkt_pid);
			vktp.processEvent(src, vkt_pid, "INITEVENT");
			
		}
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);			
			VKT04 vktp = (VKT04) src.getProtocol(vkt_pid);
			ElectionMessage msg = new ElectionMessage(src.getID(),src.getID(),vkt_pid,src.getID(), new Pair<Integer, Long>(0, src.getID()));
			vktp.processEvent(src, vkt_pid, msg);	
		}
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);			
			VKT04 vktp = (VKT04) src.getProtocol(vkt_pid);
			vktp.processEvent(src, vkt_pid, new ProbeMessage(src.getID(),Emitter.ALL,vkt_pid));
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.processEvent(src, position_pid, "LOOPEVENT");
		}
			
		return false;
	}
	
}
