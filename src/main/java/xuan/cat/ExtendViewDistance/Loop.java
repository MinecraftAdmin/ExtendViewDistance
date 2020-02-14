package xuan.cat.ExtendViewDistance;

import org.bukkit.*;
import org.bukkit.entity.Player;
import xuan.cat.XuanCatAPI.NMS;
import xuan.cat.XuanCatAPI.Packet;
import xuan.cat.XuanCatAPI.api.event.packet.PacketDelayedTrigger;
import xuan.cat.XuanCatAPI.api.nms.world.ExtendChunk;
import xuan.cat.XuanCatAPI.api.nms.world.ExtendChunkCache;

import java.util.*;

public class Loop implements Runnable {

/*

    public static void main(String[] args) {
        ChunkMapView playerView = new ChunkMapView();

        playerView.move(1000000, 10000000);

        long start = System.currentTimeMillis();
        for (int i = 0 ; i < 396900 ; i++) {
            playerView.get();
        }


        //System.out.println(playerView.move(1000, 1000).length);
        ;
        //long[] c = ;


        ChunkMapView.debug(playerView.getChunkMap());



        System.out.println(System.currentTimeMillis() - start);

    }


 */








    private static          boolean                 isRun                   = false;            // 正在運行中
    private static final    Map<Player, PlayerView> playerPlayerViewHashMap = new HashMap<>();







    /**
     * 表示玩家視野
     */
    public static class PlayerView {
        public          Player                       player;
        public          ChunkMapView                 chunkMapView;
        public          World                        world;
        public final    List<PacketDelayedTrigger>   packetTriggerListMap    = new ArrayList<>();
        public          boolean                      waitingChangeWorld      = false;

        public boolean isChangeWorld(World moveWorld) {
            this.waitingChangeWorld = !this.world.equals(moveWorld);
            return this.waitingChangeWorld;
        }
    }







