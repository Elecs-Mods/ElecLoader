package nl.elec332.minecraft.loader;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.impl.LoaderInitializer;
import nl.elec332.minecraft.loader.mod.Mod;
import nl.elec332.minecraft.loader.mod.event.*;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * Created by Elec332 on 10-02-2024
 */
@Mod(ElecLoaderMod.MODID)
public class ElecLoaderMod {

    public ElecLoaderMod(IEventBus eventBus, Dist dist) {
        LOGGER.info(Objects.requireNonNull(IModLoader.INSTANCE.getModMetaData(MODID)).getModName() + " loading for loader " + IModLoader.INSTANCE.getModLoaderName() + " on dist " + dist);
        LoaderInitializer.INSTANCE.checkFinalized();
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

    private ModLoadingStage lastStage = ModLoadingStage.PRE_CONSTRUCT;

    public void onConstruct(ConstructModEvent event) {
        processAnnotations(event);
    }

    public void preInit(CommonSetupEvent event) {
        processAnnotations(event);
    }

    public void clientInit(ClientSetupEvent event) {
        processAnnotations(event);
    }

    public void serverInit(ServerSetupEvent event) {
        processAnnotations(event);
    }

    public void sendIMC(SendModCommsEvent event) {
        processAnnotations(event);
    }

    public void postInit(PostInitEvent event) {
        processAnnotations(event);
    }

    public void complete(LoadCompleteEvent event) {
        processAnnotations(event);
    }

    private void processAnnotations(final ModLoaderEvent event) {
        checkStage(event);
        ElecModLoader.getModLoader().processAnnotations(event.getLoadingStage());
        this.lastStage = event.getLoadingStage();
        if (event.getLoadingStage() == ModLoadingStage.values()[ModLoadingStage.values().length - 1]) {
            LOGGER.info("Successfully processed all annotations.");
        }
    }

    private void checkStage(final ModLoaderEvent event) {
        if (this.lastStage != ModLoadingStage.values()[event.getLoadingStage().ordinal() - 1]) {
            throw new RuntimeException("Detected a failure processing annotations in stage " + ModLoadingStage.values()[event.getLoadingStage().ordinal() - 1]);
        }
    }

}
