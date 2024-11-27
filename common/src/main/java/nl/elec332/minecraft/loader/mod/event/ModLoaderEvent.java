package nl.elec332.minecraft.loader.mod.event;

import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;
import nl.elec332.minecraft.loader.impl.ElecModContainer;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;

/**
 * Created by Elec332 on 05-02-2024
 */
public abstract class ModLoaderEvent extends Event {

    public ModLoaderEvent(IModContainer modContainer, ModLoadingStage stage) {
        this.modContainer = modContainer;
        this.stage = stage;
    }

    private final IModContainer modContainer;
    private final ModLoadingStage stage;

    public ModLoadingStage getLoadingStage() {
        return stage;
    }

    public IModContainer getModContainer() {
        return modContainer;
    }

    /**
     * Enqueues work to be run after this {@link ModLoadingStage} has completed.
     * Useful as mod may be loaded async or in an unpredictable order.
     *
     * @param work The worker to be run after the specified {@link ModLoadingStage}
     */
    public void enqueueDeferredWork(Runnable work) {
        ((ElecModContainer) getModContainer()).enqueueDeferredWork(getLoadingStage(), work);
    }

}
