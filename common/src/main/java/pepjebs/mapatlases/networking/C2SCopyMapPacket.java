package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.IMapCollection;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;

public class C2SCopyMapPacket implements Message {

    private final int mapId;

    public C2SCopyMapPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readInt();
    }

    public C2SCopyMapPacket(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeInt(mapId);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (!(context.getSender() instanceof ServerPlayer player)) return;

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;

        IMapCollection maps = MapAtlasItem.getMaps(atlas, player.level());
        if (!maps.hasId(mapId)) return;

        MapDataHolder holder = MapDataHolder.findFromId(player.level(), mapId);
        if (holder == null) return;

        ItemStack item = holder.createExistingMapItem();
        if (!player.getInventory().add(item)) {
            player.drop(item, false);
        }
    }
}
