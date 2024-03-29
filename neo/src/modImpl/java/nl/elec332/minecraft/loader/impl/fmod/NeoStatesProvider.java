package nl.elec332.minecraft.loader.impl.fmod;

import net.neoforged.fml.IModLoadingState;
import net.neoforged.fml.IModStateProvider;
import net.neoforged.fml.ModLoadingPhase;
import net.neoforged.fml.ModLoadingState;
import nl.elec332.minecraft.loader.impl.ElecModLoader;

import java.util.List;

/**
 * Created by Elec332 on 13-02-2024
 */
public class NeoStatesProvider implements IModStateProvider {

    @Override
    public List<IModLoadingState> getAllStates() {
        return List.of(ModLoadingState.withInline("POPULATE_MODLIST", "", ModLoadingPhase.GATHER, ml -> ElecModLoader.getModLoader().finalizeLoading()));
    }

}
