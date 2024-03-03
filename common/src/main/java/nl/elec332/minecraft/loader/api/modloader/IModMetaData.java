package nl.elec332.minecraft.loader.api.modloader;

import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.ArtifactVersion;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.VersionRange;

/**
 * Created by Elec332 on 17-09-2023
 */
public interface IModMetaData {

    String getModId();

    String getModName();

    String getDescription();

    ArtifactVersion getVersion();

    String getNamespace();

    IModFile getModFile();

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

    interface Dependency {

        String getModId();

        VersionRange getVersionRange();

        boolean isMandatory();

    }

}
