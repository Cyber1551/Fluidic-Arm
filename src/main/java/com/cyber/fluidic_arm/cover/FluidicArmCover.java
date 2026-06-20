package com.cyber.fluidic_arm.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;

public class FluidicArmCover extends CoverBehavior {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidicArmCover.class, CoverBehavior.MANAGED_FIELD_HOLDER);

    public final int tier;

    public FluidicArmCover(CoverDefinition coverDefinition, ICoverable coverHolder, Direction attachedSide, int tier) {
        super(coverDefinition, coverHolder, attachedSide);
        this.tier = tier;
    }

    @Override
    @MethodsReturnNonnullByDefault
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
}
