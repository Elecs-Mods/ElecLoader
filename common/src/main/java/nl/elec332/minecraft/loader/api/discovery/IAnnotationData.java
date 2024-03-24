package nl.elec332.minecraft.loader.api.discovery;

import nl.elec332.minecraft.loader.api.modloader.IModFile;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by Elec332 on 29-10-2016.
 * <p>
 * Works like the legacy ASMData with more features for easier handling
 */
public interface IAnnotationData {

    /**
     * @return The file containing the annotated element
     */
    IModFile getFile();

    /**
     * @return The name of the annotation described by this object
     */
    default String getAnnotationName() {
        return getAnnotationType().toString();
    }

    /**
     * @return The type of the annotation described by this object
     */
    Type getAnnotationType();

    /**
     * @return The actual data contained in the annotation described by this object
     *          Values will be {@link String}'s or {@link EnumHolder}'s
     */
    Map<String, Object> getAnnotationInfo();

    /**
     * @return The name of the class containing the element annotated with the annotation described by this object
     *
     * @see IAnnotationData#getClassType()
     */
    default String getClassName() {
        return getClassType().toString();
    }


    /**
     * Tries to load the class described by {@link IAnnotationData#getClassType()}, will throw an exception upon failure.
     * If it is detected that this class may be illegal to load this method will return null instead.
     *
     * @return The loaded class containing the annotated element
     */
    default Class<?> tryLoadClass() {
        if (hasWrongSideOnlyAnnotation()) {
            return null;
        }
        return loadClass();
    }

    /**
     * Tries to load the class described by {@link IAnnotationData#getClassType()}, will throw an exception upon failure.
     *
     * @return The loaded class containing the annotated element
     */
    Class<?> loadClass();

    /**
     * @return The class type containing the element annotated with the annotation described by this object
     *          (Can be the class itself, see {@link IAnnotationData#isClass()}, {@link IAnnotationData#isMethod()}, {@link IAnnotationData#isField()})
     */
    Type getClassType();

    /**
     * @return The name of the member annotated with the annotation described by this object
     */
    String getMemberName();

    /**
     * @return Whether the annotated member is the class itself.
     */
    boolean isClass();

    /**
     * @return Whether the annotated member is a field.
     */
    boolean isField();

    /**
     * @return Field name of the annotated field. Throws an exception if the member is not a field.
     */
    String getFieldName();

    /**
     * @return Field reference to the annotated field. Throws an exception if the member is not a field.
     */
    Field getField();

    /**
     * @return Field type of the annotated field. Throws an exception if the member is not a field.
     */
    Class<?> getFieldType();

    /**
     * @return Whether the annotated member is a method.
     */
    boolean isMethod();

    /**
     * @return Method name of the annotated method. Throws an exception if the member is not a method.
     */
    String getMethodName();

    /**
     * @return Method reference of the annotated method. Throws an exception if the member is not a method.
     */
    Method getMethod();

    /**
     * @return Method parameter types of the annotated method. Throws an exception if the member is not a method.
     */
    Type[] getMethodParameterTypes();

    /**
     * @return Method parameters of the annotated method. Throws an exception if the member is not a method.
     */
    Class<?>[] getMethodParameters();

    /**
     * @return Whether the class containing the annotated member is marked
     *          (by an annotation like {@link nl.elec332.minecraft.loader.api.distmarker.OnlyIn} or otherwise)
     *          as being incompatible with the current environment.
     */
    boolean hasWrongSideOnlyAnnotation();

    /**
     * Record holding Enum data from annotations
     *
     * @param desc The enum description
     * @param value The enum value (name)
     */
    record EnumHolder(String desc, String value) {
    }

}
