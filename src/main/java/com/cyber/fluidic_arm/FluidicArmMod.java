package com.cyber.fluidic_arm;

import com.cyber.fluidic_arm.covers.CoverBehaviors;
import com.cyber.fluidic_arm.items.FluidicArmMetaItems;
import com.cyber.fluidic_arm.proxy.CommonProxy;

import com.cyber.fluidic_arm.recipes.FluidicArmRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import org.apache.logging.log4j.Logger;

import gregtech.api.GregTechAPI.RegisterEvent;
import gregtech.api.cover.CoverDefinition;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = FluidicArmMod.MODID, name = FluidicArmMod.NAME, version = FluidicArmMod.VERSION, dependencies = "required-after:gregtech@[2.0.0-beta,);")
public class FluidicArmMod {
	public static final String MODID = "fluidic_arm";
	public static final String NAME = "Fluidic Arm";
	public static final String VERSION = "1.0.0";

	private static Logger logger;

	@SidedProxy(modId = FluidicArmMod.MODID, clientSide = "com.cyber.fluidic_arm.proxy.ClientProxy", serverSide = "com.cyber.fluidic_arm.proxy.CommonProxy")
	public static CommonProxy proxy;


	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
		logger.info("Registering items for Fluidic Arms");
		FluidicArmMetaItems.init();
	}

	@Mod.EventBusSubscriber
	public static class RegistrationHandler {
		@SubscribeEvent
		public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
			logger.info("Registering recipes for Fluidic Arms");
			FluidicArmRecipes.init();
		}
		@SubscribeEvent
		public static void coverRegistry(RegisterEvent<CoverDefinition> event) {
			logger.info("Registering covers for Fluidic Arms");
			CoverBehaviors.init();
		}
	}
}
