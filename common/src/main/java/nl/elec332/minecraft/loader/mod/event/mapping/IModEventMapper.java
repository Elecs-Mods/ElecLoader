package nl.elec332.minecraft.loader.mod.event.mapping;

import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by Elec332 on 28-02-2024
 */
public interface IModEventMapper {

    void registerMappings(Registry registry);

    interface Registry {

        default <T> void register(Class<T> type, Supplier<Event> mapper) {
            register(type, (e, m) -> mapper.get());
        }

        default <T> void register(Class<T> type, Function<IModContainer, Event> mapper) {
            register(type, (e, m) -> mapper.apply(m));
        }

        <T> void register(Class<T> type, BiFunction<T, IModContainer, Event> mapper);

    }

}
