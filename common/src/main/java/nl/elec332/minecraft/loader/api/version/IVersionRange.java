package nl.elec332.minecraft.loader.api.version;

/**
 * Created by Elec332 on 22-03-2024
 */
public interface IVersionRange {

    IVersionRange UNBOUNDED = IVersionFactory.INSTANCE.createRangeFromSpec(" ");

    /**
     * Checks if the provided version is valid in this range.
     *
     * @param version The version to be checked
     * @return Whether the provided version falls within this version range and its restrictions
     */
    boolean containsVersion(IVersion version);

}
