package ExtendViewDistance;

import ExtendViewDistance.custom.Extend;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Index extends JavaPlugin {

    @Override
    public void onEnable() {

        // 取得當前版本能使用的擴展
        Value.extend = Extend.getExtend();
        if (Value.extend == null) {
            getLogger().warning(ChatColor.RED + "Error! Unsupported MC version:" + Bukkit.getBukkitVersion()); // 錯誤,不支持的MC版本
            Bukkit.getPluginManager().disablePlugin(this, false); // 停用插件
            return;
        }


        // 初始化配置文件
        saveDefaultConfig();
        // 取得配置文件
        FileConfiguration configuration = getConfig();
        Value.extendViewDistance        = configuration.getInt(     "ExtendViewDistance",        32);
        if (Value.extendViewDistance > 127) Value.extendViewDistance = 127;
        Value.tickSendChunkAmount       = configuration.getInt(     "TickSendChunkAmount",       20);
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
        Bukkit.getScheduler().runTaskTimerAsynchronously(Value.plugin, () -> {
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
