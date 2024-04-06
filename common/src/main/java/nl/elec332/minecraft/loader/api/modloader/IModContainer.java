package nl.elec332.minecraft.loader.api.modloader;

import java.util.Set;

/**
 * Created by Elec332 on 14-09-2023
 * <p>
 * Container that wraps around the actual mod implementation
 */
public interface IModContainer {

    /**
     * @return The id of this mod
     */
    default String getModId() {
        return getModMetadata().getModId();
    }

    /**
     * @return The metadata of this mod
     */
    IModMetaData getModMetadata();

    /**
     * @return The file containing this mod
     */
    default IModFile getFile() {
        return getModMetadata().getModFile();
    }

    /**
     * @return The packages owned by this mod
     */
    Set<String> getOwnedPackages();

    /**
     * @return Descriptive string of this container
     */
    default String toInfoString() {
        return "ModContainer{" +
                " ModID: " + getModId() +
                " Metadata: " + getModMetadata() +
                " File: " + getFile() +
                " Packages: " + getOwnedPackages() +
                " }";
    }

}
