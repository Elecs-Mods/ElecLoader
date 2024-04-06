package nl.elec332.minecraft.loader.impl.fabric.mixin;

import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.impl.fabriclike.FabricModStages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created by Elec332 on 13-02-2024
 */
@Mixin(ReloadableResourceManager.class)
public abstract class ClientModLoaderMixin {

    @Inject(at = @At("TAIL"), method = "<init>")
    private void run(CallbackInfo info) {
        FabricModStages.discover();
        ((ReloadableResourceManager) (Object) this).registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            if (loaded) {
                return;
            }
            FabricModStages.init(Dist.CLIENT);
            FabricModStages.postInit();
            loaded = true;
        });
    }

    private static boolean loaded = false;

}
