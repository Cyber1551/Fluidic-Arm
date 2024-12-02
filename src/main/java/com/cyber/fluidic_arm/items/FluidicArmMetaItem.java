package com.cyber.fluidic_arm.items;

import gregtech.api.items.metaitem.StandardMetaItem;
import net.minecraft.client.resources.I18n;
import gregtech.common.items.behaviors.TooltipBehavior;

public class FluidicArmMetaItem extends StandardMetaItem {
    public FluidicArmMetaItem() {
        super();
    }

    @Override
    public void registerSubItems() {
        int startID = 0;

        FluidicArmMetaItems.FLUIDIC_ARM_LV = addItem(startID++, "fluidic_arm.lv").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("metaitem.fluidic_arm.tooltip"));
            lines.add(I18n.format("gregtech.universal.tooltip.item_transfer_rate", 8));
            lines.add(I18n.format("gregtech.universal.tooltip.fluid_transfer_rate", 1280 / 20));
        }));
        FluidicArmMetaItems.FLUIDIC_ARM_MV = addItem(startID++, "fluidic_arm.mv").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("metaitem.fluidic_arm.tooltip"));
            lines.add(I18n.format("gregtech.universal.tooltip.item_transfer_rate", 32));
            lines.add(I18n.format("gregtech.universal.tooltip.fluid_transfer_rate", 1280 * 4 / 20));
        }));
        FluidicArmMetaItems.FLUIDIC_ARM_HV = addItem(startID++, "fluidic_arm.hv").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("metaitem.fluidic_arm.tooltip"));
            lines.add(I18n.format("gregtech.universal.tooltip.item_transfer_rate", 64));
            lines.add(I18n.format("gregtech.universal.tooltip.fluid_transfer_rate", 1280 * 16 / 20));
        }));
        FluidicArmMetaItems.FLUIDIC_ARM_EV = addItem(startID++, "fluidic_arm.ev").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("metaitem.fluidic_arm.tooltip"));
            lines.add(I18n.format("gregtech.universal.tooltip.item_transfer_rate_stacks", 3));
            lines.add(I18n.format("gregtech.universal.tooltip.fluid_transfer_rate", 1280 * 64 / 20));
        }));
        FluidicArmMetaItems.FLUIDIC_ARM_IV = addItem(startID++, "fluidic_arm.iv").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("metaitem.fluidic_arm.tooltip"));
            lines.add(I18n.format("gregtech.universal.tooltip.item_transfer_rate_stacks", 8));
            lines.add(I18n.format("gregtech.universal.tooltip.fluid_transfer_rate", 1280 * 64 * 4 / 20));
        }));
        FluidicArmMetaItems.FLUIDIC_ARM_LuV = addItem(startID++, "fluidic_arm.luv").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("metaitem.fluidic_arm.tooltip"));
            lines.add(I18n.format("gregtech.universal.tooltip.item_transfer_rate_stacks", 16));
            lines.add(I18n.format("gregtech.universal.tooltip.fluid_transfer_rate", 1280 * 64 * 16 / 20));
        }));
        FluidicArmMetaItems.FLUIDIC_ARM_ZPM = addItem(startID++, "fluidic_arm.zpm").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("metaitem.fluidic_arm.tooltip"));
            lines.add(I18n.format("gregtech.universal.tooltip.item_transfer_rate_stacks", 16));
            lines.add(I18n.format("gregtech.universal.tooltip.fluid_transfer_rate", 1280 * 64 * 64 / 20));
        }));
        FluidicArmMetaItems.FLUIDIC_ARM_UV = addItem(startID++, "fluidic_arm.uv").addComponents(new TooltipBehavior(lines -> {
            lines.add(I18n.format("metaitem.fluidic_arm.tooltip"));
            lines.add(I18n.format("gregtech.universal.tooltip.item_transfer_rate_stacks", 16));
            lines.add(I18n.format("gregtech.universal.tooltip.fluid_transfer_rate", 1280 * 64 * 64 * 4 / 20));
        }));
    }
}
