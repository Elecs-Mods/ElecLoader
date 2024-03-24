package nl.elec332.minecraft.loader.api.discovery;

import nl.elec332.minecraft.loader.api.modloader.IModContainer;
import nl.elec332.minecraft.loader.api.modloader.IModFile;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by Elec332 on 7-3-2016.
 * <p>
 * Handles annotation data
 */
public interface IAnnotationDataHandler {

    /**
     * Returns information on every element annotated with the provided annotation type.
     *
     * @param annotationClass The annotation type
     * @return Information on every element annotated with the provided annotation type
     */
    default Set<IAnnotationData> getAnnotationList(Class<? extends Annotation> annotationClass) {
        return getAnnotationList(Type.getType(annotationClass));
    }

    /**
     * Returns information on every element annotated with the provided annotation type.
     *
     * @param annotationType The annotation type
     * @return Information on every element annotated with the provided annotation type
     */
    Set<IAnnotationData> getAnnotationList(Type annotationType);

    /**
     * Checks whether the provided class can actually be loaded in the current environment.
     *
     * @param clazz The class to be checked
     * @return Whether the provided class can actually be loaded in the current environment
     */
    boolean hasWrongSideOnlyAnnotation(String clazz);

    /**
     * Gets data on all annotations present in the elements in the provided file.
     *
     * @param file The file to be checked for annotations
     * @return Data on all annotations present in the elements in the provided file
     */
    @NotNull
    Function<Type, Set<IAnnotationData>> getAnnotationsFor(IModFile file);

    /**
     * Gets data on all annotations present in the elements in the provided class.
     *
     * @param clazz The class to be checked for annotations
     * @return Data on all annotations present in the elements in the provided class
     */
    @NotNull
    Function<Type, Set<IAnnotationData>> getAnnotationsForClass(String clazz);

    /**
     * Gets data on all annotations present in the elements in the provided mod container.
     *
     * @param mc The mod container to be checked for annotations
     * @return Data on all annotations present in the elements in the provided mod container
     */
    Function<Type, Set<IAnnotationData>> getAnnotationsFor(IModContainer mc);

    /**
     * Returns information on every element annotated with the provided annotation type.
     * The data is presented as a map by mod container, so the data can be used to link annotation -> mod ownership.
     *
     * @param annotationClass The annotation type
     * @return Information on every element annotated with the provided annotation type by mod
     */
    default Map<IModContainer, Set<IAnnotationData>> getModdedAnnotationMap(Class<? extends Annotation> annotationClass) {
        return getModdedAnnotationMap(Type.getType(annotationClass));
    }

    /**
     * Returns information on every element annotated with the provided annotation type.
     * The data is presented as a map by mod container, so the data can be used to link annotation -> mod ownership.
     *
     * @param annotationType The annotation type
     * @return Information on every element annotated with the provided annotation type by mod
     */
    Map<IModContainer, Set<IAnnotationData>> getModdedAnnotationMap(Type annotationType);

    /**
     * Attempts to find the {@link IModContainer} that owns the element and annotation provided.
     *
     * @param annotationData The annotation data to be checked
     * @return The owner of the provided data
     */
    IModContainer findOwner(IAnnotationData annotationData);

    /**
     * Attempts to find the name of the mod that owns the element and annotation provided.
     *
     * @param annotationData The annotation data to be checked
     * @return The name of the owner of the provided data
     */
    String findOwnerName(IAnnotationData annotationData);

}
