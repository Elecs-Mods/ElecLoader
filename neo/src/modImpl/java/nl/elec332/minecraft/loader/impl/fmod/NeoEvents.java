package nl.elec332.minecraft.loader.impl.fmod;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.*;
import nl.elec332.minecraft.loader.api.modloader.IModLoader;
import nl.elec332.minecraft.loader.api.service.ModService;
import nl.elec332.minecraft.loader.mod.event.*;
import nl.elec332.minecraft.loader.mod.event.mapping.IModEventMapper;

/**
 * Created by Elec332 on 28-02-2024
 */
@ModService(IModLoader.Type.NEOFORGE)
public class NeoEvents implements IModEventMapper {

    @Override
    public void registerMappings(Registry registry) {
        registry.register(FMLConstructModEvent.class, ConstructModEvent::new);
        registry.register(ModConfigEvent.class, nl.elec332.minecraft.loader.mod.event.ModConfigEvent::new);
        registry.register(FMLCommonSetupEvent.class, CommonSetupEvent::new);
        registry.register(FMLClientSetupEvent.class, ClientSetupEvent::new);
        registry.register(FMLDedicatedServerSetupEvent.class, ServerSetupEvent::new);
        registry.register(InterModEnqueueEvent.class, SendModCommsEvent::new);
        registry.register(InterModProcessEvent.class, PostInitEvent::new);
        registry.register(FMLLoadCompleteEvent.class, LoadCompleteEvent::new);
    }

}
