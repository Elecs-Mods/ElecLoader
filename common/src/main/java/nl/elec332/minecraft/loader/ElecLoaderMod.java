package nl.elec332.minecraft.loader;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.mod.Mod;
import nl.elec332.minecraft.loader.mod.event.*;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Elec332 on 10-02-2024
 */
@Mod(ElecLoaderMod.MODID)
public class ElecLoaderMod {

    public ElecLoaderMod(IEventBus eventBus, Dist dist) {
        LOGGER.info("ElecLoader loading for side " + dist + " for modloader " + IModLoader.INSTANCE.getModLoaderName());
        ElecModLoader.checkModLoader();
        eventBus.addListener(this::onConstruct);
        eventBus.addListener(this::preInit);
        eventBus.addListener(this::clientInit);
        eventBus.addListener(this::serverInit);
        eventBus.addListener(this::sendIMC);
        eventBus.addListener(this::postInit);
        eventBus.addListener(this::complete);
    }

    public static final String MODID = "elecloader";
    public static final Logger LOGGER = LogManager.getLogger();

    public void onConstruct(ConstructModEvent event) {
        ElecModLoader.processAnnotations(event.getLoadingStage());
    }

    public void preInit(CommonSetupEvent event) {
        ElecModLoader.processAnnotations(event.getLoadingStage());
    }

    public void clientInit(ClientSetupEvent event) {
        ElecModLoader.processAnnotations(event.getLoadingStage());
    }

    public void serverInit(ServerSetupEvent event) {
        ElecModLoader.processAnnotations(event.getLoadingStage());
    }

    public void sendIMC(SendModCommsEvent event) {
        ElecModLoader.processAnnotations(event.getLoadingStage());
    }

    public void postInit(PostInitEvent event) {
        ElecModLoader.processAnnotations(event.getLoadingStage());
    }

    public void complete(LoadCompleteEvent event) {
        ElecModLoader.processAnnotations(event.getLoadingStage());
    }

}
