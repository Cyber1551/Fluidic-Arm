package com.cyber.fluidic_arm.covers;


import com.cyber.fluidic_arm.FluidicArmMod;
import com.cyber.fluidic_arm.items.FluidicArmMetaItems;
import gregtech.api.GTValues;
import net.minecraft.util.ResourceLocation;
import static gregtech.common.covers.CoverBehaviors.registerBehavior;

public class CoverBehaviors {

    public static void init() {
        registerBehavior(new ResourceLocation(FluidicArmMod.MODID, "fluidic_arm.lv"), FluidicArmMetaItems.FLUIDIC_ARM_LV, (def, tile, side) -> new CoverFluidicArm(def, tile, side, GTValues.LV, 8, 1280));
        registerBehavior(new ResourceLocation(FluidicArmMod.MODID, "fluidic_arm.mv"), FluidicArmMetaItems.FLUIDIC_ARM_MV, (def, tile, side) -> new CoverFluidicArm(def, tile, side, GTValues.MV, 32, 1280 * 4));
        registerBehavior(new ResourceLocation(FluidicArmMod.MODID, "fluidic_arm.hv"), FluidicArmMetaItems.FLUIDIC_ARM_HV, (def, tile, side) -> new CoverFluidicArm(def, tile, side, GTValues.HV, 64, 1280 * 16));
        registerBehavior(new ResourceLocation(FluidicArmMod.MODID, "fluidic_arm.ev"), FluidicArmMetaItems.FLUIDIC_ARM_EV, (def, tile, side) -> new CoverFluidicArm(def, tile, side, GTValues.EV, 3 * 64, 1280 * 64));
        registerBehavior(new ResourceLocation(FluidicArmMod.MODID, "fluidic_arm.iv"), FluidicArmMetaItems.FLUIDIC_ARM_IV, (def, tile, side) -> new CoverFluidicArm(def, tile, side, GTValues.IV, 8 * 64, 1280 * 64 * 4));
        registerBehavior(new ResourceLocation(FluidicArmMod.MODID, "fluidic_arm.luv"), FluidicArmMetaItems.FLUIDIC_ARM_LuV, (def, tile, side) -> new CoverFluidicArm(def, tile, side, GTValues.LuV, 16 * 64, 1280 * 64 * 16));
        registerBehavior(new ResourceLocation(FluidicArmMod.MODID, "fluidic_arm.zpm"), FluidicArmMetaItems.FLUIDIC_ARM_ZPM, (def, tile, side) -> new CoverFluidicArm(def, tile, side, GTValues.ZPM, 16 * 64, 1280 * 64 * 64));
        registerBehavior(new ResourceLocation(FluidicArmMod.MODID, "fluidic_arm.uv"), FluidicArmMetaItems.FLUIDIC_ARM_UV, (def, tile, side) -> new CoverFluidicArm(def, tile, side, GTValues.UV, 16 * 64, 1280 * 64 * 4));
    }
}
