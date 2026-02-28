package nl.elec332.minecraft.loader.api.modloader;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.*;

/**
 * Created by Elec332 on 17-09-2023
 * <p>
 * Used to represent a single file at runtime.
 */
public interface IModFile {

    /**
     * Scans this file for all (file) entries it contains.
     *
     * @param consumer The listener
     */
    default void scanFile(IModFileResource.Visitor consumer) {
        scanFile("", consumer);
    }

    /**
     * Scans this file for all (file) entries it contains, starting at the provided folder.
     * If the folder does not exist, no error is thrown and this function immediately returns
     *
     * @param startFolder The internal folder from where to start scanning
     * @param consumer    The listener
     */
    void scanFile(String startFolder, IModFileResource.Visitor consumer);

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
     * Attempts to find the provided entry in this file.
     * Does not throw an exception if the entry was not found (or if it was aa directory), but returns an empty {@link Optional} instead.
     *
     * @param file The requested resource
     * @return The path to the requested resource
     */
    Optional<IModFileResource> findResource(String file);

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
     */
     final class RawAnnotationData {

        /**
         * @param annotationType The annotation type that was found
         * @param targetType The element type marked by the annotation
         * @param classType The class the element is in
         * @param memberName The name of the member marked by the annotation
         * @param annotationData The data contained in the annotation marker
         */
        public RawAnnotationData(Type annotationType, ElementType targetType, Type classType, String memberName, Map<String, Object> annotationData) {
            this.annotationType = annotationType;
            this.targetType = targetType;
            this.classType = classType;
            this.memberName = memberName;
            this.annotationData = annotationData;
        }

        private final Type annotationType;
        private final ElementType targetType;
        private final Type classType;
        private final String memberName;
        private final Map<String, Object> annotationData;

        /**
         * @return The annotation type that was found
         */
        public Type annotationType() {
            return this.annotationType;
        }

        /**
         * @return The element type marked by the annotation
         */
        public ElementType targetType() {
            return this.targetType;
        }

        /**
         * @return The class the element is in
         */
        public Type classType() {
            return this.classType;
        }

        /**
         * @return The name of the member marked by the annotation
         */
        public String memberName() {
            return this.memberName;
        }

        /**
         * @return The data contained in the annotation marker
         */
        public Map<String, Object> annotationData() {
            return this.annotationData;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RawAnnotationData that = (RawAnnotationData) o;
            return Objects.equals(this.annotationType, that.annotationType) && this.targetType == that.targetType && Objects.equals(this.classType, that.classType) && Objects.equals(this.memberName, that.memberName) && Objects.equals(this.annotationData, that.annotationData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.annotationType, this.targetType, this.classType, this.memberName, this.annotationData);
        }

        @Override
        public String toString() {
            return "RawAnnotationData[" +
                    "annotationType=" + this.annotationType + ", " +
                    "targetType=" + this.targetType + ", " +
                    "classType=" + this.classType + ", " +
                    "memberName=" + this.memberName + ", " +
                    "annotationData=" + this.annotationData + "]";
        }

     }

    /**
     * Used to provide information about a class without actually loading it.
     */
    final class ClassData {

        /**
         * @param clazz The class type being represented
         * @param parent The parent of the represented class
         * @param interfaces The interfaces implemented by the represented class
         */
        public ClassData(Type clazz, Type parent, Set<Type> interfaces) {
            this.clazz = clazz;
            this.parent = parent;
            this.interfaces = interfaces;
        }

        private final Type clazz;
        private final Type parent;
        private final Set<Type> interfaces;

        /**
         * @return The class type being represented
         */
        public Type clazz() {
            return this.clazz;
        }

        /**
         * @return The parent of the represented class
         */
        public Type parent() {
            return this.parent;
        }

        /**
         * @return The interfaces implemented by the represented class
         */
        public Set<Type> interfaces() {
            return this.interfaces;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassData that = (ClassData) o;
            return Objects.equals(this.clazz, that.clazz) && Objects.equals(this.parent, that.parent) && Objects.equals(this.interfaces, that.interfaces);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.clazz, this.parent, this.interfaces);
        }

        @Override
        public String toString() {
            return "ClassData[" +
                    "clazz=" + this.clazz + ", " +
                    "parent=" + this.parent + ", " +
                    "interfaces=" + this.interfaces + "]";
        }

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
