package src.ara.manet.detection;

import src.ara.manet.algorithm.election.GlobalViewLeader.Peer;
import src.ara.util.Message;

public class RemoveMessageGlobalView extends Message{

        public long targetId;
        public Peer peer;

        public RemoveMessageGlobalView(long idsrc, long iddest, int pid, long targetId, Peer peer) {
                super(idsrc, iddest, pid);
                this.targetId = targetId;
                this.peer=peer;
        }
}