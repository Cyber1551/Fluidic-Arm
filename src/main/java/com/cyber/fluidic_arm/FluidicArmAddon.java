package com.cyber.fluidic_arm;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

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
    public void initializeAddon() {
        FluidicArm.LOGGER.info("[{}] addon initialized", FluidicArm.MOD_ID);
    }
}
