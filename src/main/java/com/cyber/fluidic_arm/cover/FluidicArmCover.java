package com.cyber.fluidic_arm.cover;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IIOCover;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.common.cover.data.DistributionMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;

public class FluidicArmCover extends CoverBehavior implements IIOCover {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidicArmCover.class, CoverBehavior.MANAGED_FIELD_HOLDER);

    public final int tier;

    // region Robot Arm

    public final int maxItemTransferRate;
    protected int itemsLeftToTransferLastSecond;

    @Persisted
    protected int transferRate;

    @Persisted
    @DescSynced
    @RequireRerender
    protected IO io;

    @Persisted
    @DescSynced
    protected DistributionMode distributionMode;

    @Persisted
    @DescSynced
    protected ManualIOMode manualIOMode = ManualIOMode.DISABLED;

    @Persisted
    @DescSynced
    protected boolean isWorkingEnabled = true;

    @Persisted
    @DescSynced
    protected final FilterHandler<ItemStack, ItemFilter> itemFilterHandler;

    // endregion

    protected final ConditionalSubscriptionHandler subscriptionHandler;

    public FluidicArmCover(CoverDefinition coverDefinition, ICoverable coverHolder, Direction attachedSide, int tier) {
        super(coverDefinition, coverHolder, attachedSide);
        this.tier = tier;

        // Robot Arm
        this.maxItemTransferRate = 2 * (int)Math.pow(4, Math.min(tier, GTValues.LuV)); // 8, 32, 128, 512, 1024
        this.transferRate = maxItemTransferRate;
        this.itemsLeftToTransferLastSecond = transferRate;
        this.io = IO.OUT;
        this.distributionMode = DistributionMode.INSERT_FIRST;
        this.itemFilterHandler = FilterHandlers.item(this);

        this.subscriptionHandler = new ConditionalSubscriptionHandler(coverHolder, this::update, this::isSubscriptionActive);
    }

    @Override
    @MethodsReturnNonnullByDefault
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean canAttach() {
        return super.canAttach() && getOwnItemHandler() != null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        subscriptionHandler.initialize(coverHolder.getLevel());
    }

    @Override
    public void onNeighborChanged(@NonNull Block block, @NonNull BlockPos fromPos, boolean isMoving) {
        subscriptionHandler.updateSubscription();
    }

    protected boolean isSubscriptionActive() {
        return isWorkingEnabled && getAdjacentItemHandler() != null;
    }

    protected void update() {
        var timer = coverHolder.getOffsetTimer();

        if (timer % 5 == 0) {
            if (itemsLeftToTransferLastSecond > 0) {
                var adjacent = getAdjacentItemHandler();
                var self = getOwnItemHandler();

                if (adjacent != null && self != null) {
                    var moved = switch (io) {
                        case IN -> doTransferItems(adjacent, self, itemsLeftToTransferLastSecond);
                        case OUT -> doTransferItems(self, adjacent, itemsLeftToTransferLastSecond);
                        default -> 0;
                    };

                    itemsLeftToTransferLastSecond -= moved;
                }
            }

            if (timer % 20 == 0) itemsLeftToTransferLastSecond = transferRate;
            subscriptionHandler.updateSubscription();
        }
    }

    // region Item Transfer Logic

    protected int doTransferItems(IItemHandler sourceInventory, IItemHandler targetInventory, int maxTransferAmount) {
        return moveInventoryItems(sourceInventory, targetInventory, maxTransferAmount);
    }

    protected int moveInventoryItems(IItemHandler sourceInventory, IItemHandler targetInventory, int maxTransferAmount) {
        var filter = itemFilterHandler.getFilter();
        var itemsLeftToTransfer = maxTransferAmount;

        for (var srcIndex = 0; srcIndex < sourceInventory.getSlots(); srcIndex++) {
            var sourceStack = sourceInventory.extractItem(srcIndex, itemsLeftToTransfer, true);
            if (sourceStack.isEmpty()) continue;
            if (!filter.test(sourceStack)) continue;

            var remainder = ItemHandlerHelper.insertItem(targetInventory, sourceStack, true);
            var amountToInsert = sourceStack.getCount() - remainder.getCount();

            if (amountToInsert > 0) {
                sourceStack = sourceInventory.extractItem(srcIndex, amountToInsert, false);
                if (!sourceStack.isEmpty()) {
                    ItemHandlerHelper.insertItem(targetInventory, sourceStack, false);
                    itemsLeftToTransfer -= sourceStack.getCount();

                    if (itemsLeftToTransfer == 0) break;
                }
            }
        }

        return maxTransferAmount - itemsLeftToTransfer;
    }

    // endregion

    // region Item Handlers

    protected @Nullable IItemHandlerModifiable getOwnItemHandler() {
        return coverHolder.getItemHandlerCap(attachedSide, false);
    }

    protected @Nullable IItemHandler getAdjacentItemHandler() {
        return GTTransferUtils.getAdjacentItemHandler(coverHolder.getLevel(), coverHolder.getPos(), attachedSide).resolve().orElse(null);
    }

    // endregion

    // region Robot Arm Getters

    @Override
    public IO getIo() {
        return io;
    }

    @Override
    public ManualIOMode getManualIOMode() {
        return manualIOMode;
    }

    @Override
    public int getTransferRate() {
        return transferRate;
    }

    public DistributionMode getDistributionMode() {
        return distributionMode;
    }

    public boolean isWorkingEnabled() {
        return isWorkingEnabled;
    }

    public FilterHandler<ItemStack, ItemFilter> getItemFilterHandler() {
        return itemFilterHandler;
    }

    // endregion
}
