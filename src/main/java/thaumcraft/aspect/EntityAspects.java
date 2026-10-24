package thaumcraft.aspect;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EntityAspects(AspectList aspects, List<Condition> conditions) {
    public EntityAspects {
        if (aspects == null) {
            throw new IllegalArgumentException("Entity aspect list must not be null");
        }
        if (conditions == null) {
            throw new IllegalArgumentException("Entity aspect conditions must not be null");
        }

        List<Condition> canonical = new ArrayList<>(conditions);
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Condition condition : canonical) {
            if (condition == null) {
                throw new IllegalArgumentException("Entity aspect conditions must not contain null");
            }
            if (!names.add(condition.name())) {
                throw new IllegalArgumentException("Duplicate entity aspect condition: " + condition.name());
            }
        }
        canonical.sort(java.util.Comparator.comparing(Condition::name));
        conditions = List.copyOf(canonical);
    }

    public static final StreamCodec<ByteBuf, EntityAspects> STREAM_CODEC = StreamCodec.composite(
        AspectList.STREAM_CODEC,
        EntityAspects::aspects,
        Condition.STREAM_CODEC.apply(ByteBufCodecs.list()),
        EntityAspects::conditions,
        EntityAspects::new
    );

    public static EntityAspects of(AspectList aspects, Object... pairs) {
        if (pairs == null || pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Entity aspect conditions must be name/value pairs");
        }

        List<Condition> conditions = new ArrayList<>();
        for (int index = 0; index < pairs.length; index += 2) {
            if (!(pairs[index] instanceof String name) || name.isBlank()) {
                throw new IllegalArgumentException("Entity aspect condition names must be non-blank strings");
            }
            conditions.add(new Condition(name, pairs[index + 1]));
        }
        return new EntityAspects(aspects, conditions);
    }

    public boolean matches(CompoundTag data) {
        for (Condition condition : conditions) {
            if (!condition.matches(data.get(condition.name()))) {
                return false;
            }
        }
        return true;
    }

    private static Object normalize(Object value) {
        if (value instanceof Boolean || value instanceof String || value instanceof Long || value instanceof Double) {
            return value;
        }
        if (value instanceof Float) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return String.valueOf(value);
    }

    public record Condition(String name, Object value) {
        public Condition {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Entity aspect condition names must be non-blank strings");
            }
            if (value == null) {
                throw new IllegalArgumentException("Entity aspect condition values must not be null");
            }
            value = normalize(value);
        }

        private static final StreamCodec<ByteBuf, Object> VALUE_CODEC = new StreamCodec<>() {
            @Override
            public Object decode(ByteBuf buffer) {
                return switch (buffer.readByte()) {
                    case 0 -> ByteBufCodecs.STRING_UTF8.decode(buffer);
                    case 1 -> buffer.readLong();
                    case 2 -> buffer.readDouble();
                    case 3 -> buffer.readBoolean();
                    default -> throw new IllegalArgumentException("Unknown entity aspect condition value type");
                };
            }

            @Override
            public void encode(ByteBuf buffer, Object value) {
                if (value instanceof String text) {
                    buffer.writeByte(0);
                    ByteBufCodecs.STRING_UTF8.encode(buffer, text);
                } else if (value instanceof Long number) {
                    buffer.writeByte(1);
                    buffer.writeLong(number);
                } else if (value instanceof Double number) {
                    buffer.writeByte(2);
                    buffer.writeDouble(number);
                } else {
                    buffer.writeByte(3);
                    buffer.writeBoolean((Boolean) value);
                }
            }
        };

        public static final StreamCodec<ByteBuf, Condition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            Condition::name,
            VALUE_CODEC,
            Condition::value,
            Condition::new
        );

        boolean matches(Tag tag) {
            if (tag == null) {
                return false;
            }
            if (value instanceof String text) {
                return tag instanceof StringTag stringTag && stringTag.value().equals(text);
            }
            if (!(tag instanceof NumericTag numeric)) {
                return false;
            }
            if (value instanceof Boolean flag) {
                return (numeric.longValue() != 0L) == flag;
            }
            if (value instanceof Double number) {
                return numeric.doubleValue() == number;
            }
            return numeric.longValue() == (Long) value;
        }
    }
}
