package ara.manet;

import java.util.Random;

import ara.manet.algorithm.election.ElectionMessage;
import ara.manet.algorithm.election.VKT04Statique;
import ara.manet.communication.EmitterImpl;
import ara.manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class InitialisationVTKStatique implements Control{
	
	public  InitialisationVTKStatique(String prefix) {}
	
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
			VKT04Statique vktp = (VKT04Statique) src.getProtocol(vkt_pid);
			vktp.processEvent(src, vkt_pid, "INITEVENT");
			
		}
		Node src = Network.get(0);
		VKT04Statique vktp = (VKT04Statique) src.getProtocol(vkt_pid);
		ElectionMessage msg = new ElectionMessage(src.getID(),src.getID(),vkt_pid,src.getID());
		vktp.processEvent(src, vkt_pid, msg);
		return false;
	}
	
}
