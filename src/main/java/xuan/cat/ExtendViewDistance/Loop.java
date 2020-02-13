package xuan.cat.ExtendViewDistance;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xuan.cat.XuanCatAPI.NMS;
import xuan.cat.XuanCatAPI.Packet;
import xuan.cat.XuanCatAPI.api.event.packet.PacketDelayedTrigger;
import xuan.cat.XuanCatAPI.api.nms.world.ExtendChunk;
import xuan.cat.XuanCatAPI.api.nms.world.ExtendChunkCache;

import java.util.*;

public class Loop {


    private static          boolean             isRun           = false;            // 正在運行中
    private static final    Map<Player, Order>  priorityOrder   = new HashMap<>();  // 擁有優先權重的緩



    public synchronized void run() {
        if (isRun) return;
        isRun = true;

        try {
            synchronized (priorityOrder) {

                // 新增 / 重新檢查玩家位置
                synchronized (Bukkit.getOnlinePlayers()) {
                    Collection<? extends Player> playerCollection = Bukkit.getOnlinePlayers();
                    for (Player player : playerCollection) {
                        Loop.priorityOrder.computeIfAbsent(player, v -> new Order(player));
                    }

                    Set<Player> players     = Loop.priorityOrder.keySet();
                    //List<World> worldList   = Bukkit.getWorlds();
                    for (Player player : players) {
                        if (!NMS.Player(player).getConnection().isConnected()) {
                            // 玩家已離線
                            Loop.priorityOrder.remove(player);
                            continue;
                        }

                        Loop.priorityOrder.get(player).move();
                    }
                }


                //System.out.println("a " + (System.currentTimeMillis() - a));

                // 抽選出一定的量, 發送區塊
                Object[] players = Loop.priorityOrder.keySet().toArray();
                if (players.length != 0) {
                    for (int i = 0, isSend = 0; i < Value.tickSendChunkAmount && isSend < Value.tickSendChunkAmount; ++i) {
                        Player  player  = (Player) players[(int) (Math.random() * players.length)];
                        Order   order   = Loop.priorityOrder.get(player);

                        if (order.isChangeWorld() || order.setChangeWorld(player.getWorld())) {
                            // 世界發生改變
                            // 將全部區塊卸除
                            order.unloadAllChunk();
                            continue;
                        }

                        Waiting waiting = order.get();
                        if (waiting != null) {
                            isSend++;

                            // 取得區塊緩存
                            ExtendChunkCache chunkCache = NMS.World(waiting.world).getChunkCache(ExtendChunk.Status.LIGHT, waiting.x, waiting.z, true);
                            if (chunkCache != null) {

                                // 防透視礦物作弊
                                // 替換全部指定材質
                                for (Map.Entry<Material, Material[]> entry : Value.conversionMaterialListMap.entrySet()) {
                                    chunkCache.replaceAllMaterial(entry.getValue(), entry.getKey());
                                }

                                Chunk chunk = chunkCache.asChunk(waiting.world);

                                Packet.callServerViewDistancePacket(player, order.clientViewDistance);
                                Packet.callServerMapChunkPacket(player, chunk);
                                Packet.callServerLightUpdatePacket(player, chunk);

                                //System.out.println("x:" + chunk.getX() + " z:" + chunk.getZ());
                            }

                            //System.out.println("b " + i + " " + (System.currentTimeMillis() - a));
                        }
                    }
                }




            }
        } catch (Exception ex) {
            ex.fillInStackTrace();
        }
        //long a = System.currentTimeMillis();

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
     * 等待更改世界
     * 這是為了確保卸載區塊封包, 要在新世界區塊封包之前發送完畢
     * @param player 玩家
     * @param trigger 事件延遲觸發氣
     * @return 是否在等待切換世界中
     */
    public static boolean addWaitingChangeWorldPacket(Player player, PacketDelayedTrigger trigger) {
        Order order = Loop.priorityOrder.get(player);
        if (order != null && order.setChangeWorld(player.getWorld())) {
            trigger.delay();
            order.addWaitingChangeWorldPacket(trigger);
            return true;
        }
        return false;
    }




    /**
     * 優先續控制庫
     */
    private static class Order {

        private boolean                     isChangeWorld               = false;                // 已經更換世界
        private Player                      player;
        private Map<Long, Waiting>          waitingMap                  = new HashMap<>();
        private Map<Byte, List<Waiting>>    waitingPriority             = new HashMap<>();      // 權重分配
        private List<PacketDelayedTrigger>  waitingChangeWorldPacket    = new ArrayList<>();
        private World                       waitingChangeWorld          = null;
        private boolean                     runWaiting                  = false;                // 正在處裡以上等待的區塊
        private Integer                     nowX                        = null;                 // 當前玩家區塊座標X
        private Integer                     nowZ                        = null;                 // 當前玩家區塊座標Z
        private World                       nowWorld                    = null;
        public  int                         clientViewDistance          = 0;



        public Order(Player player) {
            this.player = player;
            move(player); // 初始化
        }



        public boolean setChangeWorld(World moveWorld) {
            this.isChangeWorld = !this.nowWorld.getName().equals(moveWorld.getName());
            if (this.isChangeWorld) {
                this.waitingChangeWorld = moveWorld;
            }
            return this.isChangeWorld;
        }


        public boolean isChangeWorld() {
            return this.isChangeWorld;
        }



        public void addWaitingChangeWorldPacket(PacketDelayedTrigger trigger) {
            this.waitingChangeWorldPacket.add(trigger);
        }


        /**
         * 卸除全部區塊
         */
        public void unloadAllChunk() {
            if (this.runWaiting) return;

            this.runWaiting = true;

            for (Waiting waiting : Collections.unmodifiableCollection(this.waitingMap.values())) {
                if (waiting.status == Status.submitted) {
                    Packet.callServerUnloadChunkPacket(player, waiting.x, waiting.z);
                }
            }
            for (PacketDelayedTrigger trigger : Collections.unmodifiableList(waitingChangeWorldPacket)) {
                trigger.trigger();
            }
            this.waitingChangeWorldPacket.clear();
            this.clear();
            this.nowWorld           = this.waitingChangeWorld;
            this.nowX               = null;
            this.nowZ               = null;
            this.waitingChangeWorld = null;
            this.isChangeWorld      = false;
            this.runWaiting         = false;
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
            if (!NMS.Player(player).getConnection().isConnected() || this.isChangeWorld) return;  // 已離線 / 已切換世界

            if (this.nowWorld == null || nowX == null || nowZ == null || this.nowWorld != moveWorld || this.nowX != moveX || this.nowZ != moveZ || this.clientViewDistance != viewDistance) {
                // 有需要重算權重

                // 檢查權限節點
                for (int i = Value.extendViewDistance ; i > 0 ; i--) {
                    if (viewDistance > i && this.player.hasPermission("extend_view_distance." + i)) {
                        viewDistance = i;
                        break;
                    }
                }


                int minX = moveX - (viewDistance + 1);
                int minZ = moveZ - (viewDistance + 1);
                int maxX = moveX + (viewDistance + 1);
                int maxZ = moveZ + (viewDistance + 1);
                int serverViewDistance = Bukkit.getServer().getViewDistance();
                int minServerX = moveX - (serverViewDistance + 1);
                int minServerZ = moveZ - (serverViewDistance + 1);
                int maxServerX = moveX + (serverViewDistance + 1);
                int maxServerZ = moveZ + (serverViewDistance + 1);


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
                            //this.waitingMap.remove(key);
                        } else if (x < minX || x > maxX || z < minZ || z > maxZ) {
                            // 超出插件的擴展距離
                            Waiting waiting = this.waitingMap.remove(key);
                            if (waiting != null && waiting.status == Status.submitted)
                                Packet.callServerUnloadChunkPacket(player, x, z);
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

                        long chunkKey = (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
                        if (x >= minServerX && x <= maxServerX && z >= minServerZ && z <= maxServerZ) {
                            // 在伺服器的距離內
                            Waiting waiting = this.waitingMap.get(chunkKey);
                            if (waiting != null) {
                            } else {
                                waiting = new Waiting(moveWorld, x, z);
                                this.waitingMap.put(chunkKey, waiting);
                            }
                            waiting.status = Status.submitted;

                        } else {
                            // 如果沒有資料的話則新增
                            if (!this.waitingMap.containsKey(chunkKey)) waitingMap.put(chunkKey, new Waiting(moveWorld, x, z));

                        }
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
            if (!NMS.Player(player).getConnection().isConnected() || this.isChangeWorld) return null;  // 已離線 / 已切換世界

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
