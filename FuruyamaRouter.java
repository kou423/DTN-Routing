package routing;

import core.DTNHost;
import core.Settings;
import java.util.LinkedList;
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
        return nodeQueue;
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
            return; // transferring, don't try other connections yet
        }

        // ノード番号をキューに追加する例
        DTNHost currentHost = getCurrentHost();
        if (currentHost != null) {
            int address = currentHost.getAddress();
            if (!nodeQueue.contains(address)) {
                if (nodeQueue.size() >= MAX_QUEUE_SIZE) {
                    nodeQueue.removeFirst(); // キューの先頭を削除
                }
                nodeQueue.addLast(address); // 新しいノード番号を追加
                System.out.println("Added address: " + address + " to nodeQueue: " + nodeQueue);
            }
        }

        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        // then try any/all message to any/all connection
        this.tryAllMessagesToAllConnections();
    }



    private DTNHost getCurrentHost() {
        // 適切な方法で現在のホストを取得する必要があります。
        return null;
    }

    @Override
    public FuruyamaRouter replicate() {
        return new FuruyamaRouter(this);
    }
}