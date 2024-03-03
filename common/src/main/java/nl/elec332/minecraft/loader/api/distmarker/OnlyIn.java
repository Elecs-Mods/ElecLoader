package nl.elec332.minecraft.loader.api.distmarker;

import java.lang.annotation.*;

/**
 * Created by Elec332 on 04-02-2024
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Repeatable(OnlyIns.class)
public @interface OnlyIn {

    Dist value();

    Class<?> _interface() default Object.class;

}
