package com.cyber.fluidic_arm.cover;

import com.gregtechceu.gtceu.api.GTValues;

public class FARates {
    /**
     * LV - 8
     * MV - 32
     * HV - 128
     * EV - 512
     * IV - 2048
     * LuV - 8192
     * @param tier The tier of the cover
     * @return The number of items that can be transferred per second
     */
    public static int itemTransferRate(int tier) {
        return 2 * (int) Math.pow(4, Math.min(tier, GTValues.LuV));
    }

    /**
     * LV - 64
     * MV - 256
     * HV - 1024
     * EV - 4096
     * IV - 16384
     * LuV - 65536
     * @param tier The tier of the cover
     * @return The mb of fluid that can be transferred per tick
     */
    public static int fluidTransferRate(int tier) {
        return 64 * (int) Math.pow(4, Math.min(tier, GTValues.LuV) - 1);
    }
}
