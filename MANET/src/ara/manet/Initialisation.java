package ara.manet;


import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocolImpl;
import ara.manet.positioning.PositioningConfiguration;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class Initialisation implements Control{
	
	public  Initialisation(String prefix) {}
	
	@Override
	public boolean execute() {
				
		
		int position_pid=Configuration.lookupPid("position");
		for(int i = 0;i<Network.size();i++) {
			Node src = Network.get(i);
			PositionProtocolImpl pp = (PositionProtocolImpl) src.getProtocol(position_pid);
			pp.initialiseCurrentPosition(src);
			pp.processEvent(src, position_pid, "LOOPEVENT");
			//Ca a l'air de marche
			
		} 
		
		return false;
	}
	
}
