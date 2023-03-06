import java.io.IOException;
import java.util.*;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
public class LeaderElection implements Watcher{
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;
    private static final String ELECTION_NAMESPACE = "/election";
    private String currentZnodeName;
    private static final String TARGET_ZNODE = "/target_znode";
    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        LeaderElection le = new LeaderElection();
        le.connectToZooKeeper();
       le.volunteerForLeadership();
       le.reElectLeader();
        le.watchTargetZNode();
        le.run();
        le.close();
        System.out.println("Disconnected from zookeeper ending application");
    }
    public void connectToZooKeeper() throws IOException{
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);

    }
    public void volunteerForLeadership() throws KeeperException, InterruptedException{
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix,new byte[]{},ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("znode name"+znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE+"/", "");
    }
    public void reElectLeader() throws KeeperException, InterruptedException{
        Stat predStat = null;
        String predName = "";
        while(predStat==null){
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE,false);
        Collections.sort(children);
        String smallest = children.get(0);
        if(smallest.equals(currentZnodeName)){
            System.out.println("I am the leader");
            return;
        }
        else{
            System.out.println("I am not the leader, "+smallest+"is the leader");
            int predIndex = Collections.binarySearch(children, currentZnodeName)-1;
            predName = children.get(predIndex);
            predStat = zooKeeper.exists(ELECTION_NAMESPACE+"/"+predName,this);
        }
        }
        System.out.println("Watching znode "+predName);
        System.out.println();
    }
    public void watchTargetZNode() throws KeeperException, InterruptedException{
        Stat stat = zooKeeper.exists(TARGET_ZNODE,this);
        if(stat==null){
            return;
        }
        //get data and children
        byte[] data = zooKeeper.getData(TARGET_ZNODE, this, stat);
        List<String> children = zooKeeper.getChildren(TARGET_ZNODE, this);
        String childs[] = new String[children.size()];
        int i=0;
        for(String child:children){
            childs[i++]=child;
        }
        System.out.println("Data: "+ data + "children "+Arrays.toString(childs));
    }
    public void run() throws IOException, InterruptedException{
        synchronized(zooKeeper){
            zooKeeper.wait();
        }
    }
    private void close() throws InterruptedException{
        this.zooKeeper.close();
    }
    @Override
    public void process(WatchedEvent event) {
        // TODO Auto-generated method stub
        switch(event.getType()){
            case None:
                if(event.getState()==Event.KeeperState.SyncConnected){
                    System.out.println("Connected to zookeeper");
                }else{
                    System.out.println("Disconnected from zookeeper");
                    zooKeeper.notifyAll();
                }
                break;
            case NodeDeleted:
                try {
                    reElectLeader();
                } catch (KeeperException e) {}
                    catch(InterruptedException e){
                        e.printStackTrace();
                    }
                break;
            default:
                    System.out.println("Must be something else");
                }
            
                
        }
    }

