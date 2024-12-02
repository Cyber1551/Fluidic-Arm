package com.cyber.fluidic_arm.config;

import com.cyber.fluidic_arm.FluidicArmMod;

import net.minecraftforge.common.config.Config;

@Config(modid = FluidicArmMod.MODID)
public class FluidicArmConfig {
    @Config.Comment("Recipe Type. Options: none (no generated recipes), easy (2x2 crafting), normal (3x3 crafting). Default: normal")
    @Config.RequiresMcRestart
    public static String recipeType = "normal";
}
