package thaumcraft.research;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;

public final class ResearchCategory {
    private final String key;
    private final ResourceLocation icon;
    private final ResourceLocation background;
    private final Map<String, ResearchItem> research = new LinkedHashMap<>();
    private int minColumn;
    private int minRow;
    private int maxColumn;
    private int maxRow;

    ResearchCategory(String key, ResourceLocation icon, ResourceLocation background) {
        this.key = key;
        this.icon = icon;
        this.background = background;
    }

    public String key() {
        return key;
    }

    public ResourceLocation icon() {
        return icon;
    }

    public ResourceLocation background() {
        return background;
    }

    public String nameKey() {
        return "tc.research_category." + key;
    }

    public Collection<ResearchItem> research() {
        return research.values().stream().filter(ResearchItem::isAvailable).toList();
    }

    public @Nullable ResearchItem get(String researchKey) {
        ResearchItem item = research.get(researchKey);
        return item != null && item.isAvailable() ? item : null;
    }

    public int minColumn() {
        return minColumn;
    }

    public int minRow() {
        return minRow;
    }

    public int maxColumn() {
        return maxColumn;
    }

    public int maxRow() {
        return maxRow;
    }

    boolean add(ResearchItem item) {
        if (research.containsKey(item.key())) {
            return false;
        }
        if (!item.isVirtual()) {
            for (ResearchItem other : research.values()) {
                if (other.column() == item.column() && other.row() == item.row()) {
                    return false;
                }
            }
        }
        research.put(item.key(), item);
        minColumn = Math.min(minColumn, item.column());
        minRow = Math.min(minRow, item.row());
        maxColumn = Math.max(maxColumn, item.column());
        maxRow = Math.max(maxRow, item.row());
        return true;
    }
}
