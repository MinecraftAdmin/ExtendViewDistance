package xuan.cat.ExtendViewDistance;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.WorldInitEvent;
import xuan.cat.XuanCatAPI.api.event.packet.*;

public class Event implements Listener {



    /**
     * @param event 在世界初始化時調用
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void event(WorldInitEvent event) {
        //Value.extend.setSpigotViewDistance(event.getWorld(), Value.extendViewDistance);
    }


    /**
     * @param event 區塊卸除封包
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void event(ServerUnloadChunkPacketEvent event) {
        if (event.getCause() != PacketEvent.Cause.PLUGIN)
            event.setCancelled(true);
    }

/*
    @EventHandler(priority = EventPriority.NORMAL)
    public void event(ServerSpawnPositionPacketEvent event) {
        // ServerSpawnPositionPacketEvent 的時候, 世界已經寫入到玩家中
        if (event.getCause() != PacketEvent.Cause.PLUGIN) {
            Player  player  = event.getPlayer();
            World   world   = player.getWorld();
            System.out.println("B " + world.getName());
            if (Loop.setWaitingChangeWorld(player, world)) {
                // 已經切換世界
            }
        }
    }

 */
    @EventHandler(priority = EventPriority.NORMAL)
    public void event(ServerMapChunkPacketEvent event) {
        this.event((PacketEvent) event);
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void event(ServerLightUpdatePacketEvent event) {
        this.event((PacketEvent) event);
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void event(ServerViewDistancePacketEvent event) {
        if (event.getCause() != PacketEvent.Cause.PLUGIN)
            event.setCancelled(true);
    }


    /**
     * 封包事件, 確保玩家不在切換世界中
     * @param event 封包事件
     */
    private void event(PacketEvent event) {
        if (event.getCause() != PacketEvent.Cause.PLUGIN)
            Loop.addWaitingChangeWorldPacket(event.getPlayer(), event.getTrigger());
    }



    /**
     * @param event 當玩家切換到另一個世界時調用
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void event(PlayerChangedWorldEvent event) {

/*
        Player player = event.getPlayer();
        Chunk chunk = player.getChunk();
        int minX = chunk.getX() - Value.extendViewDistance - 1;
        int minZ = chunk.getZ() - Value.extendViewDistance - 1;
        int maxX = chunk.getX() + Value.extendViewDistance + 1;
        int maxZ = chunk.getZ() + Value.extendViewDistance + 1;
        // 視野距離內全部區塊卸載
        for (int x = minX ; x <= maxX ; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                Value.extend.playerSendUnloadChunk(player, x, z);
            }
        }

 */
    }

/*
    @EventHandler(priority = EventPriority.NORMAL)
    public void event(PlayerLoginEvent event) {

        Bukkit.getScheduler().runTaskLater(Index.plugin, () -> {
            WorldCreator worldCreator = new WorldCreator("TEST");
            World world = Bukkit.createWorld(worldCreator);

            if (world != null) {
                event.getPlayer().teleportAsync(world.getSpawnLocation());
            }
        }, 200);
    }

 */


}
