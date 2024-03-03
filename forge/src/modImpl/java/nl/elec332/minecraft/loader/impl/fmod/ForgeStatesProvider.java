package nl.elec332.minecraft.loader.impl.fmod;

import net.minecraftforge.fml.IModLoadingState;
import net.minecraftforge.fml.IModStateProvider;
import net.minecraftforge.fml.ModLoadingPhase;
import net.minecraftforge.fml.ModLoadingState;
import nl.elec332.minecraft.loader.impl.ElecModLoader;

import java.util.List;

/**
 * Created by Elec332 on 13-02-2024
 */
public class ForgeStatesProvider implements IModStateProvider {

    @Override
    public List<IModLoadingState> getAllStates() {
        return List.of(ModLoadingState.withInline("POPULATE_MODLIST", "", ModLoadingPhase.GATHER, ml -> ElecModLoader.getModLoader().finalizeLoading()));
    }

}
