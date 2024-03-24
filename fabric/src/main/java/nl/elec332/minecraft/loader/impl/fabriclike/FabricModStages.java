package nl.elec332.minecraft.loader.impl.fabriclike;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.impl.ElecModContainer;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.mod.event.*;

import java.util.Map;

/**
 * Created by Elec332 on 13-02-2024
 */
public final class FabricModStages {

    public static void discover() {
        ElecModLoader.getModLoader().useDiscoveredMods((meta, type) -> new ElecModContainer(meta, type.getClassName(), Class::forName, (e, c) -> new RuntimeException("Failed to " + e.getKey() + " mod " + meta.getModId(), e.getValue())));
        ElecModLoader.getModLoader().finalizeLoading();
        ElecModLoader.checkModLoader();
        ElecModLoader.getModLoader().postModEvent(ConstructModEvent::new);
        processQueue(ModLoadingStage.CONSTRUCT);
    }

    public static void init(Dist dist) {
        ElecModLoader.getModLoader().postEvent(new ModConfigEvent());
        ElecModLoader.getModLoader().postModEvent(CommonSetupEvent::new);
        processQueue(ModLoadingStage.COMMON_SETUP);
        switch (dist) {
            case CLIENT -> ElecModLoader.getModLoader().postModEvent(ClientSetupEvent::new);
            case DEDICATED_SERVER -> ElecModLoader.getModLoader().postModEvent(ServerSetupEvent::new);
            default -> throw new IllegalArgumentException("Unknown state: " + dist.name());
        }
        processQueue(ModLoadingStage.SIDED_SETUP);
    }

    public static void postInit() {
        ElecModLoader.getModLoader().postModEvent(SendModCommsEvent::new);
        processQueue(ModLoadingStage.MODCOMMS_SEND);
        ElecModLoader.getModLoader().postModEvent(PostInitEvent::new);
        processQueue(ModLoadingStage.LATE_SETUP);
        ElecModLoader.getModLoader().postModEvent(LoadCompleteEvent::new);
        processQueue(ModLoadingStage.COMPLETE);
    }

    private static void processQueue(ModLoadingStage stage) {
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
