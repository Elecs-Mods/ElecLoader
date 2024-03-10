package nl.elec332.minecraft.loader.impl;

import com.google.common.collect.Streams;
import nl.elec332.minecraft.loader.api.discovery.IAnnotationData;
import nl.elec332.minecraft.loader.api.distmarker.OnlyIn;
import nl.elec332.minecraft.loader.api.distmarker.OnlyIns;
import nl.elec332.minecraft.loader.util.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 04-02-2024
 * <p>
 * Mostly copied from: <a href="https://github.com/neoforged/FancyModLoader/blob/main/loader/src/main/java/net/neoforged/fml/common/asm/RuntimeDistCleaner.java">NeoForge</a>
 */
public final class SideCleaner implements IClassTransformer {

    public static void register(Consumer<IClassTransformer> registry, Function<Type, Set<IAnnotationData>> dataHandler) {
        Set<String> sides = new HashSet<>();
        dataHandler.apply(Type.getType(OnlyIn.class)).forEach(ad -> sides.add(ad.getClassType().getInternalName()));
        registry.accept(new SideCleaner(LogManager.getLogger("ElecLoader SideCleaner"), ElecModLoader.getDist().name(), sides));
    }

    private SideCleaner(Logger logger, String dist, Set<String> targets) {
        this.logger = logger;
        this.dist = dist;
        this.targets = Collections.unmodifiableSet(targets);
        logger.info("Initializing SideCleaner for " + this.targets.size() + " target" + (this.targets.size() == 1 ? "" : "s"));
    }

    private final Logger logger;
    private final String dist;
    private final Set<String> targets;

    private static final String ONLYIN = Type.getDescriptor(OnlyIn.class);
    private static final String ONLYINS = Type.getDescriptor(OnlyIns.class);

    @Override
    public String getName() {
        return "SideCleaner";
    }

    @Override
    public Set<String> getTargetClasses() {
        return this.targets;
    }

    @Override
    public boolean processClass(final ClassNode classNode) {
        if (dist == null) {
            throw new IllegalStateException();
        }
        AtomicBoolean changes = new AtomicBoolean();
        if (remove(classNode.visibleAnnotations, dist)) {
            logger.error("Attempted to load class {} for invalid dist {}", classNode.name, dist);
            throw new RuntimeException("Attempted to load class " + classNode.name  + " for invalid dist " + dist);
        }

        if (classNode.interfaces != null ) {
            unpack(classNode.visibleAnnotations).stream()
                    .filter(ann -> Objects.equals(ann.desc, ONLYIN))
                    .filter(ann -> ann.values.contains("_interface"))
                    .filter(ann -> !Objects.equals(((String[])ann.values.get(ann.values.indexOf("value") + 1))[1], dist))
                    .map(ann -> ((Type)ann.values.get(ann.values.indexOf("_interface") + 1)).getInternalName())
                    .forEach(intf -> {
                        if (classNode.interfaces.remove(intf)) {
                            logger.debug("Removing Interface: {} implements {}", classNode.name, intf);
                            changes.compareAndSet(false, true);
                        }
                    });

            //Remove Class level @OnlyIn/@OnlyIns annotations, this is important if anyone gets ambitious and tries to reflect an annotation with _interface set.
            if (classNode.visibleAnnotations != null) {
                Iterator<AnnotationNode> itr = classNode.visibleAnnotations.iterator();
                while (itr.hasNext()) {
                    AnnotationNode ann = itr.next();
                    if (Objects.equals(ann.desc, ONLYIN) || Objects.equals(ann.desc, ONLYINS)) {
                        logger.debug("Removing Class Annotation: {} @{}", classNode.name, ann.desc);
                        itr.remove();
                        changes.compareAndSet(false, true);
                    }
                }
            }
        }

        Iterator<FieldNode> fields = classNode.fields.iterator();
        while(fields.hasNext()) {
            FieldNode field = fields.next();
            if (remove(field.visibleAnnotations, dist)) {
                logger.debug("Removing field: {}.{}", classNode.name, field.name);
                fields.remove();
                changes.compareAndSet(false, true);
            }
        }

        LambdaGatherer lambdaGatherer = new LambdaGatherer();
        Iterator<MethodNode> methods = classNode.methods.iterator();
        while(methods.hasNext()) {
            MethodNode method = methods.next();
            if (remove(method.visibleAnnotations, dist)) {
                logger.debug("Removing method: {}.{}{}", classNode.name, method.name, method.desc);
                methods.remove();
                lambdaGatherer.accept(method);
                changes.compareAndSet(false, true);
            }
        }

        // remove dynamic synthetic lambda methods that are inside of removed methods
        for (List<Handle> dynamicLambdaHandles = lambdaGatherer.getDynamicLambdaHandles(); !dynamicLambdaHandles.isEmpty(); dynamicLambdaHandles = lambdaGatherer.getDynamicLambdaHandles()) {
            lambdaGatherer = new LambdaGatherer();
            methods = classNode.methods.iterator();
            while (methods.hasNext()) {
                MethodNode method = methods.next();
                if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) continue;
                for (Handle dynamicLambdaHandle : dynamicLambdaHandles) {
                    if (method.name.equals(dynamicLambdaHandle.getName()) && method.desc.equals(dynamicLambdaHandle.getDesc())) {
                        logger.debug("Removing lambda method: {}.{}{}", classNode.name, method.name, method.desc);
                        methods.remove();
                        lambdaGatherer.accept(method);
                        changes.compareAndSet(false, true);
                    }
                }
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<AnnotationNode> unpack(final List<AnnotationNode> anns) {
        if (anns == null) {
            return Collections.emptyList();
        }
        List<AnnotationNode> ret = anns.stream().filter(ann->Objects.equals(ann.desc, ONLYIN)).collect(Collectors.toList());
        anns.stream().filter(ann -> Objects.equals(ann.desc, ONLYINS) && ann.values != null)
                .map( ann -> (List<AnnotationNode>)ann.values.get(ann.values.indexOf("value") + 1))
                .filter(Objects::nonNull)
                .forEach(ret::addAll);
        return ret;
    }

    private static boolean remove(final List<AnnotationNode> anns, final String side) {
        return unpack(anns).stream().
                filter(ann -> Objects.equals(ann.desc, ONLYIN)).
                filter(ann -> !ann.values.contains("_interface")).
                anyMatch(ann -> !Objects.equals(((String[]) ann.values.get(ann.values.indexOf("value") + 1))[1], side));
    }

    private static class LambdaGatherer extends MethodVisitor {

        public LambdaGatherer() {
            super(Opcodes.ASM9);
        }
        private static final Handle META_FACTORY = new Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false);
        private final List<Handle> dynamicLambdaHandles = new ArrayList<>();

        public void accept(MethodNode method) {
            Streams.stream(method.instructions.iterator()).
                    filter(insnNode -> insnNode.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN).
                    forEach(insnNode -> insnNode.accept(this));
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (META_FACTORY.equals(bsm)) {
                Handle dynamicLambdaHandle = (Handle) bsmArgs[1];
                dynamicLambdaHandles.add(dynamicLambdaHandle);
            }
        }

        public List<Handle> getDynamicLambdaHandles() {
            return dynamicLambdaHandles;
        }

    }

}

