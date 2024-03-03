package nl.elec332.minecraft.loader.mod.event;

import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;

/**
 * Created by Elec332 on 13-02-2024
 */
public class LoadCompleteEvent extends ModLoaderEvent {

    public LoadCompleteEvent(IModContainer modContainer) {
        super(modContainer, ModLoadingStage.COMPLETE);
    }

}
