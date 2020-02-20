package xuan.cat.ExtendViewDistance;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import xuan.cat.XuanCatAPI.NMS;
import xuan.cat.XuanCatAPI.Packet;
import xuan.cat.XuanCatAPI.api.event.packet.PacketDelayedTrigger;
import xuan.cat.XuanCatAPI.api.nms.world.ExtendChunk;
import xuan.cat.XuanCatAPI.api.nms.world.ExtendChunkCache;

import java.util.*;

public class Loop {

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








    //private static volatile boolean                 isRun                   = false;                // 正在運行中
    private static final    Map<Player, PlayerView> playerPlayerViewHashMap = new HashMap<>();
    //private static final    List<ChunkSend>         chunkSendList           = new ArrayList<>();

    //private static final    ChunkManager            chunkManager            = new ChunkManager(30, 1, 1, 1, 1);







    /**
     * 表示玩家視野
     */
    public static class PlayerView {
        public          Player                      player;
        public          ChunkMapView                chunkMapView;
        public          World                       world                   = null;
        public final    List<PacketDelayedTrigger>  packetTriggerListMap    = new ArrayList<>();
        public volatile boolean                     waitingChangeWorld      = false;
        public          int                         delayedSendTick         = Value.delayedSendTick;
        public          int                         totalRead               = 0;                        // 單個 tick 累計發送

        public boolean isChangeWorld(World moveWorld) {
            if (this.world == null || moveWorld == null) return this.waitingChangeWorld;
            this.waitingChangeWorld = !this.world.equals(moveWorld);
            return this.waitingChangeWorld;
        }
    }
    /**
     * 表示區塊顯示
     */
    public static class ChunkSend {
        public volatile PlayerView   playerView;
        public volatile Chunk        chunk;
    }







