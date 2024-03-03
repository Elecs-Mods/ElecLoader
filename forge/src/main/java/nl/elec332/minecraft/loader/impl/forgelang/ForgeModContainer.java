package nl.elec332.minecraft.loader.impl.forgelang;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.service.ModServiceLoader;
import nl.elec332.minecraft.loader.impl.ElecModContainer;
import nl.elec332.minecraft.loader.impl.ElecModLoader;
import nl.elec332.minecraft.loader.mod.event.mapping.IModEventMapper;
import nl.elec332.minecraft.repackaged.net.neoforged.bus.api.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Created by Elec332 on 07-02-2024
 */
public final class ForgeModContainer extends ModContainer {

    public ForgeModContainer(IModInfo info, String className, ModFileScanData modFileScanResults, ModuleLayer gameLayer) {
        super(info);
        contextExtension = () -> null;

        elecModContainer = ElecModLoader.getModLoader().useDiscoveredMod(info.getModId(), (meta, type) -> {
            return new ElecModContainer(meta, className, name -> {
                var layer = gameLayer.findModule(info.getOwningFile().moduleName()).orElseThrow();
                return Class.forName(layer, className);
            }, (e, c) -> new ModLoadingException(info, ModLoadingStage.CONSTRUCT, e.getKey() == ElecModContainer.ErrorType.CLASSLOAD ? "fml.modloading.failedtoloadmodclass" : "fml.modloading.failedtoloadmod", e.getValue(), c));
        });
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker LOADING = MarkerManager.getMarker("LOADING");
    public final ElecModContainer elecModContainer;

    @Override
    public boolean matches(Object mod) {
        return elecModContainer.matches(mod);
    }

    @Override
    public Object getMod() {
        return elecModContainer.getMod();
    }

    @Override
    protected <T extends net.minecraftforge.eventbus.api.Event & IModBusEvent> void acceptEvent(final T e) {
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

    public static final Map<Class<? extends net.minecraftforge.eventbus.api.Event>, BiFunction<Object, IModContainer, ? extends Event>> EVENT_MAP;

    static {
        try {
            EVENT_MAP = new HashMap<>();
            ModServiceLoader.loadSingleModService(IModEventMapper.class).registerMappings(new IModEventMapper.Registry() {

                @Override
                @SuppressWarnings("unchecked")
                public <T> void register(Class<T> type, BiFunction<T, IModContainer, Event> mapper) {
                    if (!net.minecraftforge.eventbus.api.Event.class.isAssignableFrom(type)) {
                        throw new UnsupportedOperationException();
                    }
                    EVENT_MAP.put((Class<? extends net.minecraftforge.eventbus.api.Event>) type, (BiFunction<Object, IModContainer, ? extends Event>) mapper);
                }

            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
