package nl.elec332.minecraft.loader.api.version;

import nl.elec332.minecraft.loader.api.service.ModServiceLoader;

/**
 * Created by Elec332 on 22-03-2024
 * <p>
 * Factory class to provide implementations for {@link IVersion} and {@link IVersionRange}
 */
public interface IVersionFactory {

    /**
     * The provided implementation
     */
    IVersionFactory INSTANCE = ModServiceLoader.loadAPIService(IVersionFactory.class);

    /**
     * Creates a version from a string representation.
     *
     * @param version String representation of a version
     * @return A new {@link IVersion} object that represents the provided version
     */
    IVersion createVersion(String version);

    /**
     * Creates a version range from a string representation.
     *
     * @param spec String representation of a version or version range
     * @return A new {@link IVersionRange} object that represents the spec
     */
    IVersionRange createRangeFromSpec(String spec);


    /**
     * Creates a version range from a string representation of a single version.
     *
     * @param version String representation of a version
     * @return A new {@link IVersionRange} object that represents the provided version
     */
    IVersionRange createRangeFromVersion(String version);

}
