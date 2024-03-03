package nl.elec332.minecraft.loader.api.modloader;

import java.util.Set;

/**
 * Created by Elec332 on 14-09-2023
 */
public interface IModContainer {

    default String getModId() {
        return getModMetadata().getModId();
    }

    IModMetaData getModMetadata();

    default IModFile getFile() {
        return getModMetadata().getModFile();
    }

    Set<String> getOwnedPackages();

    default String toInfoString() {
        return "ModContainer{" +
                " ModID: " + getModId() +
                " File: " + getFile() +
                " Packages: " + getOwnedPackages() +
                " }";
    }

}
