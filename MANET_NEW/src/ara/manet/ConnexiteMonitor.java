package ara.manet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import ara.manet.communication.Emitter;
import ara.manet.positioning.Position;
import ara.manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class ConnexiteMonitor implements Control {

	private static final long serialVersionUID = -4639751772079773440L;

	private static final String PAR_POSITIONPID = "positionprotocol";
	private static final String PAR_NEIGHBORPID = "neighborprotocol";
	private static final String PAR_EMITTER = "emitter";
	private static final String PAR_MONITORABLEPID = "monitorableprotocol";
	private static final String PAR_TIMER= "timer";

	private double timer;
	//	private final int election_pid;
	private final int position_pid;
	private final int neighbor_pid;
	private final int emitter_pid;
	private final int monitorable_pid;
	private IncrementalStats stats;

	private int connexity;
	private double average_connexity;
	private double variance_connexity;

	private BufferedWriter writer1;
	private BufferedWriter writer2;
	
	private long END;

	private static final Monitorable defaultmonitorable = new Monitorable() {
		@Override
		public Object clone() {
			Monitorable res = null;
			try {
				res = (Monitorable) super.clone();
			} catch (CloneNotSupportedException e) {
			}
			return res;
		}
	};

	public ConnexiteMonitor(String prefix) {
		// TODO Auto-generated constructor stub
		neighbor_pid= Configuration.getPid(prefix+"."+PAR_NEIGHBORPID,-1);
		position_pid=Configuration.getPid(prefix+"."+PAR_POSITIONPID);
		emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER,-1);
		monitorable_pid=Configuration.getPid(prefix+"."+PAR_MONITORABLEPID,-1);
		timer=Configuration.getDouble(prefix+"."+PAR_TIMER);
		stats = new IncrementalStats();
		END = CommonState.getEndTime();
		try {
			writer1 = new BufferedWriter(new FileWriter("average_connexity_vkt.txt", true));
			writer2 = new BufferedWriter(new FileWriter("variance_connexity_vkt.txt", true));

		}catch(IOException e) {

		}
	}

	private void Connexity(){


		Map<Long, Position> positions = PositionProtocol.getPositions(position_pid);

		//System.err.println(positions);
		// Tout le monde possede le meme scope
		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		// recuperation de toutes les composantes connexes.
		Map<Integer, Set<Node>> connected_components = PositionProtocol.getConnectedComponents(positions, em.getScope());
		connexity = connected_components.size();
		stats.add(connexity);
		average_connexity = stats.getAverage();
		variance_connexity = stats.getVar();

	}


	@Override
	public boolean execute() {

		Emitter em = (Emitter) Network.get(0).getProtocol(emitter_pid);
		Connexity();
		if(CommonState.getTime() == END-1000) {
			try {
				writer1.append(em.getScope()+" "+String.valueOf(average_connexity)+"\n");
				writer2.append(em.getScope()+" "+String.valueOf(variance_connexity)+"\n");
							writer1.flush();
							writer2.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return false;

	}

}
