package xuan.cat.ExtendViewDistance;

import org.bukkit.Location;

/**
 * 表示區塊視野
 */
public class ChunkMapView {

    /*
    每位玩家都有一個 long 陣列
    最高 63 * 63 (因為求奇數)
        0 表示等待中
        1 表示已發送區塊
    63 / 2 = 31 所以實際上最遠只能擴充 31 個視野距離

    每個 long 的最後一位數用於其他資料標記

    long[].length = 64

                    chunkMap
                    6   6          5          4            3          2          1          0
                    3 2109876 54321098 76543210 98765432 1 0987654 32109876 54321098 76543210  位元位移

                      33          2          1         0 0 0         1          2          33
                      1098765 43210987 65432109 87654321 0 1234567 89012345 67890123 45678901  區塊離中心點多遠

                   |-|-------|--------|--------|--------|- -------|--------|--------|--------|
                   | |                                   *                                   | 表示 列 中心點
                   |*|                                                                       | 不使用
                   |-|------- -------- -------- -------- - ------- -------- -------- --------|
          long[ 0] |0|0000000 00000000 00000000 00000000 0 0000000 00000000 00000000 00000000|
          ...      | |                                 ...                                   |
          long[31] | |                                 ...                                   | 表示 行 中心點
          ...      | |                                 ...                                   |
          long[62] |0|0000000 00000000 00000000 00000000 0 0000000 00000000 00000000 00000000|
                   |-|-----------------------------------------------------------------------|



                    chunkKey 區塊鑰匙編號
                    X                                   Z
                   |-------- -------- -------- --------|-------- -------- -------- --------|
          long[63] |00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000|
                   |-----------------------------------------------------------------------|
     */
    public long[]   chunkMap            = new long[64];
    public int      extendViewDistance  = 32;
    public int      serverViewDistance  = 1;








    public long[] move(Location location) {
        return move(ChunkMapView.blockToChunk(location.getX()), ChunkMapView.blockToChunk(location.getZ()));
    }
    /**
     * 移動到區塊位置 (中心點)
     * @param moveX 區塊座標X
     * @param moveZ 區塊座標Z
     * @return 如果有區塊被移除, 則會集中回傳在這
     */
    public long[] move(int moveX, int moveZ) {

        /*
        先對 chunkMap 進行座標位移
        再把伺服器視野距離的範圍標記為以加載
         */



        // 上一個紀錄的區塊位置 (中心點)
        int oldX = this.getCenterX();
        int oldZ = this.getCenterZ();



        // 移除的區塊清單
        long[]  removeChunkKeyList      = new long[3969];   // 63 * 63
        int     removeChunkKeyListRead  = 0;




        // 將那些已經不再範圍內的區塊, 增加到緩存忠
        int effectiveMinX = moveX - extendViewDistance;
        int effectiveMinZ = moveZ - extendViewDistance;
        int effectiveMaxX = moveX + extendViewDistance;
        int effectiveMaxZ = moveZ + extendViewDistance;

        for (int pointerX = 0 ; pointerX < 63 ; ++pointerX) {
            for (int pointerZ = 0 ; pointerZ < 63 ; ++pointerZ) {
                int x = oldX + pointerX - 31;
                int z = oldZ + pointerZ - 31;

                // 是否已經不再範圍內
                if (x <= effectiveMinX || x >= effectiveMaxX || z <= effectiveMinZ || z >= effectiveMaxZ)
                    if (this.isSend(pointerX, pointerZ))
                        removeChunkKeyList[ removeChunkKeyListRead++ ] = getChunkKey(x, z);
            }
        }







        /*
           -      +
           X
        +Z |-------|
           | Chunk |
           | Map   |
        -  |-------|

        當座標發生移動
        x:0    -> x:1
        000000    000000
        011110    111100
        011110    111100
        011110    111100
        011110    111100
        000000    000000

        z:0    -> z:1
        000000    000000
        011110    000000
        011110    011110
        011110    011110
        011110    011110
        000000    011110
         */
        int offsetX = oldX - moveX;
        int offsetZ = oldZ - moveZ;
        // 座標X 發生改動
        if (offsetX != 0) {
            for (int i = 0, s = 63; i < s; i++)
                this.chunkMap[ i ] = offsetX > 0 ? this.chunkMap[i] >> offsetX : this.chunkMap[i] << Math.abs(offsetX);
        }
        // 座標Z 發生改動
        if (offsetZ != 0) {
            long[] newChunkMap = new long[64];
            for (int i = 0, s = 63; i < s; i++) {
                int z = i - offsetZ;
                if (z >= 0 && z < 63)
                    newChunkMap[ z ] = this.chunkMap[ i ];
            }
            this.chunkMap = newChunkMap;
        }
        /*
         先把伺服器本身的視野距離 範圍內 填充為 1

         因為 31 為擴展極限, 所以從 31 開始減

         假設伺服器視野距離為 4
         0 1111111 11111111 11111111 11111111 1 1111111 11111111 11111111 11111111

         >> 31 - 4
         >> 31 - 4
           27                            27
           ------- -------- -------- ----
                                         ---- - ------- -------- -------
         0 0000000 00000000 00000000 00000000 0 0000000 00000000 00000001 11111111

         << 31 - 4
                                                    27
                                                    --- -------- -------- --------
         0 0000000 00000000 00000000 00001111 1 1111000 00000000 00000000 00000000
                                         ---- - ----
                                         4    1 4

         位移紀錄 >> 27 >> 27 << 27
         */
        int moveChunkInsideServerByte = (31 - serverViewDistance);
        long chunkInsideServer = 0b0111111111111111111111111111111111111111111111111111111111111111L >> moveChunkInsideServerByte >> moveChunkInsideServerByte << moveChunkInsideServerByte;

        // 覆蓋掉當前的地圖組
        for (int i = moveChunkInsideServerByte++, s = moveChunkInsideServerByte + (serverViewDistance << 1) ; i < s ; i++)
            this.chunkMap[ i ] = this.chunkMap[ i ] | chunkInsideServer;




        if (offsetX != 0 || offsetZ != 0) {
            // 如果座標有發生改動, 更新目前儲存的座標
            this.chunkMap[63] = getChunkKey(moveX, moveZ);

            // 將沒有用到的地方標記為 0 (最左側)
            if (offsetX < 0) {
                for (int i = 0, s = 63; i < s; i++)
                    this.chunkMap[ i ] = this.chunkMap[ i ] & 0b0111111111111111111111111111111111111111111111111111111111111111L;
            }
        }




        // 複製
        long[] cloneRemoveChunkKeyList = new long[ removeChunkKeyListRead ];
        System.arraycopy(removeChunkKeyList, 0, cloneRemoveChunkKeyList, 0, removeChunkKeyListRead);
        return cloneRemoveChunkKeyList;
    }




