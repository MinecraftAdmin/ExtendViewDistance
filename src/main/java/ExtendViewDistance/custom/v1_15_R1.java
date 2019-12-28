package ExtendViewDistance.custom;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class v1_15_R1 implements Extend {


    /** 取得玩家類別 */
    private EntityPlayer getNMSPlayer(Player player) {
        return ((CraftPlayer) player).getHandle();
    }
    /** 取得世界類別 */
    private WorldServer getNMSWorld(World world) {
        return ((CraftWorld) world).getHandle();
    }
    /** 取得世界類別 */
    private net.minecraft.server.v1_15_R1.Chunk getNMSChunk(Chunk chunk) {
        return ((CraftChunk) chunk).getHandle();
    }
    /**
     * 創建區塊
     */
    private net.minecraft.server.v1_15_R1.Chunk createNMSChunk(World world, ProtoChunk chunk) {
        return new net.minecraft.server.v1_15_R1.Chunk(getNMSWorld(world), chunk);
    }


    /** 發送封包 */
    private void playerSendPacket(Player player, Packet packet) {

        //synchronized (getNMSPlayer(player)) {
        synchronized (getNMSPlayer(player).playerConnection.networkManager) {

            NetworkManager              networkManager  = getNMSPlayer(player).playerConnection.networkManager;   // 玩家連線
            Channel                     channel         = networkManager.channel;                                 // 取得連線通道
            AttributeKey<EnumProtocol>  enumProtocols   = AttributeKey.valueOf("protocol");                       // 取得所有協議協定類型
            EnumProtocol                enumprotocol    = EnumProtocol.a(packet);
            EnumProtocol                enumprotocol1   = channel.attr(enumProtocols).get();


            synchronized (channel) {
                synchronized (networkManager) {

                    if (channel.isOpen()) {

                        if (channel.eventLoop().inEventLoop()) {
                            if (enumprotocol != enumprotocol1) {
                                networkManager.setProtocol(enumprotocol);
                            }

                            ChannelFuture channelfuture = channel.write(packet);
                            channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                        } else {
                            channel.eventLoop().execute(() -> {
                                if (enumprotocol != enumprotocol1) {
                                    networkManager.setProtocol(enumprotocol);
                                }

                                ChannelFuture channelfuture1 = channel.write(packet);
                                channelfuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                            });
                        }

                        List<Packet> extraPackets = packet.getExtraPackets();
                        if (extraPackets != null && !extraPackets.isEmpty()) {
                            Iterator var6 = extraPackets.iterator();

                            while (var6.hasNext()) {
                                Packet extraPacket = (Packet) var6.next();
                                this.playerSendPacket(player, extraPacket);
                            }
                        }
                    }
                }
            }
        }
        //}
    }




    /** 取得區塊 */
    public Chunk getChunk(World world, int x, int z) {

        synchronized (this.getNMSWorld(world)) {
            IChunkAccess chunk = this.getNMSWorld(world).getChunkProvider().getChunkAt(x, z, ChunkStatus.SURFACE, true);

            if (chunk == null) return null;

            if (chunk instanceof ChunkEmpty) {
                return ((net.minecraft.server.v1_15_R1.Chunk) chunk).bukkitChunk;

            } else if (chunk instanceof net.minecraft.server.v1_15_R1.Chunk) {
                return ((net.minecraft.server.v1_15_R1.Chunk) chunk).bukkitChunk;

            } else if (chunk instanceof ProtoChunkExtension) {
                return ((ProtoChunkExtension) chunk).u().bukkitChunk;

            }
            return null;
        }
    }




    /** 發送視野距離 */
    public synchronized void playerSendViewDistance(Player player, int distance) {
        playerSendPacket(player, new PacketPlayOutViewDistance(distance)); // 發送視野距離x
    }


    /** 發送區塊 */
    public void playerSendChunk(Player player, Chunk chunk) {
        playerSendChunk(player, getNMSChunk(chunk));
    }
    private void playerSendChunk(Player player, net.minecraft.server.v1_15_R1.Chunk chunk) {
        // 65535 + 1 = 65536 = 16 * 256 * 16
        playerSendPacket(player, new PacketPlayOutMapChunk(chunk, 65535, true)); // 發送區塊
    }


    /** 發送區塊卸除 */
    public void playerSendUnloadChunk(Player player, int x, int z) {
        playerSendPacket(player, new PacketPlayOutUnloadChunk(x, z)); // 發送區塊卸除
    }


    /** 發送光照更新 */
    public void playerSendChunkLightUpdate(Player player, Chunk chunk) {
        playerSendChunkLightUpdate(player, getNMSChunk(chunk));
    }
    private void playerSendChunkLightUpdate(Player player, net.minecraft.server.v1_15_R1.Chunk chunk) {
        LightEngine lightEngine = chunk.getWorld().getChunkProvider().getLightEngine();         // 取得光照引擎
        playerSendPacket(player, new PacketPlayOutLightUpdate(chunk.getPos(), lightEngine));    // 更新光照
    }
}
