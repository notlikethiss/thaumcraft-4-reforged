package thaumcraft.aspect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;

public final class Aspect implements StringRepresentable {
    private static final int BUILTIN_COUNT = 48;
    private static final Map<String, Aspect> BY_TAG = new ConcurrentHashMap<>();
    private static final List<Aspect> VALUES = new ArrayList<>();
    private static volatile Aspect[] ordered = new Aspect[0];

    public static final Aspect AIR = primal("aer", 0xFFFF7E, "e", 1);
    public static final Aspect EARTH = primal("terra", 0x56C000, "2", 1);
    public static final Aspect FIRE = primal("ignis", 0xFF5A01, "c", 1);
    public static final Aspect WATER = primal("aqua", 0x3CD4FC, "3", 1);
    public static final Aspect ORDER = primal("ordo", 0xD5D4EC, "7", 1);
    public static final Aspect ENTROPY = primal("perditio", 0x404040, "8", 771);
    public static final Aspect VOID = compound("vacuos", 0x888888, AIR, ENTROPY, 771);
    public static final Aspect LIGHT = compound("lux", 0xFFF663, AIR, FIRE, 1);
    public static final Aspect WEATHER = compound("tempestas", 0xFFFFFF, AIR, WATER, 1);
    public static final Aspect MOTION = compound("motus", 0xCDCCF4, AIR, ORDER, 1);
    public static final Aspect COLD = compound("gelum", 0xE1FFFF, FIRE, ENTROPY, 1);
    public static final Aspect CRYSTAL = compound("vitreus", 0x80FFFF, EARTH, ORDER, 1);
    public static final Aspect LIFE = compound("victus", 0xDE0005, WATER, EARTH, 1);
    public static final Aspect POISON = compound("venenum", 0x89F000, WATER, ENTROPY, 1);
    public static final Aspect ENERGY = compound("potentia", 0xC0FFFF, ORDER, FIRE, 1);
    public static final Aspect EXCHANGE = compound("permutatio", 0x578357, ENTROPY, ORDER, 1);
    public static final Aspect METAL = compound("metallum", 0xB5B5CD, EARTH, CRYSTAL, 1);
    public static final Aspect DEATH = compound("mortuus", 0x887788, LIFE, ENTROPY, 1);
    public static final Aspect FLIGHT = compound("volatus", 0xE7E7D7, AIR, MOTION, 1);
    public static final Aspect DARKNESS = compound("tenebrae", 0x222222, VOID, LIGHT, 1);
    public static final Aspect SOUL = compound("spiritus", 0xEBEBFB, LIFE, DEATH, 1);
    public static final Aspect HEAL = compound("sano", 0xFF2F34, LIFE, ORDER, 1);
    public static final Aspect TRAVEL = compound("iter", 0xE0585B, MOTION, EARTH, 1);
    public static final Aspect ELDRITCH = compound("alienis", 0x805080, VOID, DARKNESS, 1);
    public static final Aspect MAGIC = compound("praecantatio", 0x9700C0, VOID, ENERGY, 1);
    public static final Aspect AURA = compound("auram", 0xFFC0FF, MAGIC, AIR, 1);
    public static final Aspect TAINT = compound("vitium", 0x800080, MAGIC, ENTROPY, 1);
    public static final Aspect SLIME = compound("limus", 0x01F800, LIFE, WATER, 1);
    public static final Aspect PLANT = compound("herba", 0x01AC00, LIFE, EARTH, 1);
    public static final Aspect TREE = compound("arbor", 0x876531, AIR, PLANT, 1);
    public static final Aspect BEAST = compound("bestia", 0x9F6409, MOTION, LIFE, 1);
    public static final Aspect FLESH = compound("corpus", 0xEE478D, DEATH, BEAST, 1);
    public static final Aspect UNDEAD = compound("exanimis", 0x3A4000, MOTION, DEATH, 1);
    public static final Aspect MIND = compound("cognitio", 0xFFC2B3, FIRE, SOUL, 1);
    public static final Aspect SENSES = compound("sensus", 0x0FD9FF, AIR, SOUL, 1);
    public static final Aspect MAN = compound("humanus", 0xFFD7C0, BEAST, MIND, 1);
    public static final Aspect CROP = compound("messis", 0xE1B371, PLANT, MAN, 1);
    public static final Aspect MINE = compound("perfodio", 0xDCD2D8, MAN, EARTH, 1);
    public static final Aspect TOOL = compound("instrumentum", 0x4040EE, MAN, ORDER, 1);
    public static final Aspect HARVEST = compound("meto", 0xEEAD82, CROP, TOOL, 1);
    public static final Aspect WEAPON = compound("telum", 0xC05050, TOOL, FIRE, 1);
    public static final Aspect ARMOR = compound("tutamen", 0x00C0C0, TOOL, EARTH, 1);
    public static final Aspect HUNGER = compound("fames", 0x9A0305, LIFE, VOID, 1);
    public static final Aspect GREED = compound("lucrum", 0xE6BE44, MAN, HUNGER, 1);
    public static final Aspect CRAFT = compound("fabrico", 0x809D80, MAN, TOOL, 1);
    public static final Aspect CLOTH = compound("pannus", 0xEAEAC2, TOOL, BEAST, 1);
    public static final Aspect MECHANISM = compound("machina", 0x8080A0, MOTION, TOOL, 1);
    public static final Aspect TRAP = compound("vinculum", 0x9A8080, MOTION, ENTROPY, 1);

