package nl.elec332.minecraft.loader.impl.version;

import nl.elec332.minecraft.loader.api.version.IVersion;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.ArtifactVersion;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Elec332 on 22-03-2024
 */
final class ArtifactVersionImpl implements IVersion {

    ArtifactVersionImpl(String version) {
        this.impl = new DefaultArtifactVersion(version);
    }

    private final DefaultArtifactVersion impl;

    static ArtifactVersion getImpl(Object o) {
        if (o instanceof ArtifactVersionImpl) {
            return  ((ArtifactVersionImpl) o).impl;
        } else if (o instanceof ArtifactVersion) {
            return  (ArtifactVersion) o;
        } else if (o instanceof IVersion) {
            return new DefaultArtifactVersion(o.toString());
        }
        return null;
    }

    @Override
    public int compareTo(@NotNull IVersion otherVersion) {
        return impl.compareTo(getImpl(otherVersion));
    }

    @Override
    public int getMajorVersion() {
        return impl.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return impl.getMinorVersion();
    }

    @Override
    public int getIncrementalVersion() {
        return impl.getIncrementalVersion();
    }

    @Override
    public int getBuildNumber() {
        return impl.getBuildNumber();
    }

    @Override
    public String getQualifier() {
        return impl.getQualifier();
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(Object obj) {
        return impl.equals(getImpl(obj));
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    @Override
    public String toString() {
        return impl.toString();
    }

}
