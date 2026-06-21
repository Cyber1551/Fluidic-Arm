package com.cyber.fluidic_arm.recipe;

import com.cyber.fluidic_arm.FluidicArm;
import com.cyber.fluidic_arm.config.FAConfig;
import com.cyber.fluidic_arm.registry.FAItems;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTItems.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static com.gregtechceu.gtceu.data.recipe.CustomTags.*;

public class FARecipes {
    public static void addRecipes(Consumer<FinishedRecipe> provider) {
        switch (FAConfig.RECIPE_TYPE.get()) {
            case NONE   -> { }
            case EASY   -> easyRecipes(provider);
            case NORMAL -> normalRecipes(provider);
        }
    }

    // EASY: shapeless + assembler
    private static void easyRecipes(Consumer<FinishedRecipe> provider) {
        easy(provider, LV,  "lv_fluidic_arm",  ROBOT_ARM_LV,  FLUID_REGULATOR_LV);
        easy(provider, MV,  "mv_fluidic_arm",  ROBOT_ARM_MV,  FLUID_REGULATOR_MV);
        easy(provider, HV,  "hv_fluidic_arm",  ROBOT_ARM_HV,  FLUID_REGULATOR_HV);
        easy(provider, EV,  "ev_fluidic_arm",  ROBOT_ARM_EV,  FLUID_REGULATOR_EV);
        easy(provider, IV,  "iv_fluidic_arm",  ROBOT_ARM_IV,  FLUID_REGULATOR_IV);
        easy(provider, LuV, "luv_fluidic_arm", ROBOT_ARM_LuV, FLUID_REGULATOR_LuV);
        easy(provider, ZPM, "zpm_fluidic_arm", ROBOT_ARM_ZPM, FLUID_REGULATOR_ZPM);
        easy(provider, UV,  "uv_fluidic_arm",  ROBOT_ARM_UV,  FLUID_REGULATOR_UV);
    }

