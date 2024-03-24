package nl.elec332.minecraft.loader.impl.version;

import nl.elec332.minecraft.loader.api.version.IVersion;
import nl.elec332.minecraft.loader.api.version.IVersionFactory;
import nl.elec332.minecraft.loader.api.version.IVersionRange;
import nl.elec332.minecraft.repackaged.org.apache.maven.artifact.versioning.VersionRange;

/**
 * Created by Elec332 on 22-03-2024
 */
public final class VersionFactoryImpl implements IVersionFactory {

    @Override
    public IVersion createVersion(String version) {
        return new ArtifactVersionImpl(version);
    }

    @Override
    public IVersionRange createRangeFromSpec(String spec) {
        try {
            return new VersionRangeImpl(VersionRange.createFromVersionSpec(spec));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public IVersionRange createRangeFromVersion(String version) {
        return new VersionRangeImpl(VersionRange.createFromVersion(version));
    }

}
