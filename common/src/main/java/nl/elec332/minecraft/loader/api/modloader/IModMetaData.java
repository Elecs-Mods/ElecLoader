package nl.elec332.minecraft.loader.api.modloader;

import nl.elec332.minecraft.loader.api.version.IVersion;
import nl.elec332.minecraft.loader.api.version.IVersionRange;

/**
 * Created by Elec332 on 17-09-2023
 * <p>
 * Used to represent information about a mod from metadata
 */
public interface IModMetaData {

    /**
     * @return The id of the represented mod
     */
    String getModId();

    /**
     * @return The name of the represented mod
     */
    String getModName();

    /**
     * @return A description of the represented mod
     */
    String getDescription();

    /**
     * @return The version of the represented mod
     */
    IVersion getVersion();

    /**
     * @return The namespace of the represented mod
     */
    String getNamespace();

    /**
     * @return The file containing the represented mod
     */
    IModFile getModFile();

    /**
     * @return Descriptive string of the contained metadata
     */
    default String toInfoString() {
        return "ModMetaData{" +
                " ModID: " + getModId() +
                " ModName: " + getModName() +
                " ModDescription: " + getDescription() +
                " ModVersion: " + getVersion() +
                " Namespace: " + getNamespace() +
                " File: " + getModFile() +
                " }";
    }

    /**
     * Used to represent information about a dependency
     */
    interface Dependency {

        /**
         * @return The id of the mod this dependency represents
         */
        String getModId();

        /**
         * @return The valid version rage of this dependency
         */
        IVersionRange getVersionRange();

        /**
         * @return Whether this dependency is mandatory to be satisfied
         */
        boolean isMandatory();

    }

}
