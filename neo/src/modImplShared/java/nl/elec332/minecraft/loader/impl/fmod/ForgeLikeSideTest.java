package nl.elec332.minecraft.loader.impl.fmod;

import nl.elec332.minecraft.loader.api.distmarker.Dist;
import nl.elec332.minecraft.loader.api.distmarker.OnlyIn;

/**
 * Created by Elec332 on 12-02-2024
 */
public class ForgeLikeSideTest {

    @OnlyIn(Dist.CLIENT)
    private static void serverTest() {
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private static void clientTest(){
    }

}
