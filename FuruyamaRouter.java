package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;



/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */

public class EncountedRecorderRouter extends ActiveRouter {
    private static final int MAX_QUEUE_SIZE = 50; // キューの最大サイズ
    private LinkedList<Integer> nodeQueue; // ノード番号を格納するキュー
    private Timer timer; // タイマー

    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public EncountedRecorderRouter(Settings s) {
        super(s);
        nodeQueue = new LinkedList<>();
        timer = new Timer();
        startQueueCleanupTask();
        //TODO: read&use epidemic router specific settings (if any)
       
    }
    
    public LinkedList<Integer> getNodeQueue() {
        return this.nodeQueue;
        
        
    }
    
    /**
     * Copy constructor.
     * @param r The router prototype where setting values are copied from
     */
    protected EncountedRecorderRouter(EncountedRecorderRouter r) {
        super(r);
        this.nodeQueue = new LinkedList<>(r.nodeQueue);
        this.timer = new Timer();
        startQueueCleanupTask();
        //TODO: copy epidemic settings here (if any)
    }

    private void startQueueCleanupTask() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanUpQueue();
            }
        }, 0, 5 * 60 * 1000); // 5分ごとに実行
    }

    private void cleanUpQueue() {
        if (nodeQueue.size() > MAX_QUEUE_SIZE) {
            nodeQueue.removeFirst(); // キューの先頭（古い要素）を削除
        }
    }
    
    @Override
    public void update() {
        super.update();
        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }
        /**/

     // メッセージを優先度の高い順に並べ替える
        List<Message> sortedMessages = new ArrayList<>(getHost().getMessageCollection());
        Collections.sort(sortedMessages);

        
        /**/
        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        // then try any/all message to any/all connection
        /*this.tryAllMessagesToAllConnections();*/
        /**/
        
        // 並べ替えたメッセージを使用して転送を試みる
        for (Message msg : sortedMessages) {
            if (tryMessageToAllConnections(msg)) {
                return; // 転送を開始した場合、他のメッセージを試みない
            }
        }
        
        /**/
    }

    
    @Override
    public EncountedRecorderRouter replicate() {
        return new EncountedRecorderRouter(this);
    }  
    public void changedConnection(Connection con) {
        super.changedConnection(con);
        
        // 新しく接続されたノードのIDを取得
        if (con.isUp()) {
            DTNHost otherHost = con.getOtherNode(getHost());
            int otherAddress = otherHost.getAddress();
            /*Collection<Message> myMessages = getHost().getMessageCollection();*/
            List<Message> sortedMessages = new ArrayList<>(getHost().getMessageCollection());
            Collections.sort(sortedMessages);
            System.out.println(sortedMessages);

            
            // ノードIDをキューに追加
            if (!nodeQueue.contains(otherAddress)) {
                if (nodeQueue.size() >= MAX_QUEUE_SIZE) {
                    nodeQueue.removeFirst(); // キューの先頭を削除
                }
                nodeQueue.addLast(otherAddress); // 新しいノードIDを追加
                System.out.println("着目ノード:"+ getHost()+" 相手ノード:" + otherAddress);
                System.out.println( "追加されたノード:" + otherAddress +", 追加後の"+ getHost()+"のキュー: " + nodeQueue);
            }
            EncountedRecorderRouter otherRouter = (EncountedRecorderRouter) otherHost.getRouter();
            System.out.println("相手ノード(ノードID:" + otherAddress +")のノードリスト: " + otherRouter.getNodeQueue());
          
            /*----------------------------------ここからがノードリスト比較部分-------------------------------------------*/
            
            
          for (Message myMsg : sortedMessages) {
                DTNHost myDestination = myMsg.getTo();
                if (otherRouter.getNodeQueue().contains(myDestination.getAddress())) {
                    System.out.println("遭遇ノード:"+otherAddress+"はメッセージID:" + myMsg.getId() + "の宛先" + myDestination.getAddress() + "とすれ違ってます");
                   
           /*----------------------------------ここまでがノードリスト比較部分-------------------------------------------*/        
                // メッセージの優先度を上げる
                   myMsg.setPriority(myMsg.getPriority() + 1);
                   System.out.println("メッセージID:" + myMsg.getId() + "の優先度が上がりました。新しい優先度: " + myMsg.getPriority());
                   List<Message> sorted = new ArrayList<>(getHost().getMessageCollection());
                   
                   Collections.sort(sorted);
                   System.out.println(sorted);
                   
                }
                }
          System.out.println("---------------------------------------------------------------------");
        
          /*-------------------------------------------------------------------------------------------------*/    
        }
        
     }   
}
