package com.cyber.fluidic_arm.registry;

import com.cyber.fluidic_arm.FluidicArm;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FAItems {
    private static final Map<Integer, ItemEntry<ComponentItem>> FLUIDIC_ARMS = new HashMap<>();

    static {
        for (int tier : FACovers.TIERS) {
            var name = GTValues.VN[tier].toLowerCase(Locale.ROOT) + "_fluidic_arm"; // lv_fluidic_arm
            FLUIDIC_ARMS.put(tier, FluidicArm.REGISTRATE
                    .item(name, ComponentItem::create)
                    .lang(GTValues.VN[tier] + " Fluidic Arm") // "LV Fluidic Arm"
                    .onRegister(attach(new CoverPlaceBehavior(FACovers.get(tier))))
                    .register());
        }
    }

    public static ItemEntry<ComponentItem> get(int tier) { return FLUIDIC_ARMS.get(tier); }

    private static NonNullConsumer<ComponentItem> attach(IItemComponent component) {
        return item -> item.attachComponents(component);
    }

    // no-op
    public static void init() { }
}