    public void runView() {
        long a = System.currentTimeMillis();

        try {
            Collection<? extends Player>    onlinePlayers           = Collections.unmodifiableCollection(Bukkit.getOnlinePlayers());
            Map<Player, PlayerView>         playerPlayerViewHashMap = Collections.unmodifiableMap(Loop.playerPlayerViewHashMap);


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
            {
                PlayerView  playerView;
                int         playerMaxViewDistance;
                boolean     changedViewDistance;

                for (Player player : onlinePlayers) {

                    playerView = playerPlayerViewHashMap.get(player);

                    if (playerView == null) {
                        playerView              = new PlayerView();
                        Loop.playerPlayerViewHashMap.put(player, playerView);

                        playerView.player       = player;
                        playerView.chunkMapView = new ChunkMapView();
                        playerView.world        = player.getWorld();

                        // 初始化區塊視野地圖
                        playerMaxViewDistance   = playerMaxViewDistance(player, extendViewDistance);
                        changedViewDistance     = playerView.chunkMapView.extendViewDistance != playerMaxViewDistance;
                        if (changedViewDistance) {
                            playerView.chunkMapView.markRangeWait(playerMaxViewDistance);
                            playerView.chunkMapView.extendViewDistance = playerMaxViewDistance;
                            Packet.callServerViewDistancePacket(playerView.player, playerMaxViewDistance);
                        }

                        playerView.chunkMapView.serverViewDistance = serverViewDistance;
                        playerView.chunkMapView.move(player.getLocation());
                    }
                }
            }


            long b = System.currentTimeMillis();


            // 當玩家確定沒問題後, 就可以放入此陣列中
            PlayerView[]    playerViews     = new PlayerView[ playerPlayerViewHashMap.size() ];
            int             playerViewsRead = 0;

            {
                Object[]    playerViewArray         = playerPlayerViewHashMap.values().toArray();
                PlayerView  playerView;
                int         playerMaxViewDistance;
                long[]      isSendChunks;
                long[]      removeChunkKeyList;
                boolean     changedViewDistance;
                for (Object o : playerViewArray) {

                    playerView = (PlayerView) o;

                    if (!NMS.Player(playerView.player).getConnection().isConnected()) {
                        // 玩家已離線
                        Loop.playerPlayerViewHashMap.remove(playerView.player);
                        continue;

                    } else if (playerView.delayedSendTick > 0) {
                        // 等待緩衝中
                        playerMaxViewDistance           = playerMaxViewDistance(playerView.player, extendViewDistance);
                        Packet.callServerViewDistancePacket(playerView.player, playerMaxViewDistance);
                        playerView.delayedSendTick--;
                        continue;

                    } else if (playerView.waitingChangeWorld || playerView.isChangeWorld(playerView.player.getWorld())) {
                        // 玩家已切換世界
                        playerView.waitingChangeWorld   = true;
                        isSendChunks                    = playerView.chunkMapView.getIsSendChunkList();
                        for (long chunkKey : isSendChunks) {
                            //System.out.println( ChunkMapView.getX(chunkKey) + " / " + ChunkMapView.getZ(chunkKey));
                            Packet.callServerUnloadChunkPacket(playerView.player, ChunkMapView.getX(chunkKey), ChunkMapView.getZ(chunkKey));
                        }
                        playerView.world                = playerView.player.getWorld();
                        playerView.chunkMapView.clear();
                        playerView.chunkMapView.setCenter(playerView.player.getLocation());
                        playerView.waitingChangeWorld   = false;
                        playerView.delayedSendTick      = Value.delayedSendTick;
                        for (PacketDelayedTrigger packetDelayedTrigger : Collections.unmodifiableList(playerView.packetTriggerListMap)) {
                            packetDelayedTrigger.trigger();
                        }
                        continue;
                    }

                    // 計算視野距離
                    playerMaxViewDistance               = playerMaxViewDistance(playerView.player, extendViewDistance);
                    changedViewDistance                 = playerView.chunkMapView.extendViewDistance != playerMaxViewDistance;
                    if (changedViewDistance) {
                        playerView.chunkMapView.markRangeWait(playerMaxViewDistance);
                        playerView.chunkMapView.extendViewDistance = playerMaxViewDistance;
                        Packet.callServerViewDistancePacket(playerView.player, playerMaxViewDistance);
                    }

                    // 沒問題, 進行移動
                    removeChunkKeyList = playerView.chunkMapView.move(playerView.player.getLocation());
                    // 已經超出視野距離的區塊
                    for (long chunkKey : removeChunkKeyList)
                        Packet.callServerUnloadChunkPacket(playerView.player, ChunkMapView.getX(chunkKey), ChunkMapView.getZ(chunkKey));
                    playerView.totalRead                = 0;
                    playerViews[ playerViewsRead++ ]    = playerView;
                }
            }

/*
                // 計算是否大幅度超出範圍
                Location move = playerView.player.getLocation();
                if (Math.abs(playerView.chunkMapView.getCenterX() - ChunkMapView.blockToChunk(move.getX())) > playerMaxViewDistance || Math.abs(playerView.chunkMapView.getCenterZ() - ChunkMapView.blockToChunk(move.getZ())) > playerMaxViewDistance) {
                    playerView.delayedSendTick      = Value.delayedSendTick;
                    playerView.waitingChangeWorld   = true;
                }

 */


            long c = System.currentTimeMillis();


/*
            // 當確定取得後放入此陣列中
            PlayerView[]        waitingSendPlayerView   = new PlayerView[ Value.tickSendChunkAmount ];
            Player[]            waitingSendPlayer       = new Player    [ Value.tickSendChunkAmount ];
            Chunk[]             waitingSendChunk        = new Chunk     [ Value.tickSendChunkAmount ];
            int                 waitingSendRead         = 0;


 */

            // 除錯用
            Map<Player, PlayerView> isSendDebugList = null;  // 需要完成調適的玩家清單
            if (Value.backgroundDebugMode == 1 || Value.backgroundDebugMode == 2)
                isSendDebugList = new HashMap<>();



            // 確保循環在限制範圍內
            {
                if (playerViewsRead > 0) {
                    PlayerView          playerView;
                    Long                chunkKey;
                    for (int i = 0, isSend = 0; i < Value.tickReadChunkAmount && isSend < Value.tickReadChunkAmount; ++i) {
                        playerView = playerViews[(int) (Math.random() * playerViewsRead)];

                        // 檢查是否符合條件
                        if (Value.worldBlacklist.contains(playerView.world.getName()))              continue;   // 在黑名單內
                        if (playerView.totalRead >= Value.tickAssignEachPlayerMaxChunkAmount)       continue;   // 超過單個 tick 能發送的最大量
                        if (playerView.delayedSendTick > 0)                                         continue;   // 需要等待

                        chunkKey = playerView.chunkMapView.get();
                        if (chunkKey != null) {
                            try {

                                int x = ChunkMapView.getX(chunkKey);
                                int z = ChunkMapView.getZ(chunkKey);

                                /*
                                由於區塊記憶體消耗很兇
                                如果緩存的話會造成大量 GC
                                所以馬上使用 馬上發送
                                讓記憶體能在新生代快速的被清除
                                 */

                                ExtendChunkCache chunkCache = NMS.World(playerView.world).getChunkIfRegionFile(x, z, true, true, false, false, true, false, false, false, true, true);

                                if (chunkCache != null) {
                                    // 有區塊
                                    // 防透視礦物作弊
                                    // 替換全部指定材質
                                    for (Map.Entry<BlockData, BlockData[]> entry : Value.conversionMaterialListMap.entrySet()) {
                                        chunkCache.replaceAllMaterial(entry.getValue(), entry.getKey());
                                    }

                                    // 轉換為區塊
                                    Chunk chunk = chunkCache.asChunk(playerView.world);

                                    Packet.callServerMapChunkPacket(playerView.player, chunk, true);
                                    Packet.callServerLightUpdatePacket(playerView.player, chunk);

                                    // 除錯用
                                    if (isSendDebugList != null)
                                        isSendDebugList.put(playerView.player, playerView);
                                }

                                playerView.totalRead++;
                                isSend++;

                            } catch (Exception ex) {
                                if (Value.backgroundDebugMode == 1 || Value.backgroundDebugMode == 2)
                                    ex.printStackTrace();
                                playerView.chunkMapView.markWait(chunkKey);
                            }

                        } else {
                            // 壓力測試用
                            if (Value.stressTestMode == 1) {
                                int x;
                                int z;
                                int minX;
                                int minZ;
                                int maxX;
                                int maxZ;
                                for (long isSendChunk : playerView.chunkMapView.getIsSendChunkList()) {
                                    // 是否已經不再範圍內
                                    x = ChunkMapView.getX(isSendChunk);
                                    z = ChunkMapView.getZ(isSendChunk);
                                    minX = x - serverViewDistance;
                                    minZ = z - serverViewDistance;
                                    maxX = x + serverViewDistance;
                                    maxZ = z + serverViewDistance;
                                    if (x < minX || x > maxX || z < minZ || z > maxZ)
                                        continue;
                                    Packet.callServerUnloadChunkPacket(playerView.player, x, z);
                                }
                                playerView.chunkMapView.clear();
                                playerView.chunkMapView.setCenter(playerView.player.getLocation());
                                playerView.delayedSendTick = 1;
                            }
                        }
                    }
                }
            }



            long d = System.currentTimeMillis();
            //isRun = false;




            if (Value.backgroundDebugMode == 3) {
                // 除錯用
                System.out.println("a-b:" + (b - a) + " b-c:" + (c - b) + " c-d:" + (d - c));
            } else if (isSendDebugList != null) {
                // 除錯用
                Player      player;
                String      playerName;
                PlayerView  playerView;
                for (Map.Entry<Player, PlayerView> entry : isSendDebugList.entrySet()) {
                    player      = entry.getKey();
                    playerName  = player.getName();
                    playerView  = entry.getValue();

                    if (Value.backgroundDebugMode == 1) {

                        System.out.print("------------------------------------------------------- ");
                        System.out.print(playerName);
                        for (int i = playerName.length() ; i < 16 ; i++)
                            System.out.print(' ');
                        System.out.print(" -------------------------------------------------------");
                        System.out.println();

                        for (int i = 0 ; i < 63 ; i++)
                            ChunkMapView.debug(playerView.chunkMapView.getChunkMap()[i]);

                        System.out.print("--------------------------------------------------------------------------------------------------------------------------------");
                        System.out.println();

                    } else if (Value.backgroundDebugMode == 2) {
                        int     all     = playerView.chunkMapView.getAllAmount();
                        long[]  send    = playerView.chunkMapView.getIsSendChunkList();
                        int     wait    = (all - send.length);

                        System.out.println("player:" + playerName + " all:" + all + " wait:" + (wait < 0 ? 0 : wait) + " send:" + send.length);
                    }
                }
            }



        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }





    public void runChunk() {
        /*
        ChunkSend chunkSend;

        for (int i = 0 ; i < Value.tickSendChunkAmount ; ++i) {
            synchronized (chunkSendList) {
                if (chunkSendList.size() > 0) {
                    chunkSend = chunkSendList.remove(0);
                } else {
                    return;
                }
            }


            if (chunkSend != null && !chunkSend.playerView.waitingChangeWorld && chunkSend.playerView.delayedSendTick <= 0) {
                //Packet.callServerLightUpdatePacket(chunkSend.playerView.player, chunkSend.chunk);
            }
        }

         */
    }








    //private static final



    public static void debug(Player player, ChunkMapView chunkMapView) {
        player.sendMessage("----------------------------------------------------");
        for (long value : chunkMapView.getChunkMap()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 63 ; i >= 0 ; i--) {
                stringBuilder.append(value >> i & 1);
            }
            player.sendMessage(stringBuilder.toString());
        }
        player.sendMessage("----------------------------------------------------");
    }













