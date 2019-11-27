package ara;

import peersim.Simulator;


public class LaunchMain {
	private static final String dir = "/home/aurora/git/MANET/MANET_NEW";
	public static void main(String[] args) {
		
		String[] path = { dir+"/src/ara/configVTKStatique"};
		Simulator.main(path);
		
	}
}
