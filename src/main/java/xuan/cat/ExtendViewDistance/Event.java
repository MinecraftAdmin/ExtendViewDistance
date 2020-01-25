package xuan.cat.ExtendViewDistance;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.WorldInitEvent;
import xuan.cat.api.event.packet.PacketEvent;
import xuan.cat.api.event.packet.ServerUnloadChunkPacketEvent;

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


}
