package xuan.cat.ExtendViewDistance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Index extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable() {

        plugin = this;

        // 初始化配置文件
        saveDefaultConfig();
        // 取得配置文件
        FileConfiguration configuration = getConfig();
        Value.extendViewDistance        = configuration.getInt(     "ExtendViewDistance",       32);
        if (Value.extendViewDistance > 127) Value.extendViewDistance = 127;
        Value.tickSendChunkAmount       = configuration.getInt(     "TickSendChunkAmount",      20);
        //Value.sendChunkAsync            = configuration.getBoolean( "SendChunkAsync",           true);
        //Value.playerTickSendChunkAmount = configuration.getInt(     "PlayerTickSendChunkAmount", 1);
        //Value.playerOutChunkSendUnload  = configuration.getBoolean( "PlayerOutChunkSendUnload",  true);
        //Value.tickChunkExamine          = configuration.getInt(     "TickChunkExamine",          40);
        //Value.tickChunkSend             = configuration.getInt(     "TickChunkSend",             20);
        //Value.tickIsLag                 = configuration.getInt(     "TickIsLag",                 50);
        /*
# 擴展視野距離本身線程大部分都是異步
# 但如果伺服器主線程tick耗時高過此值
# 則停止運行擴展視野距離,直到低於此值才繼續運行
TickIsLag: 50
         */


        // 開始迴圈線程
        Loop loop = new Loop();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            loop.run();
        }, 0, 1); // 顯示更遠的區塊給玩家


        // 事件
        Bukkit.getPluginManager().registerEvents(new Event(), this);


        // 初始化世界
        //for (World world : Bukkit.getWorlds()) {
        //    Value.extend.setSpigotViewDistance(world, Value.extendViewDistance);
        //}


        getLogger().info(ChatColor.GREEN + "Plugin loading completed!"); // 插件加载完成!
    }

    @Override
    public void onDisable() {

        getLogger().info(ChatColor.RED + "Plugin stop!"); // 插件停止!
    }
}