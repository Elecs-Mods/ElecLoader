package nl.elec332.minecraft.loader.api.modloader;

/**
 * Created by Elec332 on 14-09-2023
 */
public enum ModLoadingStage {

    PRE_CONSTRUCT("Preloaded"),
    CONSTRUCT("Constructed"),
    COMMON_SETUP("Pre-initialized"),
    SIDED_SETUP("Initialized"),
    MODCOMMS_SEND("sent IMC"),
    MODCOMMS_RECEIVE("PostInitialized"),
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