    /**
     * 取得下一個應該要處裡的區塊
     * @return chunkKey, 若沒有需要處裡的區塊, 則回傳 null
     */
    public Long get() {

        // 區塊位置 (中心點)
        int centerX = this.getCenterX();
        int centerZ = this.getCenterZ();

        
        /*
        尋找過程
        會從中心慢慢往外找
        
        順時針, 從最上方開始
         -----      -----      -----      -----      -----      -----      -----      -----      -----      -----      -----

                                           1          11         111        111        111        111        111        111
           +    ->    +    ->   1+    ->   1+    ->   1+    ->   1+    ->   1+1   ->   1+1   ->   1+1   ->   1+1   ->   1+1
                     1          1          1          1          1          1          1 1        111        111       1111
                                                                                                            1          1
         -----      -----      -----      -----      -----      -----      -----      -----      -----      -----      -----





        算公式
         單個邊長
        1 = 1 + (1 - 1)
        3 = 2 + (2 - 1)
        5 = 3 + (3 - 1)
        7 = 4 + (4 - 1)

         總邊長 (不重複步數)
        0  = 1 * 4 - 4
        8  = 3 * 4 - 4
        16 = 5 * 4 - 4
        24 = 7 * 4 - 4

         edgeStepCount = 每移動?次 換方向 總要要換4次方向
        0  / 4 = 0
        8  / 4 = 2
        16 / 4 = 4
        24 / 4 = 6

        得出的公式
        每 距離+1 所需移動的次數+2

        distance = 1    //
        1               // 由於不可為 1
        + 1             // 中心點掠過


        distance = 2
         3
        |-|
        | | 8
        |-|


        distance = 3
          5
        |---|
        |   |
        |   | 16
        |   |
        |---|


        distance = 4
           7
        |-----|
        |     |
        |     |
        |     | 24
        |     |
        |     |
        |-----|

         */

        int edgeStepCount = 0;  // 每個邊, 移動幾次換方向
        for (int distance = 0 ; distance < 32 && distance < extendViewDistance ; distance++ ) {


            // 總共有 4 次方向
            int readX = distance;
            int readZ = distance;
            int pointerX = 31 + distance;
            int pointerZ = 31 + distance;
            // Z--
            for (int i = 0 ; i < edgeStepCount ; ++i) {

                if (this.isWait(pointerX, pointerZ)) {
                    this.markSend(pointerX, pointerZ);
                    return getChunkKey(centerX - readX, centerZ - readZ);
                }

                pointerZ--;
                readZ--;
            }
            // X--
            for (int i = 0 ; i < edgeStepCount ; ++i) {

                if (this.isWait(pointerX, pointerZ)) {
                    this.markSend(pointerX, pointerZ);
                    return getChunkKey(centerX - readX, centerZ - readZ);
                }

                pointerX--;
                readX--;
            }
            // Z++
            for (int i = 0 ; i < edgeStepCount ; ++i) {

                if (this.isWait(pointerX, pointerZ)) {
                    this.markSend(pointerX, pointerZ);
                    return getChunkKey(centerX - readX, centerZ - readZ);
                }

                pointerZ++;
                readZ++;
            }
            // X++
            for (int i = 0 ; i < edgeStepCount ; ++i) {

                if (this.isWait(pointerX, pointerZ)) {
                    this.markSend(pointerX, pointerZ);
                    return getChunkKey(centerX - readX, centerZ - readZ);
                }

                pointerX++;
                readX++;
            }

            // 下一次循環
            edgeStepCount += 2;
        }


        return null;
    }




