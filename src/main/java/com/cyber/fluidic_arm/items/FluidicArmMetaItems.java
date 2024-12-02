package com.cyber.fluidic_arm.items;

import gregtech.api.items.metaitem.MetaItem;

public class FluidicArmMetaItems {
    public static MetaItem<?>.MetaValueItem FLUIDIC_ARM_LV;
    public static MetaItem<?>.MetaValueItem FLUIDIC_ARM_MV;
    public static MetaItem<?>.MetaValueItem FLUIDIC_ARM_HV;
    public static MetaItem<?>.MetaValueItem FLUIDIC_ARM_EV;
    public static MetaItem<?>.MetaValueItem FLUIDIC_ARM_IV;
    public static MetaItem<?>.MetaValueItem FLUIDIC_ARM_LuV;
    public static MetaItem<?>.MetaValueItem FLUIDIC_ARM_ZPM;
    public static MetaItem<?>.MetaValueItem FLUIDIC_ARM_UV;


    public static void init() {
        FluidicArmMetaItem fluidicArmMetaItem = new FluidicArmMetaItem();
        fluidicArmMetaItem.setRegistryName("fluidic_arm_meta_item");
    }
}
