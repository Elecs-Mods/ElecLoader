package nl.elec332.minecraft.loader.api.modloader;

/**
 * Created by Elec332 on 14-09-2023
 * <p>
 * Mod loading stages used for mod loading.
 */
public enum ModLoadingStage {

    /**
     * Special stage used before any mods have even been constructed.
     */
    PRE_CONSTRUCT("Preloaded"),

    /**
     * Stage used when constructing mods.
     */
    CONSTRUCT("Constructed"),

    /**
     * Stage used for common setup and initialization.
     */
    COMMON_SETUP("Pre-initialized"),

    /**
     * Stage used for side-specific setup and initialization.
     * @see nl.elec332.minecraft.loader.api.distmarker.Dist
     */
    SIDED_SETUP("Initialized"),

    /**
     * Stage used for sending messages to other mods.
     */
    MODCOMMS_SEND("Sent IMC"),

    /**
     * Stage used for late setup and for receiving messages from other mods.
     */
    LATE_SETUP("PostInitialized"),

    /**
     * Stage used to mark the mod loading process as completed.
     */
    COMPLETE("Completed")

    ;

    ModLoadingStage(String name) {
        this.name = name;
    }

    private final String name;

    public String getName() {
        return name;
    }

}
