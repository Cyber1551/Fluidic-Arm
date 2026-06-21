package com.cyber.fluidic_arm;

import com.cyber.fluidic_arm.recipe.FARecipes;
import com.cyber.fluidic_arm.registry.FACovers;
import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

@GTAddon
public class FluidicArmAddon implements IGTAddon {
    @Override
    public GTRegistrate getRegistrate() {
        return FluidicArm.REGISTRATE;
    }

    @Override
    public String addonModId() {
        return FluidicArm.MOD_ID;
    }

    @Override
    public void registerCovers() {
        FACovers.init();
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        FARecipes.addRecipes(provider);
    }

    @Override
    public void initializeAddon() {
        FluidicArm.LOGGER.info("[{}] addon initialized", FluidicArm.MOD_ID);
    }
}
