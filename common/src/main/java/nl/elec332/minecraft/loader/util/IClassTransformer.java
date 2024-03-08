package nl.elec332.minecraft.loader.util;

import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

/**
 * Created by Elec332 on 07-03-2024
 */
public interface IClassTransformer {

    String getName();

    Set<String> getTargetClasses();

    boolean processClass(final ClassNode classNode);

}
