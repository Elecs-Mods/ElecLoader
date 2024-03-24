package nl.elec332.minecraft.loader.api.modloader;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 17-09-2023
 * <p>
 * Used to represent a single file at runtime.
 */
public interface IModFile {

    /**
     * Scans this file for all classes and packages.
     *
     * @param consumer The listener
     */
    void scanFile(Consumer<Path> consumer);

    /**
     * @return A list about the mods contained in this file,
     */
    List<IModMetaData> getMods();

    /**
     * @return The packages contained in this file.
     */
    Set<String> getPackages();

    /**
     * @return The names of the classes contained in this file.
     */
    Set<String> getClassFiles();

    /**
     * @return The annotation data of all classes contained in this file.
     */
    Set<RawAnnotationData> getAnnotations();

    /**
     * @return Information about the individual classes contained in this file.
     */
    Set<ClassData> getClasses();

    /**
     * Attempts to find the provided path in this file.
     * Does not throw an exception if the path was not found, but returns an empty {@link Optional} instead.
     *
     * @param file The requested resource
     * @return The path to the requested resource
     */
    Optional<Path> findPath(String file);

    /**
     * @return The path of the first root file.
     */
    @Nullable
    String getComparableRootPath();

    /**
     * @return The root path of this file.
     *
     * NOTE: Probably not resolvable! Use only for informational purposes!
     */
    String getRootFileString();

    /**
     * Used to provide information about annotations without actually loading any classes.
     *
     * @param annotationType The annotation type that was found
     * @param targetType The element type marked by the annotation
     * @param classType The class the element is in
     * @param memberName The name of the member marked by the annotation
     * @param annotationData The data contained in the annotation marker
     */
    record RawAnnotationData(Type annotationType, ElementType targetType, Type classType, String memberName, Map<String, Object> annotationData) {
    }

    /**
     * Used to provide information about a class without actually loading it.
     *
     * @param clazz The class type being represented
     * @param parent The parent of the represented class
     * @param interfaces The interfaces implemented by the represented class
     */
    record ClassData(Type clazz, Type parent, Set<Type> interfaces) {
    }

    /**
     * Used for an implementation that also has information about all the contained files.
     */
    interface FileLister extends IModFile {

        /**
         * @return All the files (not just classes) contained in this file.
         */
        Set<String> getFiles();

    }

}
