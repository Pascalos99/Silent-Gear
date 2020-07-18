package net.silentchaos512.gear.api.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.parts.PartType;
import net.silentchaos512.gear.parts.PartTextureType;
import net.silentchaos512.utils.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MaterialDisplay implements IMaterialDisplay {
    public static final MaterialDisplay DEFAULT = new MaterialDisplay();

    private final List<MaterialLayer> layers;
    private int armorColor;
    private PartTextureType oldTextureType = PartTextureType.ABSENT;

    public MaterialDisplay() {
        this(PartType.MAIN, PartTextureType.ABSENT, Color.VALUE_WHITE, Color.VALUE_WHITE);
    }

    public MaterialDisplay(PartType partType, PartTextureType texture, int color, int armorColor) {
        this(armorColor, texture.getLayers(partType).stream()
                .map(tex -> {
                    int c = tex.equals(SilentGear.getId("_highlight")) ? Color.VALUE_WHITE : color;
                    return new MaterialLayer(tex, c);
                })
                .toArray(MaterialLayer[]::new));
        this.oldTextureType = texture;
    }

    public MaterialDisplay(int armorColor, MaterialLayer... layers) {
        this.armorColor = armorColor;
        this.layers = new ArrayList<>(Arrays.asList(layers));
    }

    @Override
    public List<MaterialLayer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    @Deprecated
    @Override
    public PartTextureType getTexture() {
        return oldTextureType;
    }

    @Deprecated
    @Override
    public int getColor() {
        if (!layers.isEmpty()) {
            return layers.get(0).getColor();
        }
        return Color.VALUE_WHITE;
    }

    @Override
    public int getArmorColor() {
        return armorColor;
    }

    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("armor_color", Color.format(this.armorColor & 0xFFFFFF));

        JsonArray jsonLayers = new JsonArray();
        this.layers.forEach(layer -> jsonLayers.add(layer.serialize()));
        json.add("layers", jsonLayers);

        return json;
    }

    public static MaterialDisplay deserialize(JsonObject json, IMaterialDisplay defaultProps) {
        MaterialDisplay props = new MaterialDisplay();

        props.armorColor = Color.from(json, "armor_color", Color.VALUE_WHITE).getColor();

        JsonArray jsonLayers = JSONUtils.getJsonArray(json, "layers", null);
        if (jsonLayers != null) {
            for (JsonElement je : jsonLayers) {
                props.layers.add(MaterialLayer.deserialize(je));
            }
        }

        return props;
    }

    private static int loadColor(JsonObject json, int defaultValue, int fallback, String... keys) {
        for (String key : keys) {
            if (json.has(key)) {
                return Color.from(json, key, defaultValue).getColor();
            }
        }
        return fallback;
    }

    public static MaterialDisplay read(PacketBuffer buffer) {
        MaterialDisplay props = new MaterialDisplay();
        props.armorColor = buffer.readVarInt();

        int layerCount = buffer.readByte();
        for (int i = 0; i < layerCount; ++i) {
            props.layers.add(MaterialLayer.read(buffer));
        }

        return props;
    }

    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(this.armorColor);

        buffer.writeByte(this.layers.size());
        this.layers.forEach(layer -> layer.write(buffer));
    }

    @Override
    public String toString() {
        return "MaterialDisplay{" +
                ", armorColor=" + Color.format(armorColor) +
                ", layers=" + layers +
                '}';
    }
}
