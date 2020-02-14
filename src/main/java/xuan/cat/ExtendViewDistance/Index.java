package xuan.cat.ExtendViewDistance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Index extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable() {

        plugin = this;

        // 初始化配置文件
        saveDefaultConfig();
        // 取得配置文件
        FileConfiguration configuration = getConfig();

        Value.extendViewDistance        = configuration.getInt(         "extend-view-distance",             32);
        if (Value.extendViewDistance > 32) Value.extendViewDistance = 32;

        Value.tickSendChunkAmount       = configuration.getInt(         "player-tick-send-chunk-amount",    30);
        Value.serverFieldViewCorrection = configuration.getInt(         "server-field-view-correction",     2);
        Value.worldBlacklist            = configuration.getStringList(  "world-blacklist");
        Value.delayedSendTick           = configuration.getInt(         "delayed-send-tick",                100);
        Value.backgroundDebugMode       = configuration.getInt(         "background-debug-mode",            0);

        ConfigurationSection preventXray = configuration.getConfigurationSection(   "prevent-xray");
        if (preventXray != null) {

            if (preventXray.getBoolean("enable", false)) {
                // 啟用

                // 讀取轉換清單
                ConfigurationSection conversion = preventXray.getConfigurationSection("conversion");

                if (conversion != null) {
                    for (String to : conversion.getKeys(false)) {
                        Material toMaterial = Material.getMaterial(to.toUpperCase());

                        if (toMaterial == null) {
                            getLogger().warning("Can't find this material: " + to); // 找不到這種材料
                            continue;
                        }

                        List<Material> materialList = new ArrayList<>();

                        for (String target : conversion.getStringList(to)) {
                            Material targetMaterial = Material.getMaterial(target.toUpperCase());

                            if (targetMaterial == null) {
                                getLogger().warning("Can't find this material: " + target); // 找不到這種材料
                                continue;
                            }

                            materialList.add(targetMaterial);
                        }


                        Material[] materials = new Material[materialList.size()];
                        for (int i = 0 ; i < materials.length ; ++i )
                            materials[i] = materialList.get(i);

                        Value.conversionMaterialListMap.put(toMaterial, materials);
                    }
                }
            }
        }
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
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        singleThreadExecutor.execute(() -> {

            try {
                while (true) {

                    // 計算耗時
                    long timeStart = System.currentTimeMillis();



                    loop.run();



                    long timeEnd = System.currentTimeMillis();
                    long sleep = 50 - (timeEnd - timeStart);
                    if (sleep > 0) {
                        Thread.sleep(sleep);   // 每 50 毫秒運行一次
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        /*
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {

        }, 0, 1); // 顯示更遠的區塊給玩家

         */




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
