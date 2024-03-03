package nl.elec332.minecraft.loader.impl.neolang;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingStage;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.service.ModServiceLoader;
import nl.elec332.minecraft.loader.impl.ElecModContainer;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.mod.event.mapping.IModEventMapper;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Created by Elec332 on 07-02-2024
 */
public final class NeoModContainer extends ModContainer {

    public NeoModContainer(IModInfo info, String className, ModFileScanData modFileScanResults, ModuleLayer gameLayer) {
        super(info);
        contextExtension = () -> null;
        this.neoEventBus = net.neoforged.bus.api.BusBuilder.builder()
                .setExceptionHandler(this::onNeoEventFailed)
                .markerType(IModBusEvent.class)
                .allowPerPhasePost()
                .build();

        elecModContainer = ElecModLoader.getModLoader().useDiscoveredMod(info.getModId(), (meta, type) -> {
            return new ElecModContainer(meta, className, name -> {
                var layer = gameLayer.findModule(info.getOwningFile().moduleName()).orElseThrow();
                return Class.forName(layer, className);
            }, (e, c) -> new ModLoadingException(info, ModLoadingStage.CONSTRUCT, e.getKey() == ElecModContainer.ErrorType.CLASSLOAD ? "fml.modloading.failedtoloadmodclass" : "fml.modloading.failedtoloadmod", e.getValue(), c));
        });

        EVENT_MAP.forEach((type, factory) -> neoEventBus.addListener(type, e -> elecModContainer.getEventBus().post(factory.apply(e, elecModContainer))));
    }

    private static final Logger LOGGER = LogManager.getLogger();

    public final ElecModContainer elecModContainer;
    private final net.neoforged.bus.api.IEventBus neoEventBus;

    private void onNeoEventFailed(net.neoforged.bus.api.IEventBus iEventBus, net.neoforged.bus.api.Event event, net.neoforged.bus.api.EventListener[] iEventListeners, int i, Throwable throwable) {
        LOGGER.error(new net.neoforged.bus.EventBusErrorMessage(event, i, iEventListeners, throwable));
    }

    @Override
    public boolean matches(Object mod) {
        return elecModContainer.matches(mod);
    }

    @Override
    public Object getMod() {
        return elecModContainer.getMod();
    }

    @Override
    public net.neoforged.bus.api.IEventBus getEventBus() {
        return neoEventBus;
    }

    public static final Map<Class<? extends net.neoforged.bus.api.Event>, BiFunction<Object, IModContainer, ? extends Event>> EVENT_MAP;

    static {
        try {
            EVENT_MAP = new HashMap<>();
            ModServiceLoader.loadSingleModService(IModEventMapper.class).registerMappings(new IModEventMapper.Registry() {

                @Override
                @SuppressWarnings("unchecked")
                public <T> void register(Class<T> type, BiFunction<T, IModContainer, Event> mapper) {
                    if (!net.neoforged.bus.api.Event.class.isAssignableFrom(type)) {
                        throw new UnsupportedOperationException();
                    }
                    EVENT_MAP.put((Class<? extends net.neoforged.bus.api.Event>) type, (BiFunction<Object, IModContainer, ? extends Event>) mapper);
                }

            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
