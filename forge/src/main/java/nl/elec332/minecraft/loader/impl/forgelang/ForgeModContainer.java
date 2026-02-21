package nl.elec332.minecraft.loader.impl.forgelang;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.service.ModServiceLoader;
import nl.elec332.minecraft.loader.impl.DeferredWorkQueue;
import nl.elec332.minecraft.loader.impl.ElecModContainer;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.mod.event.mapping.IModEventMapper;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Created by Elec332 on 07-02-2024
 */
public final class ForgeModContainer extends ModContainer {

    public ForgeModContainer(IModInfo info, ModuleLayer gameLayer) {
        super(info);
        this.contextExtension = () -> null;
        this.workQueue = new DeferredWorkQueue<>();
        Arrays.stream(nl.elec332.minecraft.loader.api.modloader.ModLoadingStage.values()).forEach(stage -> {
            net.minecraftforge.fml.ModLoadingStage mls = net.minecraftforge.fml.ModLoadingStage.values()[stage.ordinal() + 1];
            mls.getDeferredWorkQueue().enqueueWork(this, () -> this.workQueue.processQueue(stage, Runnable::run));
        });

        this.elecModContainer = ElecModLoader.getModLoader().useDiscoveredMod(info.getModId(), (meta, types) -> new ElecModContainer(meta, types, name -> {
            var layer = gameLayer.findModule(info.getOwningFile().moduleName()).orElseThrow();
            return Class.forName(layer, name);
        }, (e, t) -> new ModLoadingException(info, ModLoadingStage.CONSTRUCT, e == ElecModContainer.ErrorType.CLASSLOAD ? "fml.modloading.failedtoloadmodclass" : "fml.modloading.failedtoloadmod", t, "<>"),
                () -> this.workQueue::enqueueDeferredWork));

        this.activityMap.put(ModLoadingStage.CONSTRUCT, this.elecModContainer::constructMod);
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker LOADING = MarkerManager.getMarker("LOADING");
    public final ElecModContainer elecModContainer;
    private final DeferredWorkQueue<Runnable> workQueue;

    @Override
    public boolean matches(Object mod) {
        return Objects.equals(elecModContainer.getFirstModInstance(), mod);
    }

    @Override
    public Object getMod() {
        return elecModContainer.getFirstModInstance();
    }

    @Override
    protected <T extends IModBusEvent> void acceptEvent(final T e) {
        try {
            LOGGER.trace(LOADING, "Firing event for modid {} : {}", this.getModId(), e);
            BiFunction<Object, IModContainer, ? extends Event> mapper = EVENT_MAP.get(e.getClass());
            if (mapper != null) {
                elecModContainer.getEventBus().post(mapper.apply(e, elecModContainer));
            }
            LOGGER.trace(LOADING, "Fired event for modid {} : {}", this.getModId(), e);
        } catch (Throwable t) {
            LOGGER.error(LOADING,"Caught exception during event {} dispatch for modid {}", e, this.getModId(), t);
            throw new ModLoadingException(modInfo, modLoadingStage, "fml.modloading.errorduringevent", t);
        }
    }

    public static final Map<Class<? extends IModBusEvent>, BiFunction<Object, IModContainer, ? extends Event>> EVENT_MAP;

    static {
        try {
            EVENT_MAP = new HashMap<>();
            ModServiceLoader.loadSingleModService(IModEventMapper.class).registerMappings(new IModEventMapper.Registry() {

                @Override
                @SuppressWarnings("unchecked")
                public <T> void register(Class<T> type, BiFunction<T, IModContainer, Event> mapper) {
                    if (!IModBusEvent.class.isAssignableFrom(type)) {
                        throw new UnsupportedOperationException("No support for event: " + type);
                    }
                    EVENT_MAP.put((Class<? extends IModBusEvent>) type, (BiFunction<Object, IModContainer, ? extends Event>) mapper);
                }

            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
