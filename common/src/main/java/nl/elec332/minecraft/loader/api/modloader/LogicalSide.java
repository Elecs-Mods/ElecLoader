package nl.elec332.minecraft.loader.api.modloader;

/**
 * Created by Elec332 on 14-09-2023
 * <p>
 * Type used to mark the logical side of the game.
 * The client distribution has a copy of the logical client and the logical server, while the dedicated server distribution only has the logical server.
 * @see nl.elec332.minecraft.loader.api.distmarker.Dist
 */
public enum LogicalSide {

    /**
     * Type used to represent the logical client of the game.
     * The logical client is only shipped with the {@link nl.elec332.minecraft.loader.api.distmarker.Dist#CLIENT} distribution of the game.
     */
    CLIENT,

    /**
     * Type used to represent the logical server of the game.
     * The logical server is shipped with both the {@link nl.elec332.minecraft.loader.api.distmarker.Dist#CLIENT} and {@link nl.elec332.minecraft.loader.api.distmarker.Dist#DEDICATED_SERVER} distributions.
     * The client distribution runs the logical server for singleplayer mode and LAN play.
     */
    SERVER
    ;

    /**
     * @return If this marks a logical client
     */
    public boolean isClient() {
        return this == CLIENT;
    }


    /**
     * @return If this marks a logical server
     */
    public boolean isDedicatedServer() {
        return !isClient();
    }

}