    public static final Codec<Aspect> CODEC = Codec.STRING.comapFlatMap(Aspect::decodeByTag, Aspect::getTag);
    public static final StreamCodec<ByteBuf, Aspect> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Aspect::byTagOrThrow, Aspect::getTag);

    private final String tag;
    private final int color;
    private final @Nullable String chatcolor;
    private final Aspect @Nullable [] components;
    private final int blend;
    private final Identifier image;
    private int ordinal = -1;

    public Aspect(String tag, int color, Aspect[] components, Identifier image, int blend) {
        this(tag, color, null, components, blend, image, true);
    }

    private Aspect(String tag, int color, @Nullable String chatcolor, Aspect @Nullable [] components, int blend, @Nullable Identifier image, boolean addOn) {
        if (tag == null || tag.isBlank() || tag.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Aspect tag must be a non-blank, unqualified string");
        }
        if (addOn) {
            if (components == null || components.length != 2 || components[0] == null || components[1] == null) {
                throw new IllegalArgumentException("Add-on aspects must have exactly two components");
            }
            if (image == null) {
                throw new IllegalArgumentException("Add-on aspects must have an image");
            }
        }
        this.tag = tag;
        this.color = color;
        this.chatcolor = chatcolor;
        this.components = components == null ? null : components.clone();
        this.blend = blend;
        this.image = image == null ? Thaumcraft.id("textures/aspects/" + tag + ".png") : image;
        register(this);
    }

    private static synchronized void register(Aspect aspect) {
        if (BY_TAG.putIfAbsent(aspect.tag, aspect) != null) {
            throw new IllegalArgumentException(aspect.tag + " already registered!");
        }
        VALUES.add(aspect);
        reorder();
    }

    private static synchronized void reorder() {
        int builtinsEnd = Math.min(BUILTIN_COUNT, VALUES.size());
        List<Aspect> snapshot = new ArrayList<>(VALUES.subList(0, builtinsEnd));
        List<Aspect> addOns = new ArrayList<>(VALUES.subList(builtinsEnd, VALUES.size()));
        addOns.sort(Comparator.comparing(Aspect::getTag));
        snapshot.addAll(addOns);
        for (int index = 0; index < snapshot.size(); index++) {
            snapshot.get(index).ordinal = index;
        }
        ordered = snapshot.toArray(new Aspect[0]);
    }

    private static Aspect primal(String tag, int color, @Nullable String chatcolor, int blend) {
        return new Aspect(tag, color, chatcolor, null, blend, null, false);
    }

    private static Aspect compound(String tag, int color, Aspect first, Aspect second, int blend) {
        return new Aspect(tag, color, null, new Aspect[]{first, second}, blend, null, false);
    }

    public static Aspect[] values() {
        return ordered.clone();
    }

    public int ordinal() {
        return ordinal;
    }

    public String getTag() {
        return tag;
    }

    public int getColor() {
        return color;
    }

    public @Nullable String getChatcolor() {
        return chatcolor;
    }

    public Aspect @Nullable [] getComponents() {
        return components;
    }

    public int getBlend() {
        return blend;
    }

    public Identifier getImage() {
        return image;
    }

    public boolean isPrimal() {
        return components == null || components.length != 2;
    }

    public String getName() {
        return Character.toUpperCase(tag.charAt(0)) + tag.substring(1);
    }

    public Component getDescription() {
        return Component.translatable("tc.aspect." + tag);
    }

    @Override
    public String getSerializedName() {
        return tag;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Aspect aspect && aspect.tag.equals(tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public String toString() {
        return tag;
    }

    public static @Nullable Aspect getAspect(String tag) {
        return BY_TAG.get(tag);
    }

    private static DataResult<Aspect> decodeByTag(String tag) {
        Aspect aspect = getAspect(tag);
        return aspect == null
            ? DataResult.error(() -> unknownAspectMessage(tag))
            : DataResult.success(aspect);
    }

    private static Aspect byTagOrThrow(String tag) {
        Aspect aspect = getAspect(tag);
        if (aspect == null) {
            throw new IllegalArgumentException(unknownAspectMessage(tag));
        }
        return aspect;
    }

    private static String unknownAspectMessage(String tag) {
        return "Unknown aspect " + tag + "; the add-on that provides it is missing on this side";
    }

    public static List<Aspect> getPrimalAspects() {
        List<Aspect> primals = new ArrayList<>();
        for (Aspect aspect : values()) {
            if (aspect.isPrimal()) {
                primals.add(aspect);
            }
        }
        return primals;
    }

    public static List<Aspect> getCompoundAspects() {
        List<Aspect> compounds = new ArrayList<>();
        for (Aspect aspect : values()) {
            if (!aspect.isPrimal()) {
                compounds.add(aspect);
            }
        }
        return compounds;
    }

    public static @Nullable Aspect combine(Aspect first, Aspect second) {
        for (Aspect aspect : values()) {
            if (aspect.components != null
                && (aspect.components[0] == first && aspect.components[1] == second
                    || aspect.components[0] == second && aspect.components[1] == first)) {
                return aspect;
            }
        }
        return null;
    }
}
