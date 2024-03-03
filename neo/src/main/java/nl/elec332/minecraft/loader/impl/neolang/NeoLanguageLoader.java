package nl.elec332.minecraft.loader.impl.neolang;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.neoforged.fml.Logging;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingStage;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageProvider;
import net.neoforged.neoforgespi.language.ModFileScanData;
import nl.elec332.minecraft.loader.impl.LoaderConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 06-02-2024
 */
public class NeoLanguageLoader implements IModLanguageProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String name() {
        return "elecjava";
    }

    @Override
    public Consumer<ModFileScanData> getFileVisitor() {
        return scanResult -> {
            final Map<String, ModTarget> modTargetMap = scanResult.getAnnotations().stream()
                    .filter(ad -> ad.annotationType().equals(LoaderConstants.MODANNOTATION))
                    .map(ad -> new ModTarget(ad.clazz().getClassName(), (String)ad.annotationData().get("value")))
                    .collect(Collectors.toMap(ModTarget::modId, Function.identity(), (a, b) -> a));
            scanResult.addLanguageLoader(modTargetMap);
        };
    }

    private record ModTarget(String className, String modId) implements IModLanguageLoader {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T loadMod(final IModInfo info, final ModFileScanData modFileScanResults, ModuleLayer gameLayer) {
            // This language class is loaded in the system level classloader - before the game even starts
            // So we must treat container construction as an arms length operation, and load the container
            // in the classloader of the game - the context classloader is appropriate here.
            try {
                final Class<?> fmlContainer = Class.forName(LoaderConstants.PACKAGE_ROOT + ".impl.neolang.NeoModContainer", true, Thread.currentThread().getContextClassLoader());
                LOGGER.debug(Logging.LOADING, "Loading NeoModContainer from classloader {} - got {}", Thread.currentThread().getContextClassLoader(), fmlContainer.getClassLoader());
                final Constructor<?> constructor = fmlContainer.getConstructor(IModInfo.class, String.class, ModFileScanData.class, ModuleLayer.class);
                return (T) constructor.newInstance(info, className, modFileScanResults, gameLayer);
            } catch (InvocationTargetException e) {
                LOGGER.fatal(Logging.LOADING, "Failed to build mod", e);
                if (e.getTargetException() instanceof ModLoadingException mle) {
                    throw mle;
                } else {
                    throw new ModLoadingException(info, ModLoadingStage.CONSTRUCT, "fml.modloading.failedtoloadmodclass", e);
                }
            } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException |
                     IllegalAccessException e) {
                LOGGER.fatal(Logging.LOADING, "Unable to load FMLModContainer, wut?", e);

                final Class<RuntimeException> mle = (Class<RuntimeException>) LamdbaExceptionUtils.uncheck(() -> Class.forName("net.neoforged.fml.ModLoadingException", true, Thread.currentThread().getContextClassLoader()));
                final Class<ModLoadingStage> mls = (Class<ModLoadingStage>) LamdbaExceptionUtils.uncheck(() -> Class.forName("net.neoforged.fml.ModLoadingStage", true, Thread.currentThread().getContextClassLoader()));
                throw LamdbaExceptionUtils.uncheck(() -> LamdbaExceptionUtils.uncheck(() -> mle.getConstructor(IModInfo.class, mls, String.class, Throwable.class)).newInstance(info, Enum.valueOf(mls, "CONSTRUCT"), "fml.modloading.failedtoloadmodclass", e));
            }
        }

    }

}
