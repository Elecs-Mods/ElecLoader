package nl.elec332.minecraft.loader.api.modloader;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 17-09-2023
 */
public interface IModFile {

    void scanFile(Consumer<Path> consumer);

    List<IModMetaData> getMods();

    Set<String> getPackages();

    Set<String> getClassFiles();

    Set<RawAnnotationData> getAnnotations();

    Set<ClassData> getClasses();

    @Nullable
    String getComparableRootPath();

    String getRootFileString();

    record RawAnnotationData(Type annotationType, ElementType targetType, Type classType, String memberName, Map<String, Object> annotationData) {
    }

    record ClassData(Type clazz, Type parent, Set<Type> interfaces) {
    }

    interface FileLister extends IModFile {

        Set<String> getFiles();

    }

}
