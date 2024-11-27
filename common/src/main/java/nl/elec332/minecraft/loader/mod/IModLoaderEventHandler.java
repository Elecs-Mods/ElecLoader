package nl.elec332.minecraft.loader.mod;

import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.service.ModServiceLoader;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;

import java.util.function.Function;

/**
 * Created by Elec332 on 16-04-2024
 * <p>
 * Helper to easily post events to all custom loaded mods
 */
public interface IModLoaderEventHandler {

    /**
     * The provided implementation
     */
    IModLoaderEventHandler INSTANCE = ModServiceLoader.loadAPIService(IModLoaderEventHandler.class);

    /**
     * Posts the provided event to all custom loaded mods.
     *
     * @param event The event to be posted
     * @return The event whose result can be used for further processing
     * @param <T> The event type
     */
    <T extends Event> T postEvent(T event);

    /**
     * Posts a new instance of the provided event factory to each custom loaded mod.
     *
     * @param event The event factory for producing an event for each {@link IModContainer}
     * @param <T> The event type
     */
    <T extends Event> void postModEvent(Function<IModContainer, T> event);

}
