package com.cyber.fluidic_arm.cover.transfer;

import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraftforge.fluids.FluidStack;

public class FluidTransfer {
    private FluidTransfer() { }

    public enum TransferDirection {
        INSERT,
        EXTRACT,
    }

    public static Object2LongMap<FluidStack> enumerateDistinctFluids(IFluidHandlerModifiable handler, TransferDirection direction) {
        var summed = new Object2LongOpenHashMap<FluidStack>();
        for (var tank = 0; tank < handler.getTanks(); tank++) {
            if (!canTransfer(handler, direction, tank)) continue;

            var fluid = handler.getFluidInTank(tank);
            if (fluid.isEmpty()) continue;

            summed.addTo(fluid, fluid.getAmount());
        }

        return summed;
    }

    private static boolean canTransfer(IFluidHandlerModifiable handler, TransferDirection direction, int tank) {
        return switch (direction) {
            case INSERT -> handler.supportsFill(tank);
            case EXTRACT -> handler.supportsDrain(tank);
        };
    }
}
