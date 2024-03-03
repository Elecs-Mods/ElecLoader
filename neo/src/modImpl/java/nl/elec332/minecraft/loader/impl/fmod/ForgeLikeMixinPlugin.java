package nl.elec332.minecraft.loader.impl.fmod;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import nl.elec332.minecraft.loader.api.distmarker.OnlyIn;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.impl.SideCleaner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Elec332 on 11-02-2024
 */
public class ForgeLikeMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        ElecModLoader.initSideCleaner(dataHandler -> {
            try {
                Set<Type> classes = new HashSet<>();
                dataHandler.apply(Type.getType(OnlyIn.class)).forEach(ad -> classes.add(ad.getClassType()));
                Logger logger = LogManager.getLogger("ElecLoader SideCleaner");
                SideCleaner sideCleaner = new SideCleaner(logger, ElecModLoader.getDist().name());

                Field f = Launcher.class.getDeclaredField("launchPlugins");
                f.setAccessible(true);
                Object o = f.get(Launcher.INSTANCE);
                f = o.getClass().getDeclaredField("plugins");
                f.setAccessible(true);
                ((Map) f.get(o)).put("ElecSideTransformer", new ILaunchPluginService() {

                    @Override
                    public String name() {
                        return "ElecSideTransformer";
                    }

                    @Override
                    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
                        if (classes.contains(classType)) {
                            return EnumSet.of(Phase.AFTER);
                        }
                        return EnumSet.noneOf(ILaunchPluginService.Phase.class);
                    }

                    @Override
                    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
                        sideCleaner.processClass(classNode);
                        return true;
                    }

                });

//                Field f = MixinLaunchPluginLegacy.class.getDeclaredField("processors");
//                f.setAccessible(true);
//                Object plugin = Launcher.INSTANCE.environment().findLaunchPlugin("mixin").get();
//                List l = (List) f.get(plugin);
//                l = new ArrayList(l);
//                l.add(new IClassProcessor() {});
//                System.out.println(l);
//                f.set(plugin, l);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

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
