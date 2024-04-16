package nl.elec332.minecraft.loader.impl.fabriclike;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.impl.ElecModContainer;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.impl.LoaderInitializer;
import nl.elec332.minecraft.loader.mod.IModLoaderEventHandler;
import nl.elec332.minecraft.loader.mod.event.*;

import java.util.Map;

/**
 * Created by Elec332 on 13-02-2024
 */
public final class FabricModStages {

    public static void discover() {
        ElecModLoader.getModLoader().useDiscoveredMods((meta, type) -> new ElecModContainer(meta, type.getClassName(), Class::forName, (e, c) -> new RuntimeException("Failed to " + e.getKey() + " mod " + meta.getModId(), e.getValue())));
        LoaderInitializer.INSTANCE.finalizeLoading();
        IModLoaderEventHandler.INSTANCE.postModEvent(ConstructModEvent::new);
        processQueue(ModLoadingStage.CONSTRUCT);
    }

    public static void init(Dist dist) {
        IModLoaderEventHandler.INSTANCE.postEvent(new ModConfigEvent());
        IModLoaderEventHandler.INSTANCE.postModEvent(CommonSetupEvent::new);
        processQueue(ModLoadingStage.COMMON_SETUP);
        switch (dist) {
            case CLIENT -> IModLoaderEventHandler.INSTANCE.postModEvent(ClientSetupEvent::new);
            case DEDICATED_SERVER -> IModLoaderEventHandler.INSTANCE.postModEvent(ServerSetupEvent::new);
            default -> throw new IllegalArgumentException("Unknown state: " + dist.name());
        }
        processQueue(ModLoadingStage.SIDED_SETUP);
    }

    public static void postInit() {
        IModLoaderEventHandler.INSTANCE.postModEvent(SendModCommsEvent::new);
        processQueue(ModLoadingStage.MODCOMMS_SEND);
        IModLoaderEventHandler.INSTANCE.postModEvent(PostInitEvent::new);
        processQueue(ModLoadingStage.LATE_SETUP);
        IModLoaderEventHandler.INSTANCE.postModEvent(LoadCompleteEvent::new);
        processQueue(ModLoadingStage.COMPLETE);
    }

    private static void processQueue(ModLoadingStage stage) {
        synchronized (AbstractFabricBasedModLoader.DEFERRED_WORK_QUEUE) {
            var runnables = AbstractFabricBasedModLoader.DEFERRED_WORK_QUEUE.get(stage);
            if (runnables == null) {
                throw new IllegalArgumentException("Stage already processed: " + stage.getName());
            }
            Map.Entry<IModContainer, Runnable> entry;
            while ((entry = runnables.poll()) != null) {
                try {
                    entry.getValue().run();
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to do deferred work for mod: " + entry.getKey().getModId(), e);
                }
            }
            if (!runnables.isEmpty()) {
                throw new RuntimeException();
            }
            AbstractFabricBasedModLoader.DEFERRED_WORK_QUEUE.remove(stage);
        }
    }

}
