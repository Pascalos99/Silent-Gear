package net.silentchaos512.gear.data.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.api.material.MaterialDisplay;
import net.silentchaos512.gear.api.parts.PartType;
import net.silentchaos512.gear.api.stats.IItemStat;
import net.silentchaos512.gear.api.stats.LazyItemStat;
import net.silentchaos512.gear.api.stats.StatInstance;
import net.silentchaos512.gear.api.stats.StatModifierMap;
import net.silentchaos512.gear.api.traits.ITraitCondition;
import net.silentchaos512.gear.api.traits.ITraitInstance;
import net.silentchaos512.gear.api.traits.TraitInstance;
import net.silentchaos512.gear.parts.PartTextureType;
import net.silentchaos512.utils.Color;

import javax.annotation.Nullable;
import java.util.*;

public class MaterialBuilder {
    final ResourceLocation id;
    private final int tier;
    private final Ingredient ingredient;
    private boolean visible = true;
    private Collection<String> gearBlacklist = new ArrayList<>();
    private final Collection<ICondition> loadConditions = new ArrayList<>();
    @Nullable private ResourceLocation parent;
    private ITextComponent name;
    @Nullable private ITextComponent namePrefix;

    private final Map<PartType, StatModifierMap> stats = new LinkedHashMap<>();
    private final Map<PartType, List<ITraitInstance>> traits = new LinkedHashMap<>();
    private final Map<String, MaterialDisplay> display = new LinkedHashMap<>();

    public MaterialBuilder(ResourceLocation id, int tier, ResourceLocation tag) {
        this(id, tier, Ingredient.fromTag(new ItemTags.Wrapper(tag)));
    }

    public MaterialBuilder(ResourceLocation id, int tier, Tag<Item> tag) {
        this(id, tier, Ingredient.fromTag(tag));
    }

    public MaterialBuilder(ResourceLocation id, int tier, IItemProvider... items) {
        this(id, tier, Ingredient.fromItems(items));
    }

    public MaterialBuilder(ResourceLocation id, int tier, Ingredient ingredient) {
        this.id = id;
        this.tier = tier;
        this.ingredient = ingredient;
        this.name = new TranslationTextComponent(String.format("material.%s.%s",
                this.id.getNamespace(),
                this.id.getPath().replace("/", ".")));
    }

    public MaterialBuilder loadConditionTagExists(ResourceLocation tagId) {
        return loadCondition(new NotCondition(new TagEmptyCondition(tagId)));
    }

    public MaterialBuilder loadCondition(ICondition condition) {
        this.loadConditions.add(condition);
        return this;
    }

    public MaterialBuilder parent(ResourceLocation parent) {
        this.parent = parent;
        return this;
    }

    public MaterialBuilder visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public MaterialBuilder blacklistGearType(GearType gearType) {
        return blacklistGearType(gearType.getName());
    }

    public MaterialBuilder blacklistGearType(String gearType) {
        this.gearBlacklist.add(gearType);
        return this;
    }

    public MaterialBuilder name(ITextComponent text) {
        this.name = text;
        return this;
    }

    public MaterialBuilder namePrefix(ITextComponent text) {
        this.namePrefix = text;
        return this;
    }

    public MaterialBuilder display(PartTextureType texture, int color) {
        if (this.stats.isEmpty()) {
            throw new IllegalStateException("Must build stats map first!");
        }
        this.stats.keySet().forEach(partType -> display(partType, texture, color));
        return this;
    }

    public MaterialBuilder display(PartType partType, PartTextureType texture, int color) {
        return display(partType, "all", texture, color, Color.VALUE_WHITE);
    }

    public MaterialBuilder display(PartType partType, PartTextureType texture, int color, int armorColor) {
        return display(partType, "all", texture, color, armorColor);
    }

    public MaterialBuilder display(PartType partType, String gearType, PartTextureType texture, int color) {
        return display(partType, gearType, texture, color, Color.VALUE_WHITE);
    }

