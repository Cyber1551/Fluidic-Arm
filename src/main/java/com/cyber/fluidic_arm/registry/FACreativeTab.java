package com.cyber.fluidic_arm.registry;

import com.gregtechceu.gtceu.GTCEu;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;


public class FACreativeTab {
    private static final ResourceKey<CreativeModeTab> GT_ITEM_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, GTCEu.id("item"));

    public static void init(IEventBus modBus) {
        modBus.addListener(FACreativeTab::onBuildContents);
    }

    private static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(GT_ITEM_TAB)) {
            for (int tier : FACovers.TIERS) {
                event.accept(FAItems.get(tier).get());
            }
        }
    }
}
