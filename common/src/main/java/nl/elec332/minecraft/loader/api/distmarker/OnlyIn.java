package nl.elec332.minecraft.loader.api.distmarker;

import java.lang.annotation.*;

/**
 * Created by Elec332 on 04-02-2024
 * <p>
 * Marks the associated element as being only available on a certain Dist.
 * Classes, fields, methods and constructors can be marked as only available in a specific distribution based on the presence of this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Repeatable(OnlyIns.class)
public @interface OnlyIn {

    Dist value();

    /**
     * If used on a class, this can be used to specify to remove an interface from a class.
     */
    Class<?> _interface() default Object.class;

}