    private boolean isWait(int pointerX, int pointerZ) {
        return !isSend(pointerX, pointerZ);
    }
    private boolean isSend(int pointerX, int pointerZ) {
        return ((chunkMap[ pointerZ ] >> pointerX) & 0b0000000000000000000000000000000000000000000000000000000000000001L) == 0b0000000000000000000000000000000000000000000000000000000000000001L;
    }
    public void markWait(int pointerX, int pointerZ) {
        chunkMap[pointerZ] = chunkMap[pointerZ] & (0b1111111111111111111111111111111111111111111111111111111111111110L << pointerX);
    }
    public void markSend(int pointerX, int pointerZ) {
        chunkMap[pointerZ] = chunkMap[pointerZ] | (0b0000000000000000000000000000000000000000000000000000000000000001L << pointerX);
    }




    public void clear() {
        this.chunkMap = new long[64];
    }




    public long[] getChunkMap() {
        return chunkMap;
    }
    public long[] getIsSendChunkList() {
        // 上一個紀錄的區塊位置 (中心點)
        int centerX = this.getCenterX();
        int centerZ = this.getCenterZ();

        long[]  isSendChunks        = new long[3969];   // 63 * 63
        int     isSendChunksRead    = 0;

        // 將那些已經不再範圍內的區塊, 增加到緩存忠
        for (int pointerX = 0 ; pointerX < 63 ; ++pointerX) {
            for (int pointerZ = 0 ; pointerZ < 63 ; ++pointerZ) {
                int x = centerX + pointerX - 31;
                int z = centerZ + pointerZ - 31;

                    if (this.isSend(pointerX, pointerZ))
                        isSendChunks[ isSendChunksRead++ ] = getChunkKey(x, z);
            }
        }

        // 複製
        long[] cloneIsSendChunks = new long[ isSendChunksRead ];
        System.arraycopy(isSendChunks, 0, cloneIsSendChunks, 0, isSendChunksRead);
        return cloneIsSendChunks;
    }





    public void setCenter(Location location) {
        setCenter(getChunkKey((int) location.getX(), (int) location.getZ()));
    }
    public void setCenter(long chunkKey) {
        this.chunkMap[63] = chunkKey;
    }





    public int getCenterX() {
        return (int) (chunkMap[63] >> 32);
    }
    public int getCenterZ() {
        return (int) (chunkMap[63]);
    }
    public static int getX(long chunkKey) {
        return (int) (chunkKey >> 32);
    }
    public static int getZ(long chunkKey) {
        return (int) (chunkKey);
    }



    public static long getChunkKey(int x, int z) {
        return ((long) x << 32) & 0b1111111111111111111111111111111100000000000000000000000000000000L | z & 0b0000000000000000000000000000000011111111111111111111111111111111L;
    }









    public static int blockToChunk(double blockLocation) {
        return blockToChunk((int) blockLocation);
    }
    public static int blockToChunk(int blockLocation) {
        return blockLocation >> 4;
    }
















    public static void debug(long[] values) {
        System.out.println("----------------------------------------------------");
        for (long value : values)
            debug(value);
        System.out.println("----------------------------------------------------");
    }
    public static void debug(int value) {
        for (int i = 31 ; i >= 0 ; i--) {
            System.out.print(value >> i & 1);
        }
        System.out.println();
    }
    public static void debug(long value) {
        for (int i = 63 ; i >= 0 ; i--) {
            System.out.print(value >> i & 1);
        }
        System.out.println();
    }
}
