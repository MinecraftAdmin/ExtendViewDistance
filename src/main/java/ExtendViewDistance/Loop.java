package ExtendViewDistance;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class Loop {


    private boolean             isRun           = false;            // 正在運行中
    private Map<Player, Order>  priorityOrder   = new HashMap<>();  // 擁有優先權重的緩



    public void run() {
        if (isRun) return;
        isRun = true;

        long a = System.currentTimeMillis();

        // 新增 / 重新檢查玩家位置
        {
            for (Player player : Bukkit.getOnlinePlayers()) {
                this.priorityOrder.computeIfAbsent(player, v -> new Order(player));
            }
            Set<Player> players = this.priorityOrder.keySet();
            for (Player player : players) {
                if (!player.isOnline() || !Bukkit.getWorlds().contains(player.getWorld())) {
                    // 玩家已離線 / 世界已離線
                    this.priorityOrder.remove(player);
                    continue;
                }

                this.priorityOrder.get(player).move();
            }
        }


        //System.out.println("a " + (System.currentTimeMillis() - a));

        // 抽選出一定的量, 發送區塊
        {
            Object[] players = this.priorityOrder.keySet().toArray();
            if (players.length != 0) {
                for (int i = 0, isSend = 0; i < Value.tickSendChunkAmount && isSend < Value.tickSendChunkAmount; ++i) {
                    Player  player  = (Player) players[(int) (Math.random() * players.length)];
                    Order   order   = this.priorityOrder.get(player);
                    Waiting waiting = order.get();

                    if (waiting != null) {
                        isSend++;

                        Chunk chunk = Value.extend.getChunk(waiting.world, waiting.x, waiting.z);
                        Value.extend.playerSendViewDistance(player, order.clientViewDistance);
                        if (chunk != null) {
                            Value.extend.playerSendChunk(player, chunk);
                            Value.extend.playerSendChunkLightUpdate(player, chunk);
                        }

                        //System.out.println("b " + i + " " + (System.currentTimeMillis() - a));
                    }
                }
            }
        }

        //System.out.println("c " + (System.currentTimeMillis() - a));

        isRun = false;
    }







    /**
     * 表示一個等待加載中的區塊請求
     */
    private static class Waiting {
        public World    world;
        public int      x;
        public int      z;
        public Status   status = Status.wait;  // 狀態

        Waiting(World world, int x, int z) {
            this.world  = world;
            this.x      = x;
            this.z      = z;
        }
    }


    /**
     * 狀態
     */
    private enum Status {
        wait,               // 一切都還沒開始
        submitted,          // 已提交
    }








    /**
     * 優先續控制庫
     */
    private static class Order {
        private Player                      player;
        private Map<Long, Waiting>          waitingMap          = new HashMap<>();
        private Map<Byte, List<Waiting>>    waitingPriority     = new HashMap<>();      // 權重分配
        private Integer                     nowX                = null;                 // 當前玩家區塊座標X
        private Integer                     nowZ                = null;                 // 當前玩家區塊座標Z
        private World                       nowWorld            = null;
        public  int                         clientViewDistance  = 0;



        public Order(Player player) {
            this.player     = player;
            move(player); // 初始化
        }



        public void move() {
            move(this.player);
        }
        public void move(Player player) {
            int viewDistance = player.getClientViewDistance(); // 取得客戶端視野距離
            if (viewDistance > Value.extendViewDistance)
                viewDistance = Value.extendViewDistance;
            if (viewDistance < 1)
                viewDistance = 1;
            move(player.getWorld(), player.getLocation().getBlockX() >> 4, player.getLocation().getBlockZ() >> 4, viewDistance); // 初始化
        }
        /**
         * 玩家移動座標
         * @param moveX 移動到的新區塊位置X
         * @param moveZ 移動到的新區塊位置Z
         */
        public void move(World moveWorld, int moveX, int moveZ, int viewDistance) {
            if (this.nowWorld == null || nowX == null || nowZ == null || this.nowWorld != moveWorld || this.nowX != moveX || this.nowZ != moveZ || this.clientViewDistance != viewDistance) {
                // 有需要重算權重


                int minX = moveX - viewDistance - 1;
                int minZ = moveZ - viewDistance - 1;
                int maxX = moveX + viewDistance + 1;
                int maxZ = moveZ + viewDistance + 1;
                int serverViewDistance = Bukkit.getServer().getViewDistance();
                int minServerX = moveX - serverViewDistance;
                int minServerZ = moveZ - serverViewDistance;
                int maxServerX = moveX + serverViewDistance;
                int maxServerZ = moveZ + serverViewDistance;


                // 超出範圍的移除
                Object[] arrayWaiting = this.waitingMap.entrySet().toArray();
                if (arrayWaiting.length > 0)
                    for (int i = this.waitingMap.size() - 1 ; i >= 0 ; i-- ) {
                        Map.Entry<Long, Waiting> entry = (Map.Entry<Long, Waiting>) arrayWaiting[i];
                        long    key = entry.getKey();
                        int     x   = (int) key;
                        int     z   = (int) (key >> 32);

                        if (nowWorld != entry.getValue().world) {
                            // 已經切換世界
                            this.waitingMap.remove(key);
                        } else if (x < minX || x > maxX || z < minZ || z > maxZ) {
                            // 超出插件的擴展距離
                            this.waitingMap.remove(key);
                            Value.extend.playerSendUnloadChunk(player, x, z);
                        } else if (x >= minServerX && x <= maxServerX && z >= minServerZ && z <= maxServerZ) {
                            // 在伺服器的距離內
                            Waiting waiting = this.waitingMap.get(key);
                            if (waiting != null) {
                            } else {
                                waiting = new Waiting(moveWorld, x, z);
                                this.waitingMap.put(key, waiting);
                            }
                            waiting.status = Status.submitted;
                        }
                    }


                // 緩存視野距離內全部區塊
                for (int x = minX ; x < maxX ; ++x) {
                    for (int z = minZ ; z < maxZ ; ++z) {

                        if (x >= minServerX && x <= maxServerX && z >= minServerZ && z <= maxServerZ) continue; // 在伺服器的距離內

                        long chunkKey = Chunk.getChunkKey(x, z);
                        // 如果沒有資料的話則新增
                        if (!this.waitingMap.containsKey(chunkKey)) waitingMap.put(chunkKey, new Waiting(moveWorld, x, z));
                    }
                }


                // 計算權重
                this.waitingPriority.clear();
                this.waitingMap.forEach((key, value) -> {
                    int x   = key.intValue();
                    int z   = (int) (key >> 32);

                    // 計算距離權重
                    byte priority = (byte) (Math.abs(moveX - x) + Math.abs(moveZ - z));

                    List<Waiting> waitingList = this.waitingPriority.computeIfAbsent(priority, k -> new ArrayList<>());
                    waitingList.add(value);
                });


                this.nowX       = moveX; // 更新座標X
                this.nowZ       = moveZ; // 更新座標Z
                this.nowWorld   = moveWorld;

                this.clientViewDistance = viewDistance;
            }
        }



        /**
         * 取得最優先的請求
         * @return 請求
         */
        public Waiting get() {
            byte size = (byte) this.clientViewDistance;
            for (byte i = 0 ; i < size ; i++) {
                List<Waiting> waitingList = this.waitingPriority.get(i);
                if (waitingList == null || waitingList.size() == 0) continue;

                for (Waiting waiting : waitingList) {
                    if (waiting.status == Status.wait) {
                        //System.out.println("s1:" + this.waitingPriority.size() + " s2" + this.waitingMap.size() + " x:" + waiting.x + " z:" + waiting.z);
                        // 有找到了, 返回
                        waiting.status = Status.submitted;  // 狀態為已提交
                        return waiting;
                    }
                }
            }
            return null;
        }



        public void clear() {
            this.waitingPriority.clear();
            this.waitingMap.clear();
        }
    }

}
