package xuan.cat.ExtendViewDistance;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Value {
    public static int                       extendViewDistance          = 32;
    public static int                       tickSendChunkAmount;
    public static int                       serverFieldViewCorrection   = 2;
    public static int                       delayedSendTick             = 100;
    public static int                       backgroundDebugMode         = 0;
    public static Map<Material, Material[]> conversionMaterialListMap   = new HashMap<>();
    public static List<String>              worldBlacklist              = new ArrayList<>();
    //public static boolean   sendChunkAsync;
    //public static int       playerTickSendChunkAmount;
    //public static boolean   playerOutChunkSendUnload;
    //public static int       tickChunkExamine;
    //public static int       tickChunkSend;
    //public static int       tickIsLag;
}
