package nl.elec332.minecraft.loader.api.service;

import nl.elec332.minecraft.loader.api.modloader.IModLoader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Elec332 on 28-02-2024
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModService {

    IModLoader.LoaderType[] value();

}
