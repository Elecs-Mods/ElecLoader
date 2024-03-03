package nl.elec332.minecraft.loader.api.discovery;

import nl.elec332.minecraft.loader.api.modloader.IModFile;
import org.jetbrains.annotations.NotNull;
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

    IModFile getFile();

    default String getAnnotationName() {
        return getAnnotationType().toString();
    }

    Type getAnnotationType();

    Map<String, Object> getAnnotationInfo();

    default String getClassName() {
        return getClassType().getClassName();
    }

    @NotNull
    default Class<?> tryLoadClass() {
        if (hasWrongSideOnlyAnnotation()) {
            return null;
        }
        return loadClass();
    }

    Class<?> loadClass();

    Type getClassType();

    String getMemberName();

    boolean isClass();

    boolean isField();

    String getFieldName();

    Field getField();

    Class<?> getFieldType();

    boolean isMethod();

    String getMethodName();

    Method getMethod();

    Type[] getMethodParameterTypes();

    Class<?>[] getMethodParameters();

    boolean hasWrongSideOnlyAnnotation();

    record EnumHolder(String desc, String value) {
    }

}
