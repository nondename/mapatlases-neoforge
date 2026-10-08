package pepjebs.mapatlases.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;

/** LoM-only atlas enchantment: survive death, without a post-death debuff. */
public final class FallenSpirit {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MapAtlasesMod.MOD_ID);
    private static final EnchantmentCategory ATLAS =
            EnchantmentCategory.create("lom_atlas", item -> item instanceof MapAtlasItem);
    public static final RegistryObject<Enchantment> ENCHANTMENT =
            ENCHANTMENTS.register("fallen_spirit", () -> new AtlasEnchantment());

    private FallenSpirit() {}

    public static boolean protects(ItemStack stack) {
        return stack.getItem() instanceof MapAtlasItem
                && EnchantmentHelper.getItemEnchantmentLevel(ENCHANTMENT.get(), stack) > 0;
    }

    public static class AtlasEnchantment extends Enchantment {
        public AtlasEnchantment() {
            super(Rarity.RARE, ATLAS, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
        }

        @Override public int getMinLevel() { return 1; }
        @Override public int getMaxLevel() { return 1; }
        @Override public int getMinCost(int level) { return 20; }
        @Override public int getMaxCost(int level) { return 20; }
        @Override public boolean isTradeable() { return false; }
        @Override public boolean isDiscoverable() { return false; }
        @Override public boolean isAllowedOnBooks() { return false; }
    }
}
