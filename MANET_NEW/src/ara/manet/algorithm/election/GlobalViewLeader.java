package src.ara.manet.algorithm.election;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import src.ara.manet.Monitorable;
import src.ara.manet.algorithm.election.VKT04Statique.Etat;
import src.ara.manet.communication.Emitter;
import src.ara.manet.detection.NeighborProtocol;
import src.ara.manet.detection.NeighborhoodListener;
import src.ara.manet.detection.ProbeMessage;
import src.ara.manet.detection.RemoveMessageGlobalView;
import src.ara.manet.detection.ReplyMessageGlobalView;

public class GlobalViewLeader implements ElectionProtocol, Monitorable, NeighborProtocol{

        public class Peer {
                int id;
                int value;
                public Peer(int id, int value) {
                        this.id = id;
                        this.value = value;
                }
        }
        public class View{
                int clock;
                List<Peer> neighbors;
                public View(int clock, List<Peer> neighbors) {
                        this.clock = clock;
                        this.neighbors = neighbors;
                }
        }

        private static final String PAR_SCOPE = "scope";
        private static final String PAR_LATENCY = "latency";
        public static final String init_value_event = "INITEVENT";

        private static final String PAR_EMITTER = "emit";
        private static final String PAR_PROBE = "probe";
        private static final String PAR_TIMER = "timer";

        private int pid;
        private int value;
        private int clock;
        private View view;
        private List<Peer> neighbors;
        private Peer peer;
        private View[] knowledge;
        private int leader;
        private Map<Peer, Long> timeout_map; //map pour se souvenir de ses voisins et leur timeout
        private Etat state;

        private int probe;
        private int timer;
        private Emitter emitter;


        public GlobalViewLeader(String prefix) {

                String tmp[] = prefix.split("\\.");
                pid = Configuration.lookupPid(tmp[tmp.length - 1]);
                this.probe = Configuration.getInt(prefix + "." + PAR_PROBE);
                this.timer = Configuration.getInt(prefix + "." + PAR_TIMER);
                this.emitter = (Emitter) Configuration.getInstance("protocol." + PAR_EMITTER);

                neighbors = new ArrayList<>();
                view = new View(clock, neighbors);
                knowledge = new View[Network.size()];
                for(int i = 0; i<Network.size(); i++) {
                        knowledge[i] = null;
                }
                timeout_map = new HashMap<>();

                //initialisation
                neighbors.add(peer);
                clock = 0;
                leader = -1;
                state = state.NOTKNOWN;

        }



