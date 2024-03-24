package nl.elec332.minecraft.loader.api.discovery;

import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Elec332 on 7-3-2016.
 * <p>
 * Used to annotate a class that implements {@link IAnnotationDataProcessor}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AnnotationDataProcessor {

    /**
     * @return The (array of) ModLoadingStage(s) in which to load this {@link IAnnotationDataProcessor}.
     */
    ModLoadingStage[] value();

    /**
     * @return Importance of this {@link IAnnotationDataProcessor}, higher = earlier processing
     */
    int importance() default -1;

}
