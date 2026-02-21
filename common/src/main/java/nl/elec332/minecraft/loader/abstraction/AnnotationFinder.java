package nl.elec332.minecraft.loader.abstraction;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.modloader.IModFile;
import org.objectweb.asm.*;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 17-09-2023
 * <p>
 * Strongly based on the annotation-finding in:
 * <a href="https://github.com/MinecraftForge/MinecraftForge/tree/1.20.x/fmlloader/src/main/java/net/minecraftforge/fml/loading/moddiscovery">FML</a>
 */
public class AnnotationFinder extends ClassVisitor {

    public AnnotationFinder() {
        super(Opcodes.ASM9);
        this.annotations = new LinkedList<>();
    }

    private Type type;
    private Type superType;
    private Set<Type> interfaces;
    private final LinkedList<AnnotationHolder> annotations;

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.type = Type.getObjectType(name);
        this.superType = superName != null && !superName.isEmpty() ? Type.getObjectType(superName) : null;
        this.interfaces = Stream.of(interfaces).map(Type::getObjectType).collect(Collectors.toSet());
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationHolder h = new AnnotationHolder(ElementType.TYPE, Type.getType(descriptor), this.type.getClassName());
        annotations.add(h);
        return new AFAnnotationVisitor(annotations, h);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return new AFFieldVisitor(annotations, name);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new AFMethodVisitor(annotations, name, descriptor);
    }

    private class AFMethodVisitor extends MethodVisitor {

        protected AFMethodVisitor(LinkedList<AnnotationHolder> annotations, String methodName, String methodDescriptor) {
            super(AnnotationFinder.this.api);
            this.annotations = annotations;
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
        }

        private final LinkedList<AnnotationHolder> annotations;
        private final String methodName;
        private final String methodDescriptor;

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationHolder ann = new AnnotationHolder(ElementType.METHOD, Type.getType(descriptor), methodName + methodDescriptor);
            annotations.addFirst(ann);
            return new AFAnnotationVisitor(annotations, ann);
        }

    }

    private class AFFieldVisitor extends FieldVisitor {

        protected AFFieldVisitor(LinkedList<AnnotationHolder> annotations, String fieldName) {
            super(AnnotationFinder.this.api);
            this.annotations = annotations;
            this.fieldName = fieldName;
        }

        private final LinkedList<AnnotationHolder> annotations;
        private final String fieldName;

        @Override
        public AnnotationVisitor visitAnnotation(String annotationName, boolean runtimeVisible) {
            AnnotationHolder ann = new AnnotationHolder(ElementType.FIELD, Type.getType(annotationName), fieldName);
            annotations.addFirst(ann);
            return new AFAnnotationVisitor(annotations, ann);
        }

    }

    private class AFAnnotationVisitor extends AnnotationVisitor {

        AFAnnotationVisitor(LinkedList<AnnotationHolder> annotations, AnnotationHolder annotation) {
            super(AnnotationFinder.this.api);
            this.annotations = annotations;
            this.annotation = annotation;
        }

        AFAnnotationVisitor(LinkedList<AnnotationHolder> annotations, AnnotationHolder annotation, String name) {
            this(annotations, annotation);
            this.array = true;
            annotation.addArray(name);
        }

        AFAnnotationVisitor(LinkedList<AnnotationHolder> annotations, AnnotationHolder annotation, boolean isSubAnnotation) {
            this(annotations, annotation);
            this.isSubAnnotation = isSubAnnotation;
        }

        private final AnnotationHolder annotation;
        private final LinkedList<AnnotationHolder> annotations;
        private boolean array;
        private boolean isSubAnnotation;

        @Override
        public void visit(String key, Object value) {
            annotation.addProperty(key, value);
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            annotation.addEnumProperty(name, desc, value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new AFAnnotationVisitor(annotations, annotation, name);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            AnnotationHolder ma = annotations.getFirst();
            final AnnotationHolder childAnnotation = ma.addChildAnnotation(name, desc);
            annotations.addFirst(childAnnotation);
            return new AFAnnotationVisitor(annotations, childAnnotation,true);
        }

        @Override
        public void visitEnd() {
            if (array) {
                annotation.endArray();
            }
            if (isSubAnnotation) {
                AnnotationHolder child = annotations.removeFirst();
                annotations.addLast(child);
            }
        }

    }

    private static class AnnotationHolder {

        AnnotationHolder(ElementType type, Type asmType, String member) {
            this.type = type;
            this.asmType = asmType;
            this.member = member;
        }

        public AnnotationHolder(Type asmType, AnnotationHolder parent) {
            this(parent.type, asmType, parent.member);
        }

        private final ElementType type;
        private final Type asmType;
        private final String member;
        private final Map<String,Object> values = Maps.newHashMap();

        private ArrayList<Object> arrayList;
        private String arrayName;

        public Map<String, Object> getValues() {
            return values;
        }

        public void addArray(String name) {
            this.arrayList = Lists.newArrayList();
            this.arrayName = name;
        }

        public void addProperty(String key, Object value) {
            if (this.arrayList != null) {
                arrayList.add(value);
            } else {
                values.put(key, value);
            }
        }

        public void addEnumProperty(String key, String enumName, String value) {
            addProperty(key, new IAnnotationData.EnumHolder(enumName, value));
        }

        public void endArray() {
            values.put(arrayName, arrayList);
            arrayList = null;
        }

        public AnnotationHolder addChildAnnotation(String name, String desc) {
            AnnotationHolder child = new AnnotationHolder(Type.getType(desc), this);
            addProperty(name, child.getValues());
            return child;
        }

        public IModFile.RawAnnotationData fromData(final Type clazz) {
            return new IModFile.RawAnnotationData(asmType, type, clazz, member, values);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper("Annotation")
                    .add("type",type)
                    .add("name",asmType.getClassName())
                    .add("member",member)
                    .add("values", values)
                    .toString();
        }

    }

    public void accumulate(Set<IModFile.ClassData> classes, Set<IModFile.RawAnnotationData> annotations) {
        classes.add(new IModFile.ClassData(type, superType, interfaces));
        annotations.addAll(this.annotations.stream().map(h -> h.fromData(this.type)).toList());
    }

}
