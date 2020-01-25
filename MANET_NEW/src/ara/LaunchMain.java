package ara;

import peersim.Simulator;


public class LaunchMain {
	private static final String dir = "/home/aurora/git/MANET/MANET_NEW";
	private static final String dir = "/users/Etu8/3520328/git/MANET/MANET_NEW";
	public static void main(String[] args) {
		String[] path = { dir+"/src/ara/configVTK"};
		String[] path = { dir+"/src/ara/configGlobalView"};
		Simulator.main(path);
		
	}
}
