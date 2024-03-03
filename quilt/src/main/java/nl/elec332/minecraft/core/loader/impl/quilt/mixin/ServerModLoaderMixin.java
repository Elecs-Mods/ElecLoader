package nl.elec332.minecraft.loader.impl.quilt.mixin;

import net.minecraft.server.Main;
import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.impl.fabriclike.FabricModStages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created by Elec332 on 18-02-2024
 */
@Mixin(Main.class)
public class ServerModLoaderMixin {

    @Inject(
            method = "main",
//            at = @At(value = "INVOKE_STRING", target = "Ljava/nio/file/Paths;get(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;", args = "ldc=server.properties"),
//            at = @At(value = "CONSTANT", args = "stringValue=server.properties"),
            at = @At(value = "INVOKE", target = "Ljava/io/File;<init>(Ljava/lang/String;)V", ordinal = 0),
            remap = false
    )
    private static void run(String[] strings, CallbackInfo info) {
        FabricModStages.discover();
        FabricModStages.init(Dist.DEDICATED_SERVER);
        FabricModStages.postInit();
    }

}
