package nl.elec332.minecraft.loader.impl.forgelang;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.forgespi.language.*;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.impl.LoaderConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 06-02-2024
 */
public class ForgeLanguageLoader implements IModLanguageProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String name() {
        return "elecjava";
    }

    @Override
    public Consumer<ModFileScanData> getFileVisitor() {
        return scanResult -> {
            Set<String> mods = scanResult.getIModInfoData().stream()
                    .map(IModFileInfo::getMods)
                    .flatMap(List::stream)
                    .map(IModInfo::getModId)
                    .collect(Collectors.toSet());
            scanResult.addLanguageLoader(ElecModLoader.waitForModLoader().getDiscoveredMods().stream()
                    .filter(mods::contains)
                    .collect(Collectors.toMap(Function.identity(), s -> new ModTarget()))
            );
        };
    }

    @Override
    public <R extends ILifecycleEvent<R>> void consumeLifecycleEvent(Supplier<R> consumeEvent) {
    }

    private static class ModTarget implements IModLanguageLoader {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T loadMod(final IModInfo info, final ModFileScanData modFileScanResults, ModuleLayer gameLayer) {
            // This language class is loaded in the system level classloader - before the game even starts
            // So we must treat container construction as an arms length operation, and load the container
            // in the classloader of the game - the context classloader is appropriate here.
            try {

                final Class<?> fmlContainer = Class.forName(LoaderConstants.PACKAGE_ROOT + ".impl.forgelang.ForgeModContainer", true, Thread.currentThread().getContextClassLoader());
                LOGGER.debug(Logging.LOADING, "Loading ForgeModContainer from classloader {} - got {}", Thread.currentThread().getContextClassLoader(), fmlContainer.getClassLoader());
                final Constructor<?> constructor = fmlContainer.getConstructor(IModInfo.class, ModuleLayer.class);
                return (T) constructor.newInstance(info, gameLayer);
            } catch (InvocationTargetException e) {
                LOGGER.fatal(Logging.LOADING, "Failed to build mod", e);
                if (e.getTargetException() instanceof ModLoadingException mle) {
                    throw mle;
                } else {
                    throw new ModLoadingException(info, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmodclass", e);
                }
            } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                LOGGER.fatal(Logging.LOADING, "Unable to load FMLModContainer, wut?", e);

                final Class<RuntimeException> mle = (Class<RuntimeException>)LamdbaExceptionUtils.uncheck(()->Class.forName("net.minecraftforge.fml.ModLoadingException", true, Thread.currentThread().getContextClassLoader()));
                final Class<ModLoadingStage> mls = (Class<ModLoadingStage>) LamdbaExceptionUtils.uncheck(()->Class.forName("net.minecraftforge.fml.ModLoadingStage", true, Thread.currentThread().getContextClassLoader()));
                throw LamdbaExceptionUtils.uncheck(()->LamdbaExceptionUtils.uncheck(()->mle.getConstructor(IModInfo.class, mls, String.class, Throwable.class, Object[].class)).newInstance(info, Enum.valueOf(mls, "CONSTRUCT"), "fml.modloading.failedtoloadmodclass", e, new String[0]));
            }
        }

    }

}