    // NORMAL: shaped + assembler (LV-IV), assembly line (LuV-UV)
    private static void normalRecipes(Consumer<FinishedRecipe> provider) {
        normal(provider, LV, "lv_fluidic_arm", Tin, Steel, ELECTRIC_MOTOR_LV, ELECTRIC_PISTON_LV, FLUID_REGULATOR_LV, LV_CIRCUITS);
        normal(provider, MV, "mv_fluidic_arm", Copper, Aluminium, ELECTRIC_MOTOR_MV, ELECTRIC_PISTON_MV, FLUID_REGULATOR_MV, MV_CIRCUITS);
        normal(provider, HV, "hv_fluidic_arm", Gold, StainlessSteel, ELECTRIC_MOTOR_HV, ELECTRIC_PISTON_HV, FLUID_REGULATOR_HV, HV_CIRCUITS);
        normal(provider, EV, "ev_fluidic_arm", Aluminium, Titanium, ELECTRIC_MOTOR_EV, ELECTRIC_PISTON_EV, FLUID_REGULATOR_EV, EV_CIRCUITS);
        normal(provider, IV, "iv_fluidic_arm", Tungsten, TungstenSteel, ELECTRIC_MOTOR_IV, ELECTRIC_PISTON_IV, FLUID_REGULATOR_IV, IV_CIRCUITS);

        // LuV - scanner research
        ASSEMBLY_LINE_RECIPES.recipeBuilder(FluidicArm.id("luv_fluidic_arm"))
                .inputItems(rodLong, HSSS, 4)
                .inputItems(gear, HSSS)
                .inputItems(gearSmall, HSSS, 3)
                .inputItems(ELECTRIC_MOTOR_LuV.asStack())
                .inputItems(ELECTRIC_PISTON_LuV.asStack())
                .inputItems(LuV_CIRCUITS)
                .inputItems(IV_CIRCUITS, 2)
                .inputItems(EV_CIRCUITS, 4)
                .inputItems(FLUID_REGULATOR_LuV.asStack())
                .inputItems(cableGtSingle, NiobiumTitanium, 4)
                .inputFluids(SolderingAlloy.getFluid(L * 4))
                .inputFluids(Lubricant.getFluid(250))
                .outputItems(FAItems.get(LuV).asStack())
                .scannerResearch(b -> b
                        .researchStack(FAItems.get(IV).asStack())
                        .duration(900)
                        .EUt(1920))
                .duration(600)
                .EUt(6000)
                .save(provider);

        // ZPM - scanner research
        ASSEMBLY_LINE_RECIPES.recipeBuilder(FluidicArm.id("zpm_fluidic_arm"))
                .inputItems(rodLong, Osmiridium, 4)
                .inputItems(gear, Osmiridium)
                .inputItems(gearSmall, Osmiridium, 3)
                .inputItems(ELECTRIC_MOTOR_ZPM.asStack())
                .inputItems(ELECTRIC_PISTON_ZPM.asStack())
                .inputItems(ZPM_CIRCUITS)
                .inputItems(LuV_CIRCUITS, 2)
                .inputItems(IV_CIRCUITS, 4)
                .inputItems(FLUID_REGULATOR_ZPM.asStack())
                .inputItems(cableGtSingle, VanadiumGallium, 4)
                .inputFluids(SolderingAlloy.getFluid(L * 8))
                .inputFluids(Lubricant.getFluid(500))
                .outputItems(FAItems.get(ZPM).asStack())
                .scannerResearch(b -> b
                        .researchStack(FAItems.get(LuV).asStack())
                        .duration(1200)
                        .EUt(7680))
                .duration(600)
                .EUt(24000)
                .save(provider);

        // UV - station research
        ASSEMBLY_LINE_RECIPES.recipeBuilder(FluidicArm.id("uv_fluidic_arm"))
                .inputItems(rodLong, Tritanium, 4)
                .inputItems(gear, Tritanium)
                .inputItems(gearSmall, Tritanium, 3)
                .inputItems(ELECTRIC_MOTOR_UV.asStack())
                .inputItems(ELECTRIC_PISTON_UV.asStack())
                .inputItems(UV_CIRCUITS)
                .inputItems(ZPM_CIRCUITS, 2)
                .inputItems(LuV_CIRCUITS, 4)
                .inputItems(FLUID_REGULATOR_UV.asStack())
                .inputItems(cableGtSingle, YttriumBariumCuprate, 4)
                .inputFluids(SolderingAlloy.getFluid(L * 12))
                .inputFluids(Lubricant.getFluid(1000))
                .inputFluids(Naquadria.getFluid(L * 4))
                .outputItems(FAItems.get(UV).asStack())
                .stationResearch(b -> b
                        .researchStack(FAItems.get(ZPM).asStack())
                        .EUt(122880)
                        .CWUt(32, 128000))
                .duration(600)
                .EUt(100000)
                .save(provider);
    }

    private static void easy(Consumer<FinishedRecipe> provider, int tier, String name, ItemEntry<?> arm, ItemEntry<?> regulator) {
        var out = FAItems.get(tier).asStack();
        VanillaRecipeHelper.addShapelessRecipe(provider, FluidicArm.id(name + "_shapeless"), out, arm.asStack(), regulator.asStack());
        ASSEMBLER_RECIPES.recipeBuilder(FluidicArm.id(name + "_assembler"))
                .inputItems(arm.asStack())
                .inputItems(regulator.asStack())
                .outputItems(out)
                .duration(100)
                .EUt(30)
                .save(provider);
    }

    private static void normal(Consumer<FinishedRecipe> provider, int tier, String name, Material cable, Material rodMat, ItemEntry<?> motor, ItemEntry<?> piston, ItemEntry<?> regulator, TagKey<Item> circuit) {
        var out = FAItems.get(tier).asStack();

        VanillaRecipeHelper.addShapedRecipe(provider, FluidicArm.id(name), out,
                "WWW",
                "MSR",
                "PCS",
                'W', new MaterialEntry(cableGtSingle, cable),
                'M', motor.asStack(),
                'S', new MaterialEntry(rod, rodMat),
                'R', regulator.asStack(),
                'P', piston.asStack(),
                'C', circuit);

        ASSEMBLER_RECIPES.recipeBuilder(FluidicArm.id(name + "_assembler"))
                .inputItems(cableGtSingle, cable, 3)
                .inputItems(rod, rodMat, 2)
                .inputItems(motor.asStack())
                .inputItems(piston.asStack())
                .inputItems(regulator.asStack())
                .inputItems(circuit)
                .outputItems(out)
                .duration(100)
                .EUt(30)
                .save(provider);
    }
}
