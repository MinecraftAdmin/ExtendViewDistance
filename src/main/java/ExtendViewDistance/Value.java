package ExtendViewDistance;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class Value {
    public static String    pluginNmae                = "ExtendViewDistance";                            // 插件名稱
    public static Plugin    plugin                    = Bukkit.getPluginManager().getPlugin(pluginNmae); // 取得插件類別
    public static int       extendViewDistance;
    public static int       tickSendChunkAmount;
    //public static boolean   sendChunkAsync;
    //public static int       playerTickSendChunkAmount;
    //public static boolean   playerOutChunkSendUnload;
    //public static int       tickChunkExamine;
    //public static int       tickChunkSend;
    //public static int       tickIsLag;
}
