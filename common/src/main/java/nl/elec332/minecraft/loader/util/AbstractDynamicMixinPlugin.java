package nl.elec332.minecraft.loader.util;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 04-02-2024
 */
public abstract class AbstractDynamicMixinPlugin implements IMixinConfigPlugin {

    private final Map<String, IClassTransformer> allMixins = new HashMap<>();

    @Override
    public final void onLoad(String rawMixinPackage) {
        try {
            Map<String, IClassTransformer> mixins = new HashMap<>();
            String mixinPackage = rawMixinPackage.replace('.', '/');
            addTransformers(r -> mixins.put(mixinPackage + "/GeneratedMixinClass_" + r.getName(), r));
            for (final var entry : mixins.entrySet()) {
                defineClass(entry.getKey(), makeMixinBlob(entry.getKey(), entry.getValue().getTargetClasses()));
                allMixins.put(entry.getKey().replace("/", "."), entry.getValue());
            }
        } catch (Exception e) {
            throw onLoadFailed(e);
        }
    }

    protected abstract void addTransformers(Consumer<IClassTransformer> registry);

    protected abstract void defineClass(String name, byte[] data) throws Exception;

    protected RuntimeException onLoadFailed(Exception source) {
        return new RuntimeException(source);
    }

    @Override
    public final String getRefMapperConfig() {
        return null;
    }

    @Override
    public final boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public final void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public final List<String> getMixins() {
        return allMixins.keySet().stream()
                .map(s -> s.substring(s.lastIndexOf(".") + 1))
                .collect(Collectors.toList());
    }

    @Override
    public final void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        Objects.requireNonNull(this.allMixins.get(mixinClassName)).processClass(targetClass);
    }


    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static byte[] makeMixinBlob(String name, Collection<? extends String> targets) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(52, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE, name, null, "java/lang/Object", null);
        AnnotationVisitor mixinAnnotation = cw.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;", false);
        AnnotationVisitor targetAnnotation = mixinAnnotation.visitArray("value");
        for (String target : targets) {
            targetAnnotation.visit(null, Type.getType('L' + target + ';'));
        }
        targetAnnotation.visitEnd();
        mixinAnnotation.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

}