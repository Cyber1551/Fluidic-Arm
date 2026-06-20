package com.cyber.fluidic_arm.registry;

import com.cyber.fluidic_arm.FluidicArm;
import com.cyber.fluidic_arm.cover.FluidicArmCover;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.IOCoverRenderer;

public class FACovers {
    public static final CoverDefinition FLUIDIC_ARM_LV = new CoverDefinition(FluidicArm.id("fluidic_arm.lv"),
            (coverDefinition, coverHolder, attachedSide) -> new FluidicArmCover(coverDefinition, coverHolder, attachedSide, GTValues.LV),
            () -> () -> IOCoverRenderer.PUMP_LIKE_COVER_RENDERER);

    public static void init() {
        GTRegistries.COVERS.register(FLUIDIC_ARM_LV.getId(), FLUIDIC_ARM_LV);
    }
}
