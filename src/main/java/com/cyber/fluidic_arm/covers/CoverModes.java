package com.cyber.fluidic_arm.covers;

import gregtech.common.covers.IIOMode;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

enum CoverMode implements IStringSerializable {

    ITEM("metaitem.fluidic_arm.covermode.item"),
    FLUID("metaitem.fluidic_arm.covermode.fluid");

    public final String localeName;
    public static final CoverMode[] VALUES = values();

    CoverMode(String localeName) {
        this.localeName = localeName;
    }

    @Nonnull
    @Override
    public String getName() {
        return localeName;
    }
}

enum ConveyorMode implements IStringSerializable, IIOMode {
    IMPORT("cover.conveyor.mode.import"),
    EXPORT("cover.conveyor.mode.export");

    public final String localeName;

    ConveyorMode(String localeName) {
        this.localeName = localeName;
    }

    @Nonnull
    @Override
    public String getName() {
        return localeName;
    }

    @Override
    public boolean isImport() {
        return this == IMPORT;
    }
}

enum PumpMode implements IStringSerializable, IIOMode {
    IMPORT("cover.pump.mode.import"),
    EXPORT("cover.pump.mode.export");

    public final String localeName;

    PumpMode(String localeName) {
        this.localeName = localeName;
    }

    @Nonnull
    @Override
    public String getName() {
        return localeName;
    }

    @Override
    public boolean isImport() {
        return this == IMPORT;
    }
}

enum BucketMode implements IStringSerializable {
    BUCKET("cover.bucket.mode.bucket"),
    MILLI_BUCKET("cover.bucket.mode.milli_bucket");

    public final String localeName;

    BucketMode(String localeName) {
        this.localeName = localeName;
    }

    @Nonnull
    @Override
    public String getName() {
        return localeName;
    }
}