    /**
     * 計算玩家最大可擁有的距離
     * @param player 玩家
     * @param extendViewDistance 擴展視野距離
     * @return 視野距離
     */
    public static int playerMaxViewDistance(Player player, int extendViewDistance) {

        int viewDistance = player.getClientViewDistance() + 1; // 取得客戶端視野距離
        if (viewDistance > extendViewDistance)
            viewDistance = extendViewDistance;
        if (viewDistance < 1)
            viewDistance = 1;


        // 檢查權限節點
        if (Value.computingPermissions) {
            for (int i = extendViewDistance; i > 0; i--) {
                if (viewDistance > i && player.hasPermission("extend_view_distance." + i)) {
                    viewDistance = i;
                    break;
                }
            }
        }

        return ++viewDistance;
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


    public static void playerRespawnEvent(Player player) {
        PlayerView  playerView              = playerPlayerViewHashMap.get(player);
        if (playerView != null) {
            playerView.delayedSendTick      = Value.delayedSendTick;
            playerView.waitingChangeWorld   = true;
            for (long isSendChunk : playerView.chunkMapView.getIsSendChunkList()) {
                Packet.callServerUnloadChunkPacket(playerView.player, ChunkMapView.getX(isSendChunk), ChunkMapView.getZ(isSendChunk));
            }
            playerView.chunkMapView.clear();
        }
    }

    public static void needDelayedSendTick(Player player, Location from, Location move) {
        // 傳送距離過遠, 則等待一段時間

        int         playerMaxViewDistance   = Loop.playerMaxViewDistance(player, Value.extendViewDistance);
        PlayerView  playerView              = playerPlayerViewHashMap.get(player);
        if (playerView != null) {

            if (from.getWorld().equals(move.getWorld()) && (Math.abs(ChunkMapView.blockToChunk(from.getX() - move.getX())) > playerMaxViewDistance || Math.abs(ChunkMapView.blockToChunk(from.getZ() - move.getZ())) > playerMaxViewDistance)) {
                playerView.delayedSendTick      = Value.delayedSendTick;
                playerView.waitingChangeWorld   = true;
                for (long isSendChunk : playerView.chunkMapView.getIsSendChunkList()) {
                    Packet.callServerUnloadChunkPacket(playerView.player, ChunkMapView.getX(isSendChunk), ChunkMapView.getZ(isSendChunk));
                }
                playerView.chunkMapView.clear();
            }
        }
    }












}
