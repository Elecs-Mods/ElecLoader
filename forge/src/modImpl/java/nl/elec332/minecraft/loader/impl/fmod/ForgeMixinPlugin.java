package nl.elec332.minecraft.loader.impl.fmod;

import net.minecraftforge.fml.loading.LoadingModList;
import nl.elec332.minecraft.loader.impl.LoaderInitializer;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 21-02-2024
 */
public final class ForgeMixinPlugin implements IMixinConfigPlugin {

    static {
        try {
            if (isForge()) {
                LoadingModList.get().getMods().stream()
                        .map(m -> m.getOwningFile().getConfig())
                        .flatMap(cfg -> {
                            String entry = IModLoader.INSTANCE.getMappingTarget() == MappingType.NAMED ? "named_forgemixins" : "forgemixins";
                            try {
                                //Use string-array for <1.19.2 support
                                return cfg.getConfigList(new String[]{entry}).stream().map(e -> (String) e.getConfigElement(new String[]{"config"}).get());
                            } catch (Exception e) {
                                return Stream.empty();
                            }
                        }).collect(Collectors.toSet())
                        .forEach(Mixins::addConfiguration);
            }
        } catch (Throwable e) {
            LoaderInitializer.INSTANCE.mixinFailed(e);
        }
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
