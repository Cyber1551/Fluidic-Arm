package com.cyber.fluidic_arm.registry;

import com.cyber.fluidic_arm.FluidicArm;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

public class FAItems {
    public static final ItemEntry<ComponentItem> FLUIDIC_ARM_LV = FluidicArm.REGISTRATE.item("lv_fluidic_arm", ComponentItem::create)
            .lang("LV Fluidic Arm")
            .onRegister(attach(new CoverPlaceBehavior(FACovers.FLUIDIC_ARM_LV)))
            .register();

    private static NonNullConsumer<ComponentItem> attach(IItemComponent component) {
        return item -> item.attachComponents(component);
    }

    // no-op
    public static void init() { }
}
