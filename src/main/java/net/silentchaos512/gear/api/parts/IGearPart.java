package net.silentchaos512.gear.api.parts;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.api.stats.ItemStat;
import net.silentchaos512.gear.api.stats.StatInstance;
import net.silentchaos512.gear.api.traits.ITrait;
import net.silentchaos512.gear.parts.PartData;
import net.silentchaos512.gear.parts.RepairContext;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IGearPart {
    ResourceLocation getId();

    int getTier();

    PartType getType();

    IPartPosition getPartPosition();

    IPartMaterial getMaterials();

    IPartSerializer<?> getSerializer();

    default Collection<StatInstance> getStatModifiers(ItemStat stat, PartData part) {
        return getStatModifiers(ItemStack.EMPTY, stat, part);
    }

    Collection<StatInstance> getStatModifiers(ItemStack gear, ItemStat stat, PartData part);

    default Map<ITrait, Integer> getTraits(PartData part) {
        return getTraits(ItemStack.EMPTY, part);
    }

    Map<ITrait, Integer> getTraits(ItemStack gear, PartData part);

    StatInstance.Operation getDefaultStatOperation(ItemStat stat);

    float getRepairAmount(RepairContext context);

    default float computeStatValue(ItemStat stat) {
        return computeStatValue(stat, PartData.of(this));
    }

    default float computeStatValue(ItemStat stat, PartData part) {
        return stat.compute(0, getStatModifiers(stat, part));
    }

    /**
     * Get the chance the part will be lost when salvaging. Returning zero will ensure the part is
     * returned, regardless of how damaged the gear is.
     *
     * @param gear           The gear item
     * @param part           The part
     * @param normalLossRate The default loss rate, which is based on config settings and how
     *                       damaged the item is
     * @return Chance of losing the part when salvaging (defaults to {@code normalLossRate})
     */
    default double getSalvageLossRate(ItemStack gear, PartData part, double normalLossRate) {
        return normalLossRate;
    }

    default boolean isCraftingAllowed(@Nullable GearType gearType) {
        return true;
    }

    IPartDisplay getDisplayProperties(PartData part, ItemStack gear, int animationFrame);

    @Nullable
    ResourceLocation getTexture(PartData part, ItemStack gear, GearType gearClass, IPartPosition position, int animationFrame);

    @Nullable
    ResourceLocation getBrokenTexture(PartData part, ItemStack gear, GearType gearClass, IPartPosition position);

    int getColor(PartData part, ItemStack gear, int animationFrame);

    ITextComponent getDisplayName(@Nullable PartData part, ItemStack gear);

    // May be removed or changed?
    @Deprecated
    String getModelIndex(PartData part, int animationFrame);

    @OnlyIn(Dist.CLIENT)
    void addInformation(PartData part, ItemStack gear, List<ITextComponent> tooltip, ITooltipFlag flag);
}
