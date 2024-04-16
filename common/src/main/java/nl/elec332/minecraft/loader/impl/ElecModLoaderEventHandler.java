package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.mod.IModLoaderEventHandler;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 16-04-2024
 */
public final class ElecModLoaderEventHandler implements IModLoaderEventHandler {

    static ElecModLoaderEventHandler INSTANCE;

    public ElecModLoaderEventHandler() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        INSTANCE = this;
    }

    Supplier<Stream<ElecModContainer>> containers;

    @Override
    public <T extends Event> T postEvent(T event) {
        containers.get().forEach(mc -> mc.getEventBus().post(event));
        return event;
    }

    @Override
    public <T extends Event> void postModEvent(Function<IModContainer, T> event) {
        containers.get().forEach(mc -> mc.getEventBus().post(event.apply(mc)));
    }

}
