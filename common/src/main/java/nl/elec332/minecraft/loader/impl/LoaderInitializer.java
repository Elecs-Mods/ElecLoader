package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.ElecLoaderMod;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationDataHandler;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.util.IClassTransformer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 06-04-2024
 */
public enum LoaderInitializer {

    INSTANCE;

    private ElecModLoader modLoader;
    private boolean started = false;
    private Throwable crashed = null;
    private boolean checked = false;
    private boolean finalized = false;

    public void startLoader(Consumer<IClassTransformer> registry) {
        if (this.started) {
            throw new IllegalStateException();
        }
        this.started = true;
        try {
            IAnnotationDataHandler dataHandler = AnnotationDataHandler.INSTANCE.identify(DeferredModLoader.INSTANCE.getModFiles(), DeferredModLoader.INSTANCE::hasWrongSideOnly);
            this.modLoader = new ElecModLoader(dataHandler::getAnnotationList);
            SideCleaner.register(registry, dataHandler::getAnnotationList);
            MappingTransformer.register(registry, dataHandler::getAnnotationList);
        } catch (Exception e) {
            mixinFailed(e);
            throw e;
        }
    }

    public void checkEnvironment() {
        if (this.checked) {
            return;
        }
        if (this.crashed != null || !this.started || this.modLoader == null) {
            if (this.crashed != null) {
                throw new RuntimeException(this.crashed);
            }
            throw new IllegalStateException("Mixin setup failed!");
        }
        getModLoader().checkEnvironment();
        this.checked = true;
    }

    public void mixinFailed(Throwable e) {
        if (this.crashed != null) {
            return; //We already crashed before...
        }
        this.modLoader = null;
        this.crashed = Objects.requireNonNull(e);
    }

    public void finalizeLoading() {
        synchronized (INSTANCE) {
            if (!this.started) {
                throw new IllegalStateException("Loader hasn't started!");
            }
            if (this.finalized) {
                throw new IllegalStateException("Loader has already finished loading!");
            }
            checkEnvironment();

            DeferredModLoader.INSTANCE.fillModContainers();
            if (getModLoader().getModContainer(ElecLoaderMod.MODID) == null || DeferredModLoader.INSTANCE.getModContainer(ElecLoaderMod.MODID) == null) {
                loaderModNotFound();
            }

            getModLoader().finalizeLoading();
            this.finalized = true;
            AnnotationDataHandler.INSTANCE.preProcess();
            AnnotationDataHandler.INSTANCE.process(ModLoadingStage.PRE_CONSTRUCT);
        }
    }

    public void checkFinalized() {
        if (!finalized) {
            throw new IllegalStateException("Loader failed to properly finish loading!");
        }
    }

    public boolean completedSetup() {
        return finalized;
    }

    @NotNull
    ElecModLoader getModLoader() {
        if (this.modLoader == null) {
            if (!this.started) {
                throw new UnsupportedOperationException();
            }
            checkEnvironment(); //This will force the crash
            throw new RuntimeException("wut?");
        }
        return this.modLoader;
    }

    void loaderModNotFound() {
        throw new RuntimeException("ElecLoaderMod failed to load itself correctly!");
    }

}
