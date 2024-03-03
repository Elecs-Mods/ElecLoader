package nl.elec332.minecraft.loader.mod.event;

import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;

/**
 * Created by Elec332 on 10-02-2024
 */
public class CommonSetupEvent extends ModLoaderEvent {
    public CommonSetupEvent(IModContainer modContainer) {
        super(modContainer, ModLoadingStage.COMMON_SETUP);
    }
}
