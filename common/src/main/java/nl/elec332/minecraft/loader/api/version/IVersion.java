package nl.elec332.minecraft.loader.api.version;

/**
 * Created by Elec332 on 22-03-2024
 * <p>
 * Describes a version in terms of its components.
 */
public interface IVersion extends Comparable<IVersion> {

    int getMajorVersion();

    int getMinorVersion();

    int getIncrementalVersion();

    int getBuildNumber();

    String getQualifier();

}
