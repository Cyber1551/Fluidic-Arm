package com.cyber.fluidic_arm.recipes;

import com.cyber.fluidic_arm.config.FluidicArmConfig;
import com.cyber.fluidic_arm.items.FluidicArmMetaItems;
import gregtech.api.recipes.ModHandler;
import gregtech.api.unification.material.MarkerMaterials;
import gregtech.api.unification.stack.UnificationEntry;

import static gregtech.api.GTValues.*;
import static gregtech.api.recipes.RecipeMaps.ASSEMBLER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.ASSEMBLY_LINE_RECIPES;
import static gregtech.api.unification.material.Materials.*;
import static gregtech.api.unification.ore.OrePrefix.*;
import static gregtech.common.items.MetaItems.*;


public class FluidicArmRecipes {
    // Generates Fluidic Arm cover recipes based on the recipeType config setting. Any option other than "easy" or "normal" is ignored.
    public static void init() {
        String recipeType = FluidicArmConfig.recipeType;
        if (recipeType.equals("none")) {
            return;
        }

        if (recipeType.equals("easy")) {
            ModHandler.addShapelessRecipe("fluidic_arm.lv.easy", FluidicArmMetaItems.FLUIDIC_ARM_LV.getStackForm(),
                    ROBOT_ARM_LV.getStackForm(),
                    FLUID_REGULATOR_LV.getStackForm()
            );
            ModHandler.addShapelessRecipe("fluidic_arm.mv.easy", FluidicArmMetaItems.FLUIDIC_ARM_MV.getStackForm(),
                    ROBOT_ARM_MV.getStackForm(),
                    FLUID_REGULATOR_MV.getStackForm()
            );
            ModHandler.addShapelessRecipe("fluidic_arm.hv.easy", FluidicArmMetaItems.FLUIDIC_ARM_HV.getStackForm(),
                    ROBOT_ARM_HV.getStackForm(),
                    FLUID_REGULATOR_HV.getStackForm()
            );
            ModHandler.addShapelessRecipe("fluidic_arm.ev.easy", FluidicArmMetaItems.FLUIDIC_ARM_EV.getStackForm(),
                    ROBOT_ARM_EV.getStackForm(),
                    FLUID_REGULATOR_EV.getStackForm()
            );
            ModHandler.addShapelessRecipe("fluidic_arm.iv.easy", FluidicArmMetaItems.FLUIDIC_ARM_IV.getStackForm(),
                    ROBOT_ARM_IV.getStackForm(),
                    FLUID_REGULATOR_IV.getStackForm()
            );
            ModHandler.addShapelessRecipe("fluidic_arm.luv.easy", FluidicArmMetaItems.FLUIDIC_ARM_LuV.getStackForm(),
                    ROBOT_ARM_LuV.getStackForm(),
                    FLUID_REGULATOR_LUV.getStackForm()
            );
            ModHandler.addShapelessRecipe("fluidic_arm.zpm.easy", FluidicArmMetaItems.FLUIDIC_ARM_ZPM.getStackForm(),
                    ROBOT_ARM_ZPM.getStackForm(),
                    FLUID_REGULATOR_ZPM.getStackForm()
            );
            ModHandler.addShapelessRecipe("fluidic_arm.uv.easy", FluidicArmMetaItems.FLUIDIC_ARM_UV.getStackForm(),
                    ROBOT_ARM_UV.getStackForm(),
                    FLUID_REGULATOR_UV.getStackForm()
            );

            ASSEMBLER_RECIPES.recipeBuilder()
                    .input(ROBOT_ARM_LV)
                    .input(FLUID_REGULATOR_LV)
                    .outputs(FluidicArmMetaItems.FLUIDIC_ARM_LV.getStackForm())
                    .duration(100).EUt(VA[LV]).buildAndRegister();
            ASSEMBLER_RECIPES.recipeBuilder()
                    .input(ROBOT_ARM_MV)
                    .input(FLUID_REGULATOR_MV)
                    .outputs(FluidicArmMetaItems.FLUIDIC_ARM_MV.getStackForm())
                    .duration(100).EUt(VA[LV]).buildAndRegister();
            ASSEMBLER_RECIPES.recipeBuilder()
                    .input(ROBOT_ARM_HV)
                    .input(FLUID_REGULATOR_HV)
                    .outputs(FluidicArmMetaItems.FLUIDIC_ARM_HV.getStackForm())
                    .duration(100).EUt(VA[LV]).buildAndRegister();
            ASSEMBLER_RECIPES.recipeBuilder()
                    .input(ROBOT_ARM_EV)
                    .input(FLUID_REGULATOR_EV)
                    .outputs(FluidicArmMetaItems.FLUIDIC_ARM_EV.getStackForm())
                    .duration(100).EUt(VA[LV]).buildAndRegister();
            ASSEMBLER_RECIPES.recipeBuilder()
                    .input(ROBOT_ARM_IV)
                    .input(FLUID_REGULATOR_IV)
                    .outputs(FluidicArmMetaItems.FLUIDIC_ARM_IV.getStackForm())
                    .duration(100).EUt(VA[LV]).buildAndRegister();
            ASSEMBLER_RECIPES.recipeBuilder()
                    .input(ROBOT_ARM_LuV)
                    .input(FLUID_REGULATOR_LUV)
                    .outputs(FluidicArmMetaItems.FLUIDIC_ARM_LuV.getStackForm())
                    .duration(100).EUt(VA[LV]).buildAndRegister();
            ASSEMBLER_RECIPES.recipeBuilder()
                    .input(ROBOT_ARM_ZPM)
                    .input(FLUID_REGULATOR_ZPM)
                    .outputs(FluidicArmMetaItems.FLUIDIC_ARM_ZPM.getStackForm())
                    .duration(100).EUt(VA[LV]).buildAndRegister();
            ASSEMBLER_RECIPES.recipeBuilder()
                    .input(ROBOT_ARM_UV)
                    .input(FLUID_REGULATOR_UV)
                    .outputs(FluidicArmMetaItems.FLUIDIC_ARM_UV.getStackForm())
                    .duration(100).EUt(VA[LV]).buildAndRegister();
            return;
        }

        if (recipeType.equals("normal")) {
            ModHandler.addShapedRecipe("fluidic_arm.lv.normal", FluidicArmMetaItems.FLUIDIC_ARM_LV.getStackForm(),
                    "WWW",
                    "MSM",
                    "RCS",
                    'W', new UnificationEntry(cableGtSingle, Tin),
                    'M', ELECTRIC_MOTOR_LV.getStackForm(),
                    'S', new UnificationEntry(stick, Steel),
                    'R', FLUID_REGULATOR_LV.getStackForm(),
                    'C', new UnificationEntry(circuit, MarkerMaterials.Tier.LV)
            );
            ModHandler.addShapedRecipe("fluidic_arm.mv.normal", FluidicArmMetaItems.FLUIDIC_ARM_MV.getStackForm(),
                    "WWW",
                    "MSM",
                    "RCS",
                    'W', new UnificationEntry(cableGtSingle, Copper),
                    'M', ELECTRIC_MOTOR_MV.getStackForm(),
                    'S', new UnificationEntry(stick, Aluminium),
                    'R', FLUID_REGULATOR_MV.getStackForm(),
                    'C', new UnificationEntry(circuit, MarkerMaterials.Tier.MV)
            );
            ModHandler.addShapedRecipe("fluidic_arm.hv.normal", FluidicArmMetaItems.FLUIDIC_ARM_HV.getStackForm(),
                    "WWW",
                    "MSM",
                    "RCS",
                    'W', new UnificationEntry(cableGtSingle, Gold),
                    'M', ELECTRIC_MOTOR_HV.getStackForm(),
                    'S', new UnificationEntry(stick, StainlessSteel),
                    'R', FLUID_REGULATOR_HV.getStackForm(),
                    'C', new UnificationEntry(circuit, MarkerMaterials.Tier.HV)
            );
            ModHandler.addShapedRecipe("fluidic_arm.ev.normal", FluidicArmMetaItems.FLUIDIC_ARM_EV.getStackForm(),
                    "WWW",
                    "MSM",
                    "RCS",
                    'W', new UnificationEntry(cableGtSingle, Aluminium),
                    'M', ELECTRIC_MOTOR_EV.getStackForm(),
                    'S', new UnificationEntry(stick, Titanium),
                    'R', FLUID_REGULATOR_EV.getStackForm(),
                    'C', new UnificationEntry(circuit, MarkerMaterials.Tier.EV)
            );
            ModHandler.addShapedRecipe("fluidic_arm.iv.normal", FluidicArmMetaItems.FLUIDIC_ARM_IV.getStackForm(),
                    "WWW",
                    "MSM",
                    "RCS",
                    'W', new UnificationEntry(cableGtSingle, Tungsten),
                    'M', ELECTRIC_MOTOR_IV.getStackForm(),
                    'S', new UnificationEntry(stick, TungstenSteel),
                    'R', FLUID_REGULATOR_IV.getStackForm(),
                    'C', new UnificationEntry(circuit, MarkerMaterials.Tier.IV)
            );

            ASSEMBLY_LINE_RECIPES.recipeBuilder()
                    .input(stickLong, HSSS, 4)
                    .input(gear, HSSS)
                    .input(gearSmall, HSSS, 3)
                    .input(ELECTRIC_MOTOR_LuV, 2)
                    .input(FLUID_REGULATOR_LUV)
                    .input(circuit, MarkerMaterials.Tier.LuV)
                    .input(circuit, MarkerMaterials.Tier.IV, 2)
                    .input(circuit, MarkerMaterials.Tier.EV, 4)
                    .input(cableGtSingle, NiobiumTitanium)
                    .fluidInputs(SolderingAlloy.getFluid(L * 4))
                    .fluidInputs(Lubricant.getFluid(250))
                    .output(FluidicArmMetaItems.FLUIDIC_ARM_LuV)
                    .scannerResearch(b -> b
                            .researchStack(FluidicArmMetaItems.FLUIDIC_ARM_IV.getStackForm())
                            .duration(1200)
                            .EUt(VA[HV]))
                    .duration(600).EUt(6000).buildAndRegister();

            ASSEMBLY_LINE_RECIPES.recipeBuilder()
                    .input(stickLong, Osmiridium, 4)
                    .input(gear, Osmiridium)
                    .input(gearSmall, Osmiridium, 3)
                    .input(ELECTRIC_MOTOR_ZPM, 2)
                    .input(FLUID_REGULATOR_ZPM)
                    .input(circuit, MarkerMaterials.Tier.ZPM)
                    .input(circuit, MarkerMaterials.Tier.LuV, 2)
                    .input(circuit, MarkerMaterials.Tier.IV, 4)
                    .input(cableGtSingle, VanadiumGallium)
                    .fluidInputs(SolderingAlloy.getFluid(L * 8))
                    .fluidInputs(Lubricant.getFluid(500))
                    .output(FluidicArmMetaItems.FLUIDIC_ARM_ZPM)
                    .scannerResearch(b -> b
                            .researchStack(FluidicArmMetaItems.FLUIDIC_ARM_LuV.getStackForm())
                            .duration(1200)
                            .EUt(VA[IV]))
                    .duration(600).EUt(24000).buildAndRegister();

            ASSEMBLY_LINE_RECIPES.recipeBuilder()
                    .input(stickLong, Tritanium, 4)
                    .input(gear, Tritanium)
                    .input(gearSmall, Tritanium, 3)
                    .input(ELECTRIC_MOTOR_UV, 2)
                    .input(FLUID_REGULATOR_UV)
                    .input(circuit, MarkerMaterials.Tier.UV)
                    .input(circuit, MarkerMaterials.Tier.ZPM, 2)
                    .input(circuit, MarkerMaterials.Tier.LuV, 4)
                    .input(cableGtSingle, YttriumBariumCuprate)
                    .fluidInputs(SolderingAlloy.getFluid(L * 12))
                    .fluidInputs(Lubricant.getFluid(1000))
                    .fluidInputs(Naquadria.getFluid(L * 4))
                    .output(FluidicArmMetaItems.FLUIDIC_ARM_UV)
                    .stationResearch(b -> b
                            .researchStack(FluidicArmMetaItems.FLUIDIC_ARM_ZPM.getStackForm())
                            .EUt(122880)
                            .CWUt(32, 128000))
                    .duration(600).EUt(100000).buildAndRegister();

        }
    }
}
