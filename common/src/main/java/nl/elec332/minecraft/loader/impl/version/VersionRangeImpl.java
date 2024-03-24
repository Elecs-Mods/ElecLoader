package nl.elec332.minecraft.loader.impl.version;

import nl.elec332.minecraft.loader.api.version.IVersion;
import nl.elec332.minecraft.loader.api.version.IVersionRange;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.VersionRange;

/**
 * Created by Elec332 on 22-03-2024
 */
final class VersionRangeImpl implements IVersionRange {

    VersionRangeImpl(VersionRange impl) {
        this.impl = impl;
    }

    private final VersionRange impl;

    @Override
    public boolean containsVersion(IVersion version) {
        return impl.containsVersion(ArtifactVersionImpl.getImpl(version));
    }

    @Override
    public String toString() {
        return impl.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VersionRangeImpl) {
            return impl.equals(((VersionRangeImpl) obj).impl);
        } else if (obj instanceof VersionRange) {
            return impl.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

}
