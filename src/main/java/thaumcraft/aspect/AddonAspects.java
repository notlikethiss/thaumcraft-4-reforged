package thaumcraft.aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;

public final class AddonAspects {
    private static final List<Consumer<Addon>> REGISTRARS = new ArrayList<>();
    private static final Addon API = (tag, color, components, blend, texture) ->
        new Aspect(tag, color, components, texture, blend);
    private static boolean initialized;

    private AddonAspects() {
    }

    public interface Addon {
        Aspect register(String tag, int color, Aspect[] components, int blend, Identifier texture);
    }

    public static synchronized void addRegistrar(Consumer<Addon> registrar) {
        Objects.requireNonNull(registrar, "registrar");
        if (initialized) {
            registrar.accept(API);
        } else {
            REGISTRARS.add(registrar);
        }
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        for (int index = 0; index < REGISTRARS.size(); index++) {
            REGISTRARS.get(index).accept(API);
        }
        REGISTRARS.clear();
        initialized = true;
    }
}