    public MaterialBuilder display(PartType partType, String gearType, PartTextureType texture, int color, int armorColor) {
        MaterialDisplay materialDisplay = new MaterialDisplay(texture, color, armorColor);
        this.display.put(SilentGear.shortenId(partType.getName()) + "/" + gearType, materialDisplay);
        return this;
    }

    public MaterialBuilder noStats(PartType partType) {
        // Put an empty map for the part type, because the part type can only be supported if in the stats object
        stats.computeIfAbsent(partType, pt -> new StatModifierMap());
        return this;
    }

    public MaterialBuilder stat(PartType partType, IItemStat stat, float value) {
        return stat(partType, stat, value, StatInstance.Operation.AVG);
    }

    public MaterialBuilder stat(PartType partType, IItemStat stat, float value, StatInstance.Operation operation) {
        StatInstance mod = new StatInstance(value, operation);
        StatModifierMap map = stats.computeIfAbsent(partType, pt -> new StatModifierMap());
        map.put(stat, mod);
        return this;
    }

    public MaterialBuilder stat(PartType partType, ResourceLocation statId, float value) {
        return stat(partType, statId, value, StatInstance.Operation.AVG);
    }

    public MaterialBuilder stat(PartType partType, ResourceLocation statId, float value, StatInstance.Operation operation) {
        return stat(partType, LazyItemStat.of(statId), value, operation);
    }

    public MaterialBuilder trait(PartType partType, ResourceLocation traitId, int level, ITraitCondition... conditions) {
        ITraitInstance inst = TraitInstance.lazy(traitId, level, conditions);
        List<ITraitInstance> list = traits.computeIfAbsent(partType, pt -> new ArrayList<>());
        list.add(inst);
        return this;
    }

    public JsonObject serialize() {
        JsonObject json = new JsonObject();

        if (this.parent != null) {
            json.addProperty("parent", this.parent.toString());
        }

        if (!this.loadConditions.isEmpty()) {
            JsonArray array = new JsonArray();
            for (ICondition condition : this.loadConditions) {
                array.add(CraftingHelper.serialize(condition));
            }
            json.add("conditions", array);
        }

        JsonObject availability = new JsonObject();
        if (this.tier >= 0) {
            availability.addProperty("tier", this.tier);
            availability.addProperty("visible", this.visible);
            JsonArray array = new JsonArray();
            for (String gearType : this.gearBlacklist) {
                array.add(gearType);
            }
            availability.add("gear_blacklist", array);
        }
        if (!availability.entrySet().isEmpty()) {
            json.add("availability", availability);
        }

        JsonObject craftingItems = new JsonObject();
        if (this.ingredient.getMatchingStacks().length > 0) {
            craftingItems.add("main", this.ingredient.serialize());
        }
        json.add("crafting_items", craftingItems);

        if (this.name != null) {
            json.add("name", ITextComponent.Serializer.toJsonTree(this.name));
        }

        if (this.namePrefix != null) {
            json.add("name_prefix", ITextComponent.Serializer.toJsonTree(this.namePrefix));
        }

        if (!this.display.isEmpty()) {
            JsonObject displayObj = new JsonObject();
            this.display.forEach((key, materialDisplay) -> displayObj.add(key, materialDisplay.serialize()));
            json.add("display", displayObj);
        }

        if (!this.stats.isEmpty()) {
            JsonObject statsJson = new JsonObject();
            this.stats.forEach((partType, map) -> statsJson.add(SilentGear.shortenId(partType.getName()), map.serialize()));
            json.add("stats", statsJson);
        }

        if (!this.traits.isEmpty()) {
            JsonObject traitsJson = new JsonObject();
            this.traits.forEach((partType, list) -> {
                JsonArray array = new JsonArray();
                list.forEach(t -> array.add(t.serialize()));
                traitsJson.add(SilentGear.shortenId(partType.getName()), array);
            });
            json.add("traits", traitsJson);
        }

        return json;
    }
}