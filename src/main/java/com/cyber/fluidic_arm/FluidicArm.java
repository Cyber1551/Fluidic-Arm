package com.cyber.fluidic_arm;

import com.cyber.fluidic_arm.registry.FACreativeTab;
import com.cyber.fluidic_arm.registry.FAItems;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FluidicArm.MOD_ID)
public class FluidicArm {

    public static final String MOD_ID = "fluidic_arm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final GTRegistrate REGISTRATE = GTRegistrate.create(MOD_ID);

    public FluidicArm() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        REGISTRATE.registerRegistrate();
        modBus.addListener(this::commonSetup);
        LOGGER.debug("[{}] constructed", MOD_ID);
        FAItems.init();
        FACreativeTab.init(modBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) { }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
