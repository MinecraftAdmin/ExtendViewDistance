package xuan.cat.ExtendViewDistance;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import xuan.cat.XuanCatAPI.NMS;
import xuan.cat.XuanCatAPI.Packet;
import xuan.cat.XuanCatAPI.api.event.packet.ServerMapChunkPacketEvent;
import xuan.cat.XuanCatAPI.api.nms.world.ExtendChunkCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChunkManager {


    private ScheduledExecutorService scheduleAtFixedRate = null;


    private final int tickCanHandleQuantity;
    private final int numberOfChunkIO;
    private final int numberOfChunkXRAY;
    private final int numberOfChunkConvertPacket;
    private final int numberOfChunkChunkSen;


    private final AsyncChunkIO[]            asyncChunkIOs;
    private final AsyncChunkXRAY[]          asyncChunkXRAYs;
    private final AsyncChunkConvertPacket[] asyncChunkConvertPackets;
    private final AsyncChunkSend[]          asyncChunkSends;


    /**
     *
     * @param tickCanHandleQuantity         每個 tick 能處理多少請求
     * @param numberOfChunkIO               區塊IO執行續數量
     * @param numberOfChunkXRAY             區塊防止礦物透視執行續數量
     * @param numberOfChunkConvertPacket    區塊轉換為封包執行續數量
     * @param numberOfChunkChunkSen         區塊顯示給玩家執行續數量
     */
    public ChunkManager(int tickCanHandleQuantity, int numberOfChunkIO, int numberOfChunkXRAY, int numberOfChunkConvertPacket, int numberOfChunkChunkSen) {
        this.tickCanHandleQuantity      = tickCanHandleQuantity;
        this.numberOfChunkIO            = numberOfChunkIO;
        this.numberOfChunkXRAY          = numberOfChunkXRAY;
        this.numberOfChunkConvertPacket = numberOfChunkConvertPacket;
        this.numberOfChunkChunkSen      = numberOfChunkChunkSen;

        this.asyncChunkIOs              = new AsyncChunkIO              [numberOfChunkIO];
        this.asyncChunkXRAYs            = new AsyncChunkXRAY            [numberOfChunkXRAY];
        this.asyncChunkConvertPackets   = new AsyncChunkConvertPacket   [numberOfChunkConvertPacket];
        this.asyncChunkSends            = new AsyncChunkSend            [numberOfChunkChunkSen];

        for (int i = 0 ; i < numberOfChunkIO            ; i++)
            this.asyncChunkIOs[ i ]             = new AsyncChunkIO(             this, tickCanHandleQuantity / numberOfChunkIO);
        for (int i = 0 ; i < numberOfChunkXRAY          ; i++)
            this.asyncChunkXRAYs[ i ]           = new AsyncChunkXRAY(           this, tickCanHandleQuantity / numberOfChunkXRAY);
        for (int i = 0 ; i < numberOfChunkConvertPacket ; i++)
            this.asyncChunkConvertPackets[ i ]  = new AsyncChunkConvertPacket(  this, tickCanHandleQuantity / numberOfChunkConvertPacket);
        for (int i = 0 ; i < numberOfChunkChunkSen      ; i++)
            this.asyncChunkSends[ i ]           = new AsyncChunkSend(           this, tickCanHandleQuantity / numberOfChunkChunkSen);
    }





    /**
     * @param waiting 區塊請求
     * @return 是否加入成功
     */
    public boolean addChunkIO(Waiting waiting) {
        for (AsyncChunkIO asyncChunkIO : asyncChunkIOs)
            if (asyncChunkIO.add(waiting))
                return true;

        return false;
    }
    /**
     * @param waiting 區塊請求
     * @return 是否加入成功
     */
    public boolean addChunkXRAY(Waiting waiting) {
        for (AsyncChunkXRAY asyncChunkXRAY : asyncChunkXRAYs)
            if (asyncChunkXRAY.add(waiting))
                return true;

        return false;
    }
    /**
     * @param waiting 區塊請求
     * @return 是否加入成功
     */
    public boolean addChunkConvertPacket(Waiting waiting) {
        for (AsyncChunkConvertPacket asyncChunkConvertPacket : asyncChunkConvertPackets)
            if (asyncChunkConvertPacket.add(waiting))
                return true;

        return false;
    }
    /**
     * @param waiting 區塊請求
     * @return 是否加入成功
     */
    public boolean addChunkSend(Waiting waiting) {
        for (AsyncChunkSend asyncChunkSend : asyncChunkSends)
            if (asyncChunkSend.add(waiting))
                return true;

        return false;
    }


    public boolean enoughChunkIO() {
        for (AsyncChunkIO asyncChunkIO : asyncChunkIOs)
            if (asyncChunkIO.enough())
                return true;

        return false;
    }
    public boolean enoughChunkXRAY() {
        for (AsyncChunkXRAY asyncChunkXRAY : asyncChunkXRAYs)
            if (asyncChunkXRAY.enough())
                return true;

        return false;
    }
    public boolean enoughChunkConvertPacket() {
        for (AsyncChunkConvertPacket asyncChunkConvertPacket : asyncChunkConvertPackets)
            if (asyncChunkConvertPacket.enough())
                return true;

        return false;
    }
    public boolean enoughChunkSend() {
        for (AsyncChunkSend asyncChunkSend : asyncChunkSends)
            if (asyncChunkSend.enough())
                return true;

        return false;
    }



    /**
     *
     */
    public void start() {
        if (this.scheduleAtFixedRate != null) {
            this.scheduleAtFixedRate = Executors.newScheduledThreadPool(6);


            this.scheduleAtFixedRate.scheduleAtFixedRate(() -> {
                for (AsyncChunkSend asyncChunkSend : asyncChunkSends)
                    asyncChunkSend.tick();
            }, 0, 50, TimeUnit.MILLISECONDS);

            this.scheduleAtFixedRate.scheduleAtFixedRate(() -> {
                for (AsyncChunkConvertPacket asyncChunkConvertPacket : asyncChunkConvertPackets)
                    asyncChunkConvertPacket.tick();

            }, 0, 50, TimeUnit.MILLISECONDS);
            this.scheduleAtFixedRate.scheduleAtFixedRate(() -> {
                for (AsyncChunkXRAY asyncChunkXRAY : asyncChunkXRAYs)
                    asyncChunkXRAY.tick();

            }, 0, 50, TimeUnit.MILLISECONDS);
            this.scheduleAtFixedRate.scheduleAtFixedRate(() -> {
                for (AsyncChunkIO asyncChunkIO : asyncChunkIOs)
                    asyncChunkIO.tick();
            }, 0, 50, TimeUnit.MILLISECONDS);




            //waitingChunkIO = new ChunkIO[];
        }
    }
    public void stop() {
        this.scheduleAtFixedRate.shutdown();
        this.scheduleAtFixedRate = null;
    }










    /**
     * 表示區塊請求
     */
    public static class Waiting {
        public int                          x;
        public int                          z;
        public Player                       player      = null;
        public World                        world       = null;
        public ExtendChunkCache             chunkCache  = null;
        public Chunk                        chunk       = null;
        public ServerMapChunkPacketEvent    chunkPacket = null;
    }







    /***
     * 表示異步區塊任務
     */
    public static abstract class AsyncChunk {
        protected final     List<Waiting>   wantings;           // 緩存陣列
        //protected           int             used        = 0;    // 已使用的空間
        protected final     int             size;               // 可使用的空間
        protected volatile  boolean         isRun;
        protected final     ChunkManager    chunkManager;

        public AsyncChunk(ChunkManager chunkManager, int cacheAmount) {
            this.size           = cacheAmount;
            this.chunkManager   = chunkManager;
            this.wantings       = new ArrayList<>(cacheAmount);
            this.isRun          = false;
        }

        /**
         * 新增需要處理的區塊
         * @param waiting 請求
         * @return 是否加入成功
         */
        public boolean add(Waiting waiting) {
            synchronized (wantings) {
                if (this.wantings.size() < this.size) {
                    // 還有空間
                    this.wantings.add(waiting);
                    return true;
                } else {
                    // 庫容已滿
                    return false;
                }
            }
        }

        /**
         * @return 是否還有空位
         */
        public boolean enough() {
            return this.wantings.size() < this.size;
        }

        public abstract void tick();
        public abstract boolean process(Waiting waiting);
    }








    /**
     * 讀取區塊
     */
    public static class AsyncChunkIO extends AsyncChunk {
        public AsyncChunkIO(ChunkManager chunkManager, int cacheAmount) {
            super(chunkManager, cacheAmount);
        }

        public void tick() {
            if (this.isRun) return;
            this.isRun = true;

            for (; this.wantings.size() > 0 && this.chunkManager.enoughChunkXRAY() ; ) {
                Waiting waiting = this.wantings.remove(0);
                if (process(waiting)) {
                    // 執行成功
                    if (!this.chunkManager.addChunkXRAY(waiting)) {
                        // 所有緩存已滿
                        this.wantings.add(waiting);
                        return;
                    }
                }
            }

            this.isRun = false;
        }

        public synchronized boolean process(Waiting waiting) {
            try {
                // 從文件取得區塊緩存
                waiting.chunkCache = NMS.World(waiting.world).getChunkCache(waiting.x, waiting.z);
                return waiting.chunkCache != null;

            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }



    /**
     * 礦物代換
     */
    public static class AsyncChunkXRAY extends AsyncChunk {
        public AsyncChunkXRAY(ChunkManager chunkManager, int cacheAmount) {
            super(chunkManager, cacheAmount);
        }

        public void tick() {
            if (this.isRun) return;
            this.isRun = true;

            for (; this.wantings.size() > 0 && this.chunkManager.enoughChunkConvertPacket() ; ) {
                Waiting waiting = this.wantings.remove(0);
                if (process(waiting)) {
                    // 執行成功
                    if (!this.chunkManager.addChunkConvertPacket(waiting)) {
                        // 所有緩存已滿
                        this.wantings.add(waiting);
                        return;
                    }
                }
            }

            this.isRun = false;
        }

        public synchronized boolean process(Waiting waiting) {
            try {
                // 防透視礦物作弊
                // 替換全部指定材質
                for (Map.Entry<BlockData, BlockData[]> entry : Value.conversionMaterialListMap.entrySet()) {
                    waiting.chunkCache.replaceAllMaterial(entry.getValue(), entry.getKey());
                }
                waiting.chunk = waiting.chunkCache.asChunk(waiting.world);
                return waiting.chunk != null;

            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }



    /**
     * 區塊轉換成封包
     */
    public static class AsyncChunkConvertPacket extends AsyncChunk {
        public AsyncChunkConvertPacket(ChunkManager chunkManager, int cacheAmount) {
            super(chunkManager, cacheAmount);
        }

        public void tick() {
            if (this.isRun) return;
            this.isRun = true;

            for (; this.wantings.size() > 0 && this.chunkManager.enoughChunkSend() ; ) {
                Waiting waiting = this.wantings.remove(0);
                if (process(waiting)) {
                    // 執行成功
                    if (!this.chunkManager.addChunkSend(waiting)) {
                        // 所有緩存已滿
                        this.wantings.add(waiting);
                        return;
                    }
                }
            }

            this.isRun = false;
        }

        public synchronized boolean process(Waiting waiting) {
            try {
                // 轉換成封包
                waiting.chunkPacket = Packet.createServerMapChunkPacket(waiting.player, waiting.chunk, true);
                return waiting.chunkPacket != null;

            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }



    /**
     * 顯示區塊
     */
    public static class AsyncChunkSend extends AsyncChunk {
        public AsyncChunkSend(ChunkManager chunkManager, int cacheAmount) {
            super(chunkManager, cacheAmount);
        }

        public void tick() {
            if (this.isRun) return;
            this.isRun = true;

            for (; this.wantings.size() > 0 && this.chunkManager.enoughChunkXRAY() ; ) {
                Waiting waiting = this.wantings.remove(0);
                process(waiting);
                // 結束
                waiting.chunkPacket = null;
                waiting.chunk       = null;
                waiting.chunkCache  = null;
                waiting.world       = null;
                waiting.player      = null;
            }

            this.isRun = false;
        }

        public synchronized boolean process(Waiting waiting) {
            try {
                // 轉換成封包
                waiting.chunkPacket.getTrigger().trigger();
                return true;

            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }









}
