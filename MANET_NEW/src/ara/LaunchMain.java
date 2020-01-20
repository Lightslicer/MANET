package ara;

import peersim.Simulator;


public class LaunchMain {
	//private static final String dir = "/home/aurora/git/MANET/MANET_NEW";
	private static final String dir = "\\Users\\Seathkiller\\git\\MANET\\MANET_NEW";
	public static void main(String[] args) {
		
		//String[] path = { dir+"/src/ara/config"};
		String[] path = { dir+"\\src\\ara\\configGlobalView"};
		Simulator.main(path);
		
	}
}
