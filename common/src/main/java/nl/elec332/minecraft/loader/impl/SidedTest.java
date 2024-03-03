package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.distmarker.OnlyIn;

/**
 * Created by Elec332 on 11-02-2024
 */
final class SidedTest {

    static boolean testSide(Dist dist) {
        if (dist.isClient() && dist.isDedicatedServer()) {
            throw new IllegalArgumentException();
        }
        if (!(dist.isClient() || dist.isDedicatedServer())) {
            throw new IllegalArgumentException();
        }
        boolean correct = false;
        try {
            if (dist.isClient()) {
                clientTest();
            } else {
                serverTest();
            }
        } catch (Throwable e) {
            if (e instanceof NoSuchMethodException || e instanceof NoSuchMethodError) {
                correct = true;
            }
        }
        if (!correct) {
            return false;
        }
        try {
            if (dist.isClient()) {
                serverTest();
            } else {
                clientTest();
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }


    @OnlyIn(Dist.CLIENT)
    private static void serverTest() throws NoSuchMethodException {
        if (fClass != null) {
            fClass.getDeclaredMethod("serverTest");
        }
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private static void clientTest() throws NoSuchMethodException {
        if (fClass != null) {
            fClass.getDeclaredMethod("clientTest");
        }
    }

    private static final Class<?> fClass;

    static {
        Class<?> fClass1;
        try {
            fClass1 = Class.forName(LoaderConstants.PACKAGE_ROOT + ".impl.fmod.ForgeLikeSideTest", true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            fClass1 = null;
        }
        fClass = fClass1;
    }

}
