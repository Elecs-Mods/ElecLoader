package nl.elec332.minecraft.loader.impl;

import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import net.neoforged.neoforgespi.transformation.ProcessorName;

/**
 * Created by Elec332 on 07-02-2026
 */
public class NeoForgeClassProcessor implements ClassProcessorProvider {

    @Override
    public void createProcessors(Context context, Collector collector) {
        LoaderInitializer.INSTANCE.startLoader(t -> {
            ProcessorName name = new ProcessorName("elecloader", t.getName());
            collector.add(new ClassProcessor() {

                @Override
                public ProcessorName name() {
                    return name;
                }

                @Override
                public boolean handlesClass(SelectionContext context) {
                    return t.getTargetClasses().contains(context.type().getInternalName());
                }

                @Override
                public ComputeFlags processClass(TransformationContext context) {
                    if (t.processClass(context.node())) {
                        return ComputeFlags.COMPUTE_FRAMES;
                    }
                    return ComputeFlags.NO_REWRITE;
                }

            });
        });
    }

}
