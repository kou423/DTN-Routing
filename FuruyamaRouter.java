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

public class FuruyamaRouter extends ActiveRouter {
    private static final int MAX_QUEUE_SIZE = 3; // キューの最大サイズ
    private LinkedList<Integer> nodeQueue; // ノード番号を格納するキュー
    private Timer timer; // タイマー

    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public FuruyamaRouter(Settings s) {
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
    protected FuruyamaRouter(FuruyamaRouter r) {
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
            return; // 転送中なので、まだ他の接続を試さないでください
        }

        // まずは最終受信者に配信できるメッセージを試してください
        if (exchangeDeliverableMessages() != null) {
            return; // 転送を開始しました。他の転送はまだ試さないでください
        }

        // 次に、すべてのメッセージをすべての接続に試します
        this.tryAllMessagesToAllConnections();
    }

    
    @Override
    public FuruyamaRouter replicate() {
        return new FuruyamaRouter(this);
    }  
    public void changedConnection(Connection con) {
        super.changedConnection(con);
        
        // 新しく接続されたノードのIDを取得
        if (con.isUp()) {
            DTNHost otherHost = con.getOtherNode(getHost());
            int otherAddress = otherHost.getAddress();
            
            // ノードIDをキューに追加
            if (!nodeQueue.contains(otherAddress)) {
                if (nodeQueue.size() >= MAX_QUEUE_SIZE) {
                    nodeQueue.removeFirst(); // キューの先頭を削除
                }
                nodeQueue.addLast(otherAddress); // 新しいノードIDを追加
                System.out.println("着目ノード:" + getHost());
                System.out.println("追加されたノード:" + otherAddress + ", 追加後の" + getHost() + "のキュー: " + nodeQueue);
            }
            FuruyamaRouter otherRouter = (FuruyamaRouter) otherHost.getRouter();
            System.out.println("相手ノード(ノードID:" + otherAddress + ")のノードリスト: " + otherRouter.getNodeQueue());

            /*----------------------------------ノードリスト比較部分-------------------------------------------*/
            
            Collection<Message> myMessages = getHost().getMessageCollection();
            for (Message myMsg : myMessages) {
                DTNHost myDestination = myMsg.getTo();
                if (otherRouter.getNodeQueue().contains(myDestination.getAddress())) {
                    System.out.println("遭遇ノード:" + otherAddress + "はメッセージID:" + myMsg.getId() + "の宛先" + myDestination.getAddress() + "とすれ違ってます");

                    // メッセージを優先的に渡す処理
                    if (exchangeDeliverableMessages() != null) {
                        // メッセージが配信された場合、処理を終了
                        return;
                    }
                }
            }
            System.out.println("---------------------------------------------------------------------");
        }
    }
   
}