        public void processEvent(Node node, int pid, Object event) {
        	
                if(event instanceof ProbeMessage) {
                        ProbeMessage m = (ProbeMessage) event;
                        int idsrc = (int) m.getIdSrc();
                        if (idsrc == node.getID()) {//Si je recois mon ProbeMessage relancer heartbeat dans probe tps
                                emitter.emit(node, new ProbeMessage(idsrc,Emitter.ALL,pid));
                        }else {
                                emitter.emit(node, new ReplyMessageGlobalView(node.getID(),idsrc,pid, leader, peer));
                        }
                }

                if(event instanceof ReplyMessageGlobalView) {
                        ReplyMessageGlobalView m = (ReplyMessageGlobalView) event;
                        if(!neighbors.contains(m.peer)) {
                                neighbors.add(m.peer);
                        }
                        clock++;
                        EDSimulator.add(timer, new RemoveMessageGlobalView(node.getID(),node.getID(),pid, (int)m.getIdSrc(), m.peer), node, pid); // timer+49 to adjust visual effect
                        timeout_map.put(m.peer, CommonState.getTime()+timer);
                        emitter.emit(node, new knowledgeMessage(node.getID(), Emitter.ALL, pid, knowledge, clock));
                }


                if(event instanceof RemoveMessageGlobalView) {
                        RemoveMessageGlobalView m = (RemoveMessageGlobalView) event;

                        if(timeout_map.containsKey(m.peer)){
                                if(timeout_map.get(m.peer) <= CommonState.getTime()) {
                                        neighbors.remove(m.peer);
                                        timeout_map.remove(m.peer);
                                }
                        }
                        List<Peer> removed = new ArrayList<>();
                        removed.add(m.peer);
                        editMessage edit = new editMessage(node.getID(), Emitter.ALL, pid, null, removed, clock, clock+1);
                        clock++;
                        emitter.emit(node, edit);
                        leader = -1;
                        state = state.NOTKNOWN;
                        leader();
                        if(leader == (int) node.getID()) {
                        	state = state.LEADER;
                        }
                        if(leader != (int) node.getID()) {
                        	state = state.KNOWN;
                        }
                }



                //upon reception of knowledge from peer j
                
                if(event instanceof knowledgeMessage) {
                        knowledgeMessage m = (knowledgeMessage) event;

                        editMessage edit = null;
                        if(m.knowledge[(int) m.getIdSrc()] != null) {
                                for(Peer p : m.knowledge[(int)m.getIdSrc()].neighbors) {
                                        if(knowledge[(int) node.getID()] == null) {
                                                edit = new editMessage(p.id, Emitter.ALL, p.id, m.knowledge[(int)m.getIdSrc()].neighbors, null, 0, m.knowledge[(int)m.getIdSrc()].clock);
                                                knowledge[(int) node.getID()].neighbors = m.knowledge[(int)m.getIdSrc()].neighbors;
                                                knowledge[(int) node.getID()].clock = m.knowledge[(int)m.getIdSrc()].clock;
                                        }else{
                                                if(m.knowledge[(int)m.getIdSrc()].clock > knowledge[(int) node.getID()].clock) {
                                                        List<Peer> added = new ArrayList<>(m.knowledge[(int)m.getIdSrc()].neighbors);
                                                        List<Peer> removed = new ArrayList<>(knowledge[(int) node.getID()].neighbors);
                                                        for(int i=0; i<knowledge[(int) node.getID()].neighbors.size(); i++) {
                                                                added.remove(knowledge[(int) node.getID()].neighbors.get(i));
                                                                removed.remove(m.knowledge[(int)m.getIdSrc()].neighbors.get(i));
                                                        }
                                                        edit = new editMessage(p.id, Emitter.ALL, p.id, added, removed, knowledge[(int) node.getID()].clock, m.knowledge[(int)m.getIdSrc()].clock);
                                                        knowledge[(int) node.getID()].neighbors = m.knowledge[(int)m.getIdSrc()].neighbors;
                                                        knowledge[(int) node.getID()].clock = m.knowledge[(int)m.getIdSrc()].clock;
                                                }

                                        }
                                }
                        }
                        if(edit != null) {
                                emitter.emit(node, edit);
                        }
                }

                if(event instanceof editMessage) {
                        editMessage m = (editMessage) event;
                        int update = 0;
                        if(m.added != null) {
                                if(knowledge[(int) m.source] == null) {
                                        if(m.old_clock == 0) {
                                                update = 1;
                                                for(int i=0; i< m.added.size(); i++) {
                                                        knowledge[(int) m.source].neighbors.add(m.added.get(i));
                                                }
                                        }
                                }else {
                                        if(m.old_clock == knowledge[(int) m.source].clock) {
                                                update = 1;
                                                for(int i=0; i< m.added.size(); i++) {
                                                        if(knowledge[(int) m.source].neighbors.contains(m.added.get(i))) {
                                                                knowledge[(int) m.source].neighbors.add(m.added.get(i));
                                                        }
                                                }
                                        }
                                }
                        }
                        if(m.removed != null) {
                                if(knowledge[(int) m.source] != null) {
                                        if(m.old_clock == knowledge[(int) m.source].clock) {
                                                update = 1;
                                                for(int i=0; i< m.added.size(); i++) {
                                                        knowledge[(int) m.source].neighbors.remove(m.removed.get(i));
                                                }
                                        }
                                }
                        }
                        if(knowledge[(int) m.source] != null) {
                                if(update>0) {
                                        knowledge[(int) m.source].clock=m.new_clock;
                                }
                        }
                        if(update>0) {
                                emitter.emit(node, m);
                        }
                }
                if (event instanceof String) {
                        String ev = (String) event;
                        if (ev.equals(init_value_event)) {

                                value = (int) node.getID();
                                peer = new Peer(value, value);
                                knowledge[peer.id] = view;
                                return;
                        }
                }
        }

        public void leader() {
        		int max = value;
                for(Peer p : neighbors) {
                		if(p.value>max) {
                				max = p.value;
                		}
                }
                leader = max;
        }


        @Override
        public List<Long> getNeighbors() {
                List<Long> res = new ArrayList<>();
                for(int i=0; i<neighbors.size(); i++) {
                        res.add((long) neighbors.get(i).id);
                }
                return res;
        }





        public long getIDLeader() {
                return leader;
        }


        public int getState(Node host) {
    		if(state == Etat.NOTKNOWN) {
    			return 2;
    		}else if (state == Etat.KNOWN) {
    			return 1;
    		}else { //être le leader
    			return 0; //vert
    		}
    	}





        public int getValue() {
                return value;
        }



        @Override
        public GlobalViewLeader clone() {
                GlobalViewLeader gvl = null;
                try {
                        gvl = (GlobalViewLeader) super.clone();
                        gvl.neighbors = new ArrayList<Peer>();
                        gvl.value = value;
                        gvl.clock = 0;
                        gvl.peer = new Peer(gvl.value, gvl.value);
                        gvl.knowledge = new View[Network.size()];
                        gvl.view = new View(gvl.clock, gvl.neighbors);
                        gvl.leader = leader;
                        gvl.timeout_map = new HashMap<Peer, Long>();
                        gvl.state = state.NOTKNOWN;
                }
                catch( CloneNotSupportedException e ) {} // never happens
                return gvl;
        }

        @Override
        public void attach(NeighborhoodListener nl) {
                // TODO Auto-generated method stub

        }



        @Override
        public void detach(NeighborhoodListener nl) {
                // TODO Auto-generated method stub

        }



        @Override
        public void notifyAddListener(Node node, Long newId) {
                // TODO Auto-generated method stub

        }



        @Override
        public void notifyRemoveListener(Node node, Long newId) {
                // TODO Auto-generated method stub

        }


        @Override
        public List<String> infos(Node host) {
                List<String> res = new ArrayList<String>();
                res.add(""+host.getID()+" Voisins " + getNeighbors());

                //              res.add("Leader " + leaderId);
                //              res.add("index " + computation_index.getNum()+":"+computation_index.getId());
                //              res.add("num : "+computation_num);
                //              res.add("id : "+computation_id);
                //              res.add("parent : "+parent);
                //              res.add("beacon counter : "+ arrived_beacon_counter);
                return res;
        }

}