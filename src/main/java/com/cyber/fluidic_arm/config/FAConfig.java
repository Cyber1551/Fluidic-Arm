package com.cyber.fluidic_arm.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class FAConfig {
    public enum RecipeType {
        NONE,
        EASY,
        NORMAL
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<RecipeType> RECIPE_TYPE;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        RECIPE_TYPE = builder.comment(
                "Which recipes to generate for the Fluidic Arm covers.",
                "",
                "  NONE = no recipes (define your own in a pack)",
                "  EASY = shapeless + assembler, all tiers",
                "  NORMAL = crafting + assembler for LV-IV, assembly line for LuV-UV",
                "", "Default: NORMAL"
            ).defineEnum("recipeType", RecipeType.NORMAL);
        builder.pop();
        SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
