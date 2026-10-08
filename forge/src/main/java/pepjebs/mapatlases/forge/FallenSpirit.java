package pepjebs.mapatlases.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;

/** LoM Fallen Spirit: preserve eligible enchanted items through player death. */
public final class FallenSpirit {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MapAtlasesMod.MOD_ID);
    private static final EnchantmentCategory PROTECTED_GEAR =
            EnchantmentCategory.create("lom_fallen_spirit_gear", FallenSpirit::isSupportedItem);

    public static boolean isSupportedItem(Item item) {
        if (item instanceof MapAtlasItem || item instanceof ArmorItem
                || item instanceof SwordItem || item instanceof AxeItem
                || item instanceof BowItem || item instanceof CrossbowItem
                || item instanceof TridentItem) return true;
        // Identify Iron's Spellbooks by its registry namespace and spellbook item names.
        var id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && "irons_spellbooks".equals(id.getNamespace())
                && (id.getPath().contains("spellbook") || id.getPath().contains("spell_book"));
    }
    public static final RegistryObject<Enchantment> ENCHANTMENT =
            ENCHANTMENTS.register("fallen_spirit", () -> new AtlasEnchantment());

    private FallenSpirit() {}

    public static boolean protects(ItemStack stack) {
        return isSupportedItem(stack.getItem())
                && EnchantmentHelper.getItemEnchantmentLevel(ENCHANTMENT.get(), stack) > 0;
    }

    public static class AtlasEnchantment extends Enchantment {
        public AtlasEnchantment() {
            super(Rarity.RARE, PROTECTED_GEAR, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
        }

        @Override public boolean canEnchant(ItemStack stack) { return isSupportedItem(stack.getItem()); }
        @Override public int getMinLevel() { return 1; }
        @Override public int getMaxLevel() { return 1; }
        @Override public int getMinCost(int level) { return 1; }
        @Override public int getMaxCost(int level) { return 50; }
        @Override public boolean isTradeable() { return false; }
        @Override public boolean isDiscoverable() { return true; }
        @Override public boolean isAllowedOnBooks() { return false; }
    }
}
