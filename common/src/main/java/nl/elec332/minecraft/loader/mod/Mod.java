package nl.elec332.minecraft.loader.mod;

import nl.elec332.minecraft.loader.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Elec332 on 05-02-2024
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {

    String value();


    Dist[] dist() default {Dist.CLIENT, Dist.DEDICATED_SERVER};

}
