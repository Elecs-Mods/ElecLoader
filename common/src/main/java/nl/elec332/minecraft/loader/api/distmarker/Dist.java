package nl.elec332.minecraft.loader.api.distmarker;

/**
 * Created by Elec332 on 04-02-2024
 * <p>
 * Type used to mark the distribution of the game.
 * The client distribution has a copy of the logical client and the logical server, while the dedicated server distribution only has the logical server.
 */
public enum Dist {

    /**
     * Type used to represent the client distribution of the game.
     */
    CLIENT,

    /**
     * Type used to represent the server distribution of the game.
     */
    DEDICATED_SERVER
    ;

    /**
     * @return If this marks a client
     */
    public boolean isClient() {
        return this == CLIENT;
    }

    /**
     * @return If this marks a dedicated server
     */
    public boolean isDedicatedServer() {
        return !isClient();
    }

}
