package nl.elec332.minecraft.loader.impl.fmod;

import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IConfigurable;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.modloader.MappingType;
import nl.elec332.minecraft.loader.impl.LoaderInitializer;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 21-02-2024
 */
public final class ForgeMixinPlugin implements IMixinConfigPlugin {

    static {
        try {
            if (isForge()) {
                Consumer<String> mixinRegistrar;
                try {
                    MethodHandle mh = MethodHandles.lookup().unreflect(Mixins.class.getDeclaredMethod("addConfiguration", String.class, Class.forName("org.spongepowered.asm.mixin.extensibility.IMixinConfigSource")));
                    mixinRegistrar = s -> {
                        try {
                            mh.invoke(s, null);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    };
                } catch (NoSuchMethodException | ClassNotFoundException q) {
                    try {
                        MethodHandle mh = MethodHandles.lookup().unreflect(Mixins.class.getDeclaredMethod("addConfiguration", String.class));
                        mixinRegistrar = s -> {
                            try {
                                mh.invoke(s);
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        };
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException("Failed to find mixin registry endpoints!");
                    }
                }
                getModConfigurations().flatMap(cfg -> {
                            String entry = IModLoader.INSTANCE.getMappingTarget() == MappingType.NAMED ? "named_forgemixins" : "forgemixins";
                            try {
                                //Use string-array for <1.19.2 support
                                return cfg.getConfigList(new String[]{entry}).stream().flatMap(e -> e.<String>getConfigElement(new String[]{"config"}).stream());
                            } catch (Exception e) {
                                return Stream.empty();
                            }
                        }).collect(Collectors.toSet())
                        .forEach(mixinRegistrar);
            }
        } catch (Throwable e) {
            LoaderInitializer.INSTANCE.mixinFailed(e);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static Stream<IConfigurable> getModConfigurations() {
        return LoadingModList.get().getMods().stream()
                .map(m -> m.getOwningFile().getConfig());
    }

    private static boolean isForge() {
        try {
            Class.forName("net.minecraftforge.fml.loading.LoadingModList");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return Collections.emptyList();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
