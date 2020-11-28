package net.silentchaos512.gear.gear.trait;

import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.traits.ITraitSerializer;
import net.silentchaos512.gear.block.PhantomLight;
import net.silentchaos512.gear.util.GearHelper;
import net.silentchaos512.lib.item.FakeItemUseContext;
import net.silentchaos512.lib.util.NameUtils;

import java.util.Collection;
import java.util.Objects;

public class BlockPlacerTrait extends SimpleTrait {
    private static final ResourceLocation SERIALIZER_ID = SilentGear.getId("block_placer");
    public static final ITraitSerializer<BlockPlacerTrait> SERIALIZER = new Serializer<>(SERIALIZER_ID, BlockPlacerTrait::new, BlockPlacerTrait::readJson, BlockPlacerTrait::read, BlockPlacerTrait::write);

    private Block block;
    private int damageOnUse;
    private int cooldown;
    private SoundEvent sound = SoundEvents.ENTITY_ITEM_PICKUP;
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;

    public BlockPlacerTrait(ResourceLocation name) {
        super(name, SERIALIZER);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context, int traitLevel) {
        ItemStack stack = context.getItem();

        World world = context.getWorld();
        BlockPos pos = context.getPos();
        if (!world.isRemote && (damageOnUse < 1 || stack.getDamage() < stack.getMaxDamage() - damageOnUse - 1)) {
            // Try place block, damage tool if successful
            ItemStack fakeBlockStack = new ItemStack(block);
            ActionResultType result = fakeBlockStack.onItemUse(new FakeItemUseContext(context, fakeBlockStack));
            if (result.isSuccessOrConsume()) {
                if (damageOnUse > 0) {
                    GearHelper.attemptDamage(stack, damageOnUse, context.getPlayer(), context.getHand());
                }
                if (sound != null) {
                    float pitch = (float) (soundPitch * (1 + 0.05 * SilentGear.RANDOM.nextGaussian()));
                    world.playSound(null, pos, sound, SoundCategory.BLOCKS, soundVolume, pitch);
                }
                if (this.cooldown > 0 && context.getPlayer() != null) {
                    context.getPlayer().getCooldownTracker().setCooldown(stack.getItem(), this.cooldown);
                }
            }
            return result;
        }

        for (int i = 0; i < 5; i++) {
            PhantomLight.spawnParticle(world, pos.offset(context.getFace()), SilentGear.RANDOM);
        }

        return ActionResultType.SUCCESS;
    }

    private static void readJson(BlockPlacerTrait trait, JsonObject json) {
        trait.block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(JSONUtils.getString(json, "block")));
        trait.damageOnUse = JSONUtils.getInt(json, "damage_on_use");
        trait.cooldown = JSONUtils.getInt(json, "cooldown", 0);
        trait.sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(JSONUtils.getString(json, "sound")));
        trait.soundVolume = JSONUtils.getFloat(json, "sound_volume");
        trait.soundPitch = JSONUtils.getFloat(json, "sound_pitch");
    }

    private static void read(BlockPlacerTrait trait, PacketBuffer buffer) {
        trait.block = ForgeRegistries.BLOCKS.getValue(buffer.readResourceLocation());
        trait.damageOnUse = buffer.readVarInt();
        trait.cooldown = buffer.readVarInt();
        trait.sound = ForgeRegistries.SOUND_EVENTS.getValue(buffer.readResourceLocation());
        trait.soundVolume = buffer.readFloat();
        trait.soundPitch = buffer.readFloat();
    }

    private static void write(BlockPlacerTrait trait, PacketBuffer buffer) {
        buffer.writeResourceLocation(Objects.requireNonNull(trait.block.getRegistryName()));
        buffer.writeVarInt(trait.damageOnUse);
        buffer.writeVarInt(trait.cooldown);
        buffer.writeResourceLocation(Objects.requireNonNull(trait.sound.getRegistryName()));
        buffer.writeFloat(trait.soundVolume);
        buffer.writeFloat(trait.soundPitch);
    }

    @Override
    public Collection<String> getExtraWikiLines() {
        Collection<String> ret = super.getExtraWikiLines();
        ret.add("  - Places: " + NameUtils.from(block));
        ret.add("  - Durability Cost: " + damageOnUse);
        if (cooldown > 0) {
            ret.add("  - Cooldown: " + cooldown);
        }
        return ret;
    }
}
