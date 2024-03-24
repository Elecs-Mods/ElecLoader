package nl.elec332.minecraft.loader.api.distmarker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Elec332 on 04-02-2024
 * <p>
 * Java 8 Repeatable container for the {@link OnlyIn} annotation.
 * Only usable on type definitions, and only meaningful when interface value is specified in {@link OnlyIn#_interface()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OnlyIns {

    OnlyIn[] value();

}