    @Override
    public void run() {

        if (isRun) return;
        isRun = true;

        try {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();


            int extendViewDistance = Value.extendViewDistance;
            // 伺服器視野距離
            int serverViewDistance = Bukkit.getViewDistance() + Value.serverFieldViewCorrection;
            if (serverViewDistance < 1 ) {
                serverViewDistance = 1;
            } else if (serverViewDistance > 32) {
                serverViewDistance = 32;
            }


            // 如果玩家沒有自己的 區塊視野距離分配
            // 則創建
            for (Player player : onlinePlayers) {
                PlayerView playerView = playerPlayerViewHashMap.get(player);
                if (playerView == null) {
                    playerView = new PlayerView();
                    playerPlayerViewHashMap.put(player, playerView);

                    playerView.player       = player;
                    playerView.chunkMapView = new ChunkMapView();
                    playerView.world        = player.getWorld();

                    // 初始化區塊視野地圖
                    playerView.chunkMapView.serverViewDistance = serverViewDistance;
                    playerView.chunkMapView.extendViewDistance = playerMaxViewDistance(player, extendViewDistance);
                    playerView.chunkMapView.move(player.getLocation());
                }
            }



            // 當玩家確定沒問題後, 就可以放入此陣列中
            PlayerView[]    playerViews     = new PlayerView[playerPlayerViewHashMap.size()];
            int             playerViewsRead = 0;



            for (PlayerView playerView : playerPlayerViewHashMap.values()) {
                if (!NMS.Player(playerView.player).getConnection().isConnected()) {
                    // 玩家已離線
                    playerPlayerViewHashMap.remove(playerView.player);
                    continue;
                }

                if (playerView.waitingChangeWorld || playerView.isChangeWorld(playerView.player.getWorld())) {
                    // 玩家已切換世界
                    playerView.waitingChangeWorld   = true;
                    long[] isSendChunks = playerView.chunkMapView.getIsSendChunkList();
                    for (long chunkKey : isSendChunks) {
                        Packet.callServerUnloadChunkPacket(playerView.player, ChunkMapView.getX(chunkKey), ChunkMapView.getZ(chunkKey));
                    }
                    playerView.world                = playerView.player.getWorld();
                    playerView.chunkMapView.clear();
                    playerView.chunkMapView.setCenter(playerView.player.getLocation());
                    playerView.waitingChangeWorld   = false;
                        for (PacketDelayedTrigger packetDelayedTrigger : Collections.unmodifiableList(playerView.packetTriggerListMap)) {
                            packetDelayedTrigger.trigger();
                        }
                    continue;
                }

                // 沒問題, 進行移動
                playerView.chunkMapView.extendViewDistance = playerMaxViewDistance(playerView.player, extendViewDistance);
                long[] removeChunkKeyList = playerView.chunkMapView.move(playerView.player.getLocation());
                // 已經超出視野距離的區塊
                for (long chunkKey : removeChunkKeyList) {
                    Packet.callServerUnloadChunkPacket(playerView.player, ChunkMapView.getX(chunkKey), ChunkMapView.getZ(chunkKey));
                }

                playerViews[ playerViewsRead++ ] = playerView;
            }



            // 當確定取得後放入此陣列中
            PlayerView[]        waitingSendPlayerView   = new PlayerView        [ Value.tickSendChunkAmount ];
            Player[]            waitingSendPlayer       = new Player            [ Value.tickSendChunkAmount ];
            World[]             waitingSendWorld        = new World             [ Value.tickSendChunkAmount ];
            ExtendChunkCache[]  waitingSendChunkCache   = new ExtendChunkCache  [ Value.tickSendChunkAmount ];
            int                 waitingSendRead         = 0;



            // 確保循環在限制範圍內
            if (playerViewsRead > 0) {
                for (int i = 0, isSend = 0; i < Value.tickSendChunkAmount && isSend < Value.tickSendChunkAmount; ++i) {
                    PlayerView playerView = playerViews[(int) (Math.random() * playerViewsRead)];

                    Long chunkKey = playerView.chunkMapView.get();


                    if (Value.worldBlacklist.contains(playerView.world.getName())) continue; // 在黑名單內


                    if (chunkKey != null) {
                        ExtendChunkCache chunkCache = NMS.World(playerView.world).getChunkCache(ExtendChunk.Status.LIGHT, ChunkMapView.getX(chunkKey), ChunkMapView.getZ(chunkKey), true);

                        if (chunkCache != null) {
                            waitingSendPlayerView   [ waitingSendRead ] = playerView;
                            waitingSendPlayer       [ waitingSendRead ] = playerView.player;
                            waitingSendWorld        [ waitingSendRead ] = playerView.world;
                            waitingSendChunkCache   [ waitingSendRead ] = chunkCache;
                            waitingSendRead++;
                            isSend++;
                        }
                    }
                }
            }



            // 剩下的處裡不強制同步
            isRun = false;



            for (int i = 0 ; i < waitingSendRead ; ++i) {
                PlayerView          playerView  = waitingSendPlayerView [ i ];
                Player              player      = waitingSendPlayer     [ i ];
                World               world       = waitingSendWorld      [ i ];
                ExtendChunkCache    chunkCache  = waitingSendChunkCache [ i ];


                if (playerView.waitingChangeWorld) continue;


                // 防透視礦物作弊
                // 替換全部指定材質
                for (Map.Entry<Material, Material[]> entry : Value.conversionMaterialListMap.entrySet()) {
                    chunkCache.replaceAllMaterial(entry.getValue(), entry.getKey());
                }

                Chunk chunk = chunkCache.asChunk(world);

                Packet.callServerViewDistancePacket(player, playerView.chunkMapView.extendViewDistance);
                Packet.callServerMapChunkPacket(player, chunk);
                Packet.callServerLightUpdatePacket(player, chunk);
            }




        } catch (Exception ex) {
            ex.printStackTrace();
            isRun = false;
        }
    }

    //private static final








    /**
     * 計算玩家最大可擁有的距離
     * @param player 玩家
     * @param extendViewDistance 擴展視野距離
     * @return 視野距離
     */
    public static int playerMaxViewDistance(Player player, int extendViewDistance) {

        int viewDistance = player.getClientViewDistance(); // 取得客戶端視野距離
        if (viewDistance > extendViewDistance)
            viewDistance = extendViewDistance;
        if (viewDistance < 1)
            viewDistance = 1;


        // 檢查權限節點
        for (int i = Value.extendViewDistance ; i > 0 ; i--) {
            if (viewDistance > i && player.hasPermission("extend_view_distance." + i)) {
                viewDistance = i;
                break;
            }
        }

        return viewDistance;
    }







    /**
     * 等待更改世界
     * 這是為了確保卸載區塊封包, 要在新世界區塊封包之前發送完畢
     * @param player 玩家
     * @param trigger 事件延遲觸發氣
     * @return 是否在等待切換世界中
     */
    public static boolean waitingChangeWorldPacket(Player player, PacketDelayedTrigger trigger) {
        PlayerView playerView = playerPlayerViewHashMap.get(player);
        if (playerView != null && playerView.isChangeWorld(player.getWorld())) {
            trigger.delay();
            playerView.packetTriggerListMap.add(trigger);
            return true;
        }
        return false;
    }












}
