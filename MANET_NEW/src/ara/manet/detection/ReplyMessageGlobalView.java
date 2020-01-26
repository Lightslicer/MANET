package src.ara.manet.detection;

import src.ara.manet.algorithm.election.GlobalViewLeader.Peer;
import src.ara.util.Message;

public class ReplyMessageGlobalView extends Message {

        public long idLeader;
        public Peer peer;

        public ReplyMessageGlobalView(long idsrc, long iddest, int pid, long idLeader, Peer  peer) {
                super(idsrc, iddest, pid);
                this.idLeader = idLeader;
                this.peer=peer;
                // TODO Auto-generated constructor stub
        }
}