package ara;

import peersim.Simulator;


public class LaunchMain {
	private static final String dir = "/home/aurora/git/MANET/";
	public static void main(String[] args) {
		
		String[] path = { dir+"MANET/src/ara/configVTKStatique"};
		Simulator.main(path);
		
	}
}
