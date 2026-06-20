package com.cyber.fluidic_arm.registry;

import com.cyber.fluidic_arm.FluidicArm;
import com.cyber.fluidic_arm.cover.FluidicArmCover;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.IOCoverRenderer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FACovers {

    // Technically, scaling caps at LuV. However, I'm adding up to UV for future proofing
    public static final int[] TIERS = {
            GTValues.LV,
            GTValues.MV,
            GTValues.HV,
            GTValues.EV,
            GTValues.IV,
            GTValues.LuV,
            GTValues.ZPM,
            GTValues.UV
    };

    private static final Map<Integer, CoverDefinition> DEFINITIONS = new HashMap<>();

    static {
        for (var tier : TIERS) {
            var id = FluidicArm.id("fluidic_arm." + GTValues.VN[tier].toLowerCase(Locale.ROOT));
            DEFINITIONS.put(tier, new CoverDefinition(id,
                    (coverDefinition, coverHolder, attachedSide) -> new FluidicArmCover(coverDefinition, coverHolder, attachedSide, tier),
                    () -> () -> IOCoverRenderer.PUMP_LIKE_COVER_RENDERER));
        }
    }

    public static CoverDefinition get(int tier) { return DEFINITIONS.get(tier); }

    public static void init() {
        for (var tier : TIERS) {
            var definition = DEFINITIONS.get(tier);
            GTRegistries.COVERS.register(definition.getId(), definition);
        }
    }
}
