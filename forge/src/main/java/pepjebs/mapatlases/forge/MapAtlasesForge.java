package pepjebs.mapatlases.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.forge.MapAtlasesClientImpl;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerEvents;
import pepjebs.mapatlases.map_collection.forge.CapStuff;

@Mod(MapAtlasesMod.MOD_ID)
public class MapAtlasesForge {

    public MapAtlasesForge() {
        MapAtlasesMod.init();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(CapStuff::register);
        FallenSpirit.ENCHANTMENTS.register(bus);

        MinecraftForge.EVENT_BUS.register(this);

        if (PlatHelper.getPhysicalSide().isClient()) {
            MapAtlasesClientImpl.init();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDimensionUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel)
            MapAtlasesServerEvents.onDimensionUnload();
    }

    @SubscribeEvent
    public void mapAtlasesPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side == LogicalSide.CLIENT) {
            MapAtlasesClient.cachePlayerState(event.player);
        } else {
            MapAtlasesServerEvents.onPlayerTick(event.player);
            if (event.player instanceof ServerPlayer sp) restoreSavedAtlases(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MapAtlasesServerEvents.onPlayerJoin(sp);
        }
    }


    // The normal third enchanting-table option is capped at 30; for this
    // exclusive atlas enchantment we offer level 20 once six shelves are present.
    @SubscribeEvent
    public void onAtlasEnchantmentLevel(EnchantmentLevelSetEvent event) {
        if (event.getItem().getItem() instanceof pepjebs.mapatlases.item.MapAtlasItem
                && event.getEnchantRow() == 2 && event.getPower() >= 6) {
            event.setEnchantLevel(20);
        } else if (event.getItem().getItem() instanceof pepjebs.mapatlases.item.MapAtlasItem) {
            event.setEnchantLevel(0);
        }
    }

    // Extract protected atlases before a grave mod collects inventory contents.
    // Store full ItemStack NBT, preserving maps, annotations and enchantments.
    private static final String SAVED_ATLASES = "lom_fallen_spirit_saved";
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) return;
        var saved = new net.minecraft.nbt.ListTag();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (FallenSpirit.protects(stack)) {
                CompoundTag entry = new CompoundTag();
                entry.put("Stack", stack.save(new CompoundTag()));
                saved.add(entry);
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        // Curios slots are stored in a separate capability, not Player.Inventory.
        // Remove the protected atlas before GraveStone/Curios death-drop handlers run.
        if (ModList.get().isLoaded("curios")) {
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(curios -> {
                curios.getCurios().forEach((slotType, handler) -> {
                    var stacks = handler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (FallenSpirit.protects(stack)) {
                            CompoundTag entry = new CompoundTag();
                            entry.put("Stack", stack.save(new CompoundTag()));
                            entry.putString("CuriosType", slotType);
                            entry.putInt("CuriosSlot", i);
                            saved.add(entry);
                            stacks.setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }
                });
            });
        }
        if (!saved.isEmpty()) player.getPersistentData().put(SAVED_ATLASES, saved);
    }

    // Transfer the saved stacks to the new player; restore on the next server tick
    // so Curios has finished cloning/reinitializing its slot capabilities.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRespawnClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag oldData = event.getOriginal().getPersistentData();
        if (oldData.contains(SAVED_ATLASES, net.minecraft.nbt.Tag.TAG_LIST)) {
            player.getPersistentData().put(SAVED_ATLASES, oldData.getList(SAVED_ATLASES,
                    net.minecraft.nbt.Tag.TAG_COMPOUND).copy());
            oldData.remove(SAVED_ATLASES);
        }
    }

    private void restoreSavedAtlases(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(SAVED_ATLASES, net.minecraft.nbt.Tag.TAG_LIST)) return;
        var saved = data.getList(SAVED_ATLASES, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag entry = saved.getCompound(i);
            // Entries written before this feature stored ItemStack directly.
            ItemStack stack = ItemStack.of(entry.contains("Stack", net.minecraft.nbt.Tag.TAG_COMPOUND)
                    ? entry.getCompound("Stack") : entry);
            if (stack.isEmpty()) continue;
            boolean restored = false;
            if (entry.contains("CuriosType", net.minecraft.nbt.Tag.TAG_STRING)
                    && ModList.get().isLoaded("curios")) {
                restored = restoreCuriosSlot(player, entry, stack);
            }
            if (!restored && !player.getInventory().add(stack)) player.drop(stack, false);
        }
        data.remove(SAVED_ATLASES);
    }

    private boolean restoreCuriosSlot(ServerPlayer player, CompoundTag entry, ItemStack stack) {
        return CuriosApi.getCuriosHelper().getCuriosHandler(player).map(curios -> {
            var handler = curios.getCurios().get(entry.getString("CuriosType"));
            if (handler == null) return false;
            var stacks = handler.getStacks();
            int slot = entry.getInt("CuriosSlot");
            if (slot < 0 || slot >= stacks.getSlots() || !stacks.getStackInSlot(slot).isEmpty()) return false;
            stacks.setStackInSlot(slot, stack);
            return true;
        }).orElse(false);
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.Key event) {
        if (event.getAction() == InputConstants.PRESS) {
            MapAtlasesClientEvents.onKeyPressed(event.getKey(), event.getScanCode());
        }
    }
}
