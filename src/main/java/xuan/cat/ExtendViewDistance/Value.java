package xuan.cat.ExtendViewDistance;

import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Value {
    public static int                           extendViewDistance;
    public static int                           tickReadChunkAmount;
    public static int                           tickAssignEachPlayerMaxChunkAmount;
    public static int                           serverFieldViewCorrection;
    public static int                           delayedSendTick;
    public static int                           backgroundDebugMode;
    public static int                           stressTestMode;
    public static Map<BlockData, BlockData[]>   conversionMaterialListMap               = new HashMap<>();
    public static List<String>                  worldBlacklist                          = new ArrayList<>();
    //public static boolean   sendChunkAsync;
    //public static int       playerTickSendChunkAmount;
    //public static boolean   playerOutChunkSendUnload;
    //public static int       tickChunkExamine;
    //public static int       tickChunkSend;
    //public static int       tickIsLag;
}
