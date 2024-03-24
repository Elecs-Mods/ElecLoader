package nl.elec332.minecraft.loader.api.modloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Created by Elec332 on 07-03-2024
 * <p>
 * Mapping types used to mark the active mappings of the runtime or implementation.
 */
public enum MappingType {

    /**
     * Type used to mark "mojang" named mappings.
     */
    NAMED,

    /**
     * Type used to mark Forge/MCP SRG mappings.
     */
    FORGE_SRG,

    /**
     * Type used to mark fabric:intermediary mappings.
     */
    FABRIC_INTERMEDIARY
    ;

    /**
     * Prefix used to mark a mixed implementation
     */
    public static final String MIXED_PREFIX = "MIXED-";

    /**
     * Checks the provided argument and returns a default value if it is null.
     *
     * @param type The type to be checked
     * @return The provided type, or the default if the type was null
     */
    @NotNull
    public static MappingType checkValue(@Nullable MappingType type) {
        if (type == null) {
            return NAMED;
        }
        return type;
    }

    /**
     * Checks whether the provided string representation of a {@link MappingType} represents a mixed implementation.
     *
     * @param str The string to be checked
     * @return Whether the provided string represents a mixed implementation.
     */
    public static boolean isMixed(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        Objects.requireNonNull(fromString(str));
        return str.startsWith(MIXED_PREFIX);
    }

    /**
     * Returns the correct {@link MappingType} represented by the provided string
     *
     * @param str The string to be converted
     * @return The type represented by the provided string
     */
    @Nullable
    public static MappingType fromString(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        return MappingType.valueOf(str.replace(MIXED_PREFIX, ""));
    }

}
