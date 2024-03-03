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

    default Set<IAnnotationData> getAnnotationList(Class<? extends Annotation> annotationClass) {
        return getAnnotationList(Type.getType(annotationClass));
    }

    Set<IAnnotationData> getAnnotationList(Type annotationType);

    boolean hasWrongSideOnlyAnnotation(String clazz);

    @NotNull
    Function<Type, Set<IAnnotationData>> getAnnotationsFor(IModFile file);

    @NotNull
    Function<Type, Set<IAnnotationData>> getAnnotationsForClass(String clazz);

    Function<Type, Set<IAnnotationData>> getAnnotationsFor(IModContainer mc);

    default Map<IModContainer, Set<IAnnotationData>> getModdedAnnotationMap(Class<? extends Annotation> annotationClass) {
        return getModdedAnnotationMap(Type.getType(annotationClass));
    }

    Map<IModContainer, Set<IAnnotationData>> getModdedAnnotationMap(Type annotationType);

    IModContainer deepSearchOwner(IAnnotationData annotationData);

    String deepSearchOwnerName(IAnnotationData annotationData);

}
