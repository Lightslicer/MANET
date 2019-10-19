package ara.manet;

import peersim.config.Configuration;
import peersim.core.Control;

public class HeartBeatController implements Control {

	
private static final String PAR_PROTO_APPLICATIF="application";
	
	private final int pid_application;
	
	public HeartBeatController(String prefix) {
		pid_application=Configuration.getPid(prefix+"."+PAR_PROTO_APPLICATIF);
	}
	
	
	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		return false;
	}

}
