package nl.elec332.minecraft.loader.impl.fabriclike;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.impl.DeferredWorkQueue;
import nl.elec332.minecraft.loader.impl.ElecModContainer;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.mod.IModLoaderEventHandler;
import nl.elec332.minecraft.loader.mod.event.*;
import nl.elec332.minecraft.loader.util.LateObject;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by Elec332 on 13-02-2024
 */
public final class FabricModStages {

    public static void discover() {
        ElecModLoader.getModLoader().useDiscoveredMods((meta, types) -> {
            LateObject<BiConsumer<ModLoadingStage, Runnable>> reg = new LateObject<>();
            final ElecModContainer ret = new ElecModContainer(meta, types, Class::forName, (e, t) -> new RuntimeException("Failed to " + e + " mod " + meta.getModId(), t), reg);
            reg.set(((stage, runnable) -> FabricModStages.DEFERRED_WORK_QUEUE.enqueueDeferredWork(stage, Map.entry(ret, runnable))));
            return ret;
        });
        ElecModLoader.getModLoader().getMods().forEach(mc -> ((ElecModContainer) mc).constructMod());
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

    private static final DeferredWorkQueue<Map.Entry<IModContainer, Runnable>> DEFERRED_WORK_QUEUE = new DeferredWorkQueue<>();

    private static void processQueue(ModLoadingStage stage) {
        DEFERRED_WORK_QUEUE.processQueue(stage, entry -> {
            try {
                entry.getValue().run();
            } catch (Throwable e) {
                throw new RuntimeException("Failed to do deferred work for mod: " + entry.getKey().getModId(), e);
            }
        });
    }

}
