package nl.elec332.minecraft.loader.impl.neo20lang;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingStage;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.ParallelDispatchEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.service.ModServiceLoader;
import nl.elec332.minecraft.loader.impl.DeferredWorkQueue;
import nl.elec332.minecraft.loader.impl.ElecModContainer;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.mod.event.ModLoaderEvent;
import nl.elec332.minecraft.loader.mod.event.mapping.IModEventMapper;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Created by Elec332 on 07-02-2024
 */
public final class NeoModContainer extends ModContainer {

    public NeoModContainer(IModInfo info, ModuleLayer gameLayer) {
        super(info);
        this.contextExtension = () -> null;
        this.neoEventBus = net.neoforged.bus.api.BusBuilder.builder()
                .setExceptionHandler(this::onNeoEventFailed)
                .markerType(IModBusEvent.class)
                .allowPerPhasePost()
                .build();
        this.workQueue = new DeferredWorkQueue<>();

        elecModContainer = ElecModLoader.getModLoader().useDiscoveredMod(info.getModId(), (meta, types) -> new ElecModContainer(meta, types, name -> {
            var layer = gameLayer.findModule(info.getOwningFile().moduleName()).orElseThrow();
            return Class.forName(layer, name);
        }, (e, t) -> new ModLoadingException(info, ModLoadingStage.CONSTRUCT, e == ElecModContainer.ErrorType.CLASSLOAD ? "fml.modloading.failedtoloadmodclass" : "fml.modloading.failedtoloadmod", t, "<>"),
                () -> this.workQueue::enqueueDeferredWork));
        this.activityMap.put(ModLoadingStage.CONSTRUCT, this.elecModContainer::constructMod);

        EVENT_MAP.forEach((type, factory) -> neoEventBus.addListener(type, e -> {
            final Event event = factory.apply(e, elecModContainer);
            if (e instanceof ParallelDispatchEvent) {
                if (!(event instanceof ModLoaderEvent)) {
                    throw new UnsupportedOperationException();
                }
                ((ParallelDispatchEvent) e).enqueueWork(() -> this.workQueue.processQueue(((ModLoaderEvent) event).getLoadingStage(), Runnable::run));
            }
            elecModContainer.getEventBus().post(event);
        }));
    }

    private static final Logger LOGGER = LogManager.getLogger();

    public final ElecModContainer elecModContainer;
    private final net.neoforged.bus.api.IEventBus neoEventBus;
    private final DeferredWorkQueue<Runnable> workQueue;

    private void onNeoEventFailed(net.neoforged.bus.api.IEventBus iEventBus, net.neoforged.bus.api.Event event, net.neoforged.bus.api.EventListener[] iEventListeners, int i, Throwable throwable) {
        LOGGER.error(new net.neoforged.bus.EventBusErrorMessage(event, i, iEventListeners, throwable));
    }

    @Override
    public boolean matches(Object mod) {
        return Objects.equals(elecModContainer.getFirstModInstance(), mod);
    }

    @Override
    public Object getMod() {
        return elecModContainer.getFirstModInstance();
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
