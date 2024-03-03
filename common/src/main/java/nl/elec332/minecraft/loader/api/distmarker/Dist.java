package nl.elec332.minecraft.loader.api.distmarker;

/**
 * Created by Elec332 on 04-02-2024
 */
public enum Dist {

    CLIENT, DEDICATED_SERVER;

    public boolean isClient() {
        return this == CLIENT;
    }

    public boolean isDedicatedServer() {
        return !isClient();
    }

}
