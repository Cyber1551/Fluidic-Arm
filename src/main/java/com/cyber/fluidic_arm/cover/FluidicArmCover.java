package com.cyber.fluidic_arm.cover;

import com.cyber.fluidic_arm.cover.transfer.FluidTransfer;
import com.cyber.fluidic_arm.cover.transfer.ItemTransfer;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IIOCover;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.*;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.NumberInputWidget;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerDelegate;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.api.transfer.fluid.ModifiableFluidHandlerWrapper;
import com.gregtechceu.gtceu.api.transfer.item.ItemHandlerDelegate;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;
import com.gregtechceu.gtceu.common.cover.data.TransferMode;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FluidicArmCover extends CoverBehavior implements IIOCover, IUICover, IControllable {
    // region Managed Fields

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidicArmCover.class, CoverBehavior.MANAGED_FIELD_HOLDER);

    @Override
    @MethodsReturnNonnullByDefault
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // endregion

    // region Shared Fields

    public final int tier;
    protected final ConditionalSubscriptionHandler subscriptionHandler;

    @Persisted
    @DescSynced
    protected boolean isWorkingEnabled = true;

    @Persisted
    @DescSynced
    protected ManualIOMode manualIOMode = ManualIOMode.DISABLED;

    // endregion

    // region Robot Arm Fields

    public final int maxItemTransferRate;
    protected int itemsLeftToTransferLastSecond;
    protected int itemsTransferBuffered;

    @Persisted
    protected int transferRate;

    @Persisted
    @DescSynced
    @RequireRerender
    protected IO io;

    @Persisted
    @DescSynced
    protected TransferMode transferMode = TransferMode.TRANSFER_ANY;

    @Persisted
    protected int globalTransferLimit;

    @Persisted
    @DescSynced
    protected final FilterHandler<ItemStack, ItemFilter> itemFilterHandler;

    private IntInputWidget stackSizeInput;
    private CoverableItemHandlerWrapper itemHandlerWrapper;

    // endregion

    // region Fluid Regulator Fields

    private static final int MAX_FLUID_STACK_SIZE = 2_048_000_000;

    public final int maxFluidTransferRate;
    protected int mbLeftToTransferLastSecond;
    protected int fluidTransferBuffered;

    @Persisted
    protected int fluidTransferRate;

    @Persisted
    @DescSynced
    @RequireRerender
    protected IO fluidIo;

    @Persisted
    @DescSynced
    protected BucketMode bucketMode = BucketMode.MILLI_BUCKET;

    @Persisted
    @DescSynced
    protected BucketMode transferBucketMode = BucketMode.MILLI_BUCKET;

    @Persisted
    @DescSynced
    protected TransferMode fluidTransferMode = TransferMode.TRANSFER_ANY;

    @Persisted
    protected int fluidGlobalTransferLimit;

    @Persisted
    @DescSynced
    protected final FilterHandler<FluidStack, FluidFilter> fluidFilterHandler;

    private NumberInputWidget<Integer> fluidRateInput;
    private NumberInputWidget<Integer> transferSizeInput;
    private EnumSelectorWidget<BucketMode> transferBucketModeInput;
    private CoverableFluidHandlerWrapper fluidHandlerWrapper;

    // endregion

    // region Constructor

    public FluidicArmCover(CoverDefinition coverDefinition, ICoverable coverHolder, Direction attachedSide, int tier) {
        super(coverDefinition, coverHolder, attachedSide);
        this.tier = tier;

        // Robot Arm
        this.maxItemTransferRate = FARates.itemTransferRate(tier); // 8, 32, 128, 512, 2048, 8192
        this.transferRate = maxItemTransferRate;
        this.itemsLeftToTransferLastSecond = transferRate;
        this.io = IO.OUT;
        this.itemFilterHandler = FilterHandlers.item(this)
                .onFilterLoaded(f -> configureFilter())
                .onFilterUpdated(f -> configureFilter())
                .onFilterRemoved(f -> configureFilter());

        // Fluid Regulator
        this.maxFluidTransferRate = FARates.fluidTransferRate(tier); // 64, 256, 1024, 4096, 16384, 65536
        this.fluidIo = IO.OUT;
        this.fluidTransferRate = maxFluidTransferRate;
        this.mbLeftToTransferLastSecond = fluidTransferRate * 20;
        this.fluidFilterHandler = FilterHandlers.fluid(this)
                .onFilterLoaded(f -> configureFluidFilter())
                .onFilterUpdated(f -> configureFluidFilter())
                .onFilterRemoved(f -> configureFluidFilter());

        this.subscriptionHandler = new ConditionalSubscriptionHandler(coverHolder, this::update, this::isSubscriptionActive);
    }

    // endregion Constructor

    // region Lifecycle

    @Override
    public void onLoad() {
        super.onLoad();
        subscriptionHandler.initialize(coverHolder.getLevel());
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        subscriptionHandler.unsubscribe();
    }

    @Override
    public boolean canAttach() {
        return super.canAttach() && (getOwnItemHandler() != null || getOwnFluidHandler() != null);
    }

    @Override
    public void onNeighborChanged(@NonNull Block block, @NonNull BlockPos fromPos, boolean isMoving) {
        subscriptionHandler.updateSubscription();
    }

    // endregion

    // region Ticking

    protected void update() {
        var timer = coverHolder.getOffsetTimer();

        if (timer % 5 == 0) {
            if (itemsLeftToTransferLastSecond > 0) {
                itemsLeftToTransferLastSecond -= doTransferItems(itemsLeftToTransferLastSecond);
            }

            if (mbLeftToTransferLastSecond > 0) {
                mbLeftToTransferLastSecond -= doTransferFluids(mbLeftToTransferLastSecond);
            }

            if (timer % 20 == 0) {
                itemsLeftToTransferLastSecond = transferRate;
                mbLeftToTransferLastSecond = fluidTransferRate * 20;
            }

            subscriptionHandler.updateSubscription();
        }
    }

    protected boolean isSubscriptionActive() {
        return isWorkingEnabled && (getAdjacentItemHandler() != null || getAdjacentFluidHandler() != null);
    }

    // endregion

    // region Item Transfer Logic

    protected int doTransferItems(int maxTransferAmount) {
        var adjacent = getAdjacentItemHandler();
        var self = getOwnItemHandler();

        if (adjacent == null || self == null) return 0;

        return switch (io) {
            case IN -> doTransferItemsInternal(adjacent, self, maxTransferAmount);
            case OUT -> doTransferItemsInternal(self, adjacent, maxTransferAmount);
            default -> 0;
        };
    }

    protected int doTransferItemsInternal(IItemHandler sourceInventory, IItemHandler targetInventory, int maxTransferAmount) {
        return switch (transferMode) {
            case TRANSFER_ANY -> doTransferAnyItems(sourceInventory, targetInventory, maxTransferAmount);
            case TRANSFER_EXACT -> doTransferExactItems(sourceInventory, targetInventory, maxTransferAmount);
            case KEEP_EXACT -> doKeepExactItems(sourceInventory, targetInventory, maxTransferAmount);
        };
    }

    protected int doTransferAnyItems(IItemHandler sourceInventory, IItemHandler targetInventory, int maxTransferAmount) {
        return ItemTransfer.moveAny(sourceInventory, targetInventory, itemFilterHandler.getFilter(), maxTransferAmount);
    }

    protected int doTransferExactItems(IItemHandler sourceInventory, IItemHandler targetInventory, int maxTransferAmount) {
        var sourceItemAmount = ItemTransfer.countByType(sourceInventory, itemFilterHandler.getFilter());

        var iterator = sourceItemAmount.keySet().iterator();
        while (iterator.hasNext()) {
            var sourceInfo = sourceItemAmount.get(iterator.next());
            var itemToMoveAmount = getFilteredItemAmount(sourceInfo.itemStack);
            if (sourceInfo.totalCount >= itemToMoveAmount) {
                sourceInfo.totalCount = itemToMoveAmount;
            } else {
                iterator.remove();
            }
        }

        var itemsTransferred = 0;
        var maxTotalTransferAmount = maxTransferAmount + itemsTransferBuffered;
        var notEnoughTransferRate = false;

        for (var itemInfo : sourceItemAmount.values()) {
            if (maxTotalTransferAmount >= itemInfo.totalCount) {
                var result = ItemTransfer.moveExact(sourceInventory, targetInventory, itemInfo);
                itemsTransferred += result ? itemInfo.totalCount : 0;
                maxTotalTransferAmount -= result ? itemInfo.totalCount : 0;
            } else {
                notEnoughTransferRate = true;
            }
        }

        if (itemsTransferred == 0 && notEnoughTransferRate) {
            itemsTransferBuffered += maxTransferAmount;
        } else {
            itemsTransferBuffered = 0;
        }

        return Math.min(itemsTransferred, maxTransferAmount);
    }

    protected int doKeepExactItems(IItemHandler sourceInventory, IItemHandler targetInventory, int maxTransferAmount) {
        var filter = itemFilterHandler.getFilter();
        var targetItemAmounts = ItemTransfer.countByMatchSlot(targetInventory, filter);
        var sourceItemAmounts = ItemTransfer.countByMatchSlot(sourceInventory, filter);

        var iterator = sourceItemAmounts.keySet().iterator();
        while (iterator.hasNext()) {
            var filteredItem = iterator.next();
            var sourceInfo = sourceItemAmounts.get(filteredItem);
            var itemToKeepAmount = getFilteredItemAmount(sourceInfo.itemStack);

            var itemAmount = targetItemAmounts.containsKey(filteredItem) ? targetItemAmounts.get(filteredItem).totalCount : 0;
            if (itemAmount < itemToKeepAmount) {
                sourceInfo.totalCount = itemToKeepAmount - itemAmount;
            } else {
                iterator.remove();
            }
        }

        return ItemTransfer.moveByGroup(sourceInventory, targetInventory, sourceItemAmounts, filter, maxTransferAmount);
    }

    private int getFilteredItemAmount(ItemStack itemStack) {
        if (!itemFilterHandler.isFilterPresent()) return globalTransferLimit;
        var filter = itemFilterHandler.getFilter();
        return filter.supportsAmounts() ? filter.testItemCount(itemStack) : globalTransferLimit;
    }

    // endregion

    // region Fluid Transfer Logic

    protected int doTransferFluids(int platformTransferLimit) {
        var adjacent = getAdjacentFluidHandler();
        var own = getOwnFluidHandler();

        if (adjacent == null || own == null) return 0;

        var adjacentMod = adjacent instanceof IFluidHandlerModifiable modifiable ? modifiable : new ModifiableFluidHandlerWrapper(adjacent);
        return switch (fluidIo) {
            case IN -> doTransferFluidsInternal(adjacentMod, own, platformTransferLimit);
            case OUT -> doTransferFluidsInternal(own, adjacentMod, platformTransferLimit);
            default -> 0;
        };
    }

    protected int doTransferFluidsInternal(IFluidHandlerModifiable sourceInventory, IFluidHandlerModifiable targetInventory, int platformTransferLimit) {
        return switch (fluidTransferMode) {
            case TRANSFER_ANY -> doTransferAnyFluids(sourceInventory, targetInventory, platformTransferLimit);
            case TRANSFER_EXACT -> doTransferExactFluids(sourceInventory, targetInventory, platformTransferLimit);
            case KEEP_EXACT -> doKeepExactFluids(sourceInventory, targetInventory, platformTransferLimit);
        };
    }

    protected int doTransferAnyFluids(IFluidHandlerModifiable sourceInventory, IFluidHandlerModifiable targetInventory, int platformTransferLimit) {
        return GTTransferUtils.transferFluidsFiltered(sourceInventory, targetInventory, fluidFilterHandler.getFilter(), platformTransferLimit);
    }

    protected int doTransferExactFluids(IFluidHandlerModifiable sourceInventory, IFluidHandlerModifiable targetInventory, int platformTransferLimit) {
        var fluidLeftToTransfer = platformTransferLimit;

        for (var tank = 0; tank < sourceInventory.getTanks(); tank++) {
            if (fluidLeftToTransfer <= 0) break;

            var sourceFluid = sourceInventory.getFluidInTank(tank).copy();
            var supplyAmount = getFilteredFluidAmount(sourceFluid);
            if (fluidLeftToTransfer + fluidTransferBuffered < supplyAmount) {
                fluidTransferBuffered += fluidLeftToTransfer;
                fluidLeftToTransfer = 0;
                break;
            }

            if (sourceFluid.isEmpty() || supplyAmount <= 0) continue;
            sourceFluid.setAmount(supplyAmount);

            var drained = sourceInventory.drain(sourceFluid, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty() || drained.getAmount() < supplyAmount) continue;

            var insertableAmount = targetInventory.fill(drained.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (insertableAmount != supplyAmount) continue;

            drained.setAmount(insertableAmount);
            drained = sourceInventory.drain(drained, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                targetInventory.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                fluidLeftToTransfer -= (drained.getAmount() - fluidTransferBuffered);
            }

            fluidTransferBuffered = 0;
        }

        return platformTransferLimit - fluidLeftToTransfer;
    }

    protected int doKeepExactFluids(IFluidHandlerModifiable sourceInventory, IFluidHandlerModifiable targetInventory, int platformTransferLimit) {
        var fluidLeftToTransfer = platformTransferLimit;

        var sourceAmounts = FluidTransfer.enumerateDistinctFluids(sourceInventory, FluidTransfer.TransferDirection.EXTRACT);
        var destinationAmounts = FluidTransfer.enumerateDistinctFluids(targetInventory, FluidTransfer.TransferDirection.INSERT);

        for (var fluidStack : sourceAmounts.keySet()) {
            if (fluidLeftToTransfer <= 0) break;

            var amountToKeep = getFilteredFluidAmount(fluidStack);
            var amountInDest = destinationAmounts.getOrDefault(fluidStack, 0);
            if (amountInDest >= amountToKeep) continue;

            var fluidToMove = fluidStack.copy();
            fluidToMove.setAmount(Math.min(fluidLeftToTransfer, (int) (amountToKeep - amountInDest)));
            if (fluidToMove.getAmount() <= 0) continue;

            var drained = sourceInventory.drain(fluidToMove, IFluidHandler.FluidAction.SIMULATE);
            var fillableAmount = targetInventory.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            if (fillableAmount <= 0) continue;

            fluidToMove.setAmount(Math.min(fluidToMove.getAmount(), fillableAmount));
            drained = sourceInventory.drain(fluidToMove, IFluidHandler.FluidAction.EXECUTE);
            var movedAmount = targetInventory.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            fluidLeftToTransfer -= movedAmount;
        }

        return platformTransferLimit - fluidLeftToTransfer;
    }

    private int getFilteredFluidAmount(FluidStack fluidStack) {
        if (!fluidFilterHandler.isFilterPresent()) return fluidGlobalTransferLimit;
        var filter = fluidFilterHandler.getFilter();
        return filter.supportsAmounts() ? filter.testFluidAmount(fluidStack) : fluidGlobalTransferLimit;
    }

    // endregion

    // region Item Handlers

    protected @Nullable IItemHandlerModifiable getOwnItemHandler() {
        return coverHolder.getItemHandlerCap(attachedSide, false);
    }

    protected @Nullable IItemHandler getAdjacentItemHandler() {
        return GTTransferUtils.getAdjacentItemHandler(coverHolder.getLevel(), coverHolder.getPos(), attachedSide).resolve().orElse(null);
    }

    @Override
    public IItemHandlerModifiable getItemHandlerCap(@Nullable IItemHandlerModifiable defaultValue) {
        if (defaultValue == null) return null;
        if (itemHandlerWrapper == null || itemHandlerWrapper.delegate != defaultValue) {
            this.itemHandlerWrapper = new CoverableItemHandlerWrapper(defaultValue);
        }

        return itemHandlerWrapper;
    }

    private class CoverableItemHandlerWrapper extends ItemHandlerDelegate {
        public CoverableItemHandlerWrapper(IItemHandlerModifiable delegate) { super(delegate); }

        @Override
        @NonNull
        public ItemStack insertItem(int slot, @NonNull ItemStack stack, boolean simulate) {
            if (io == IO.OUT) {
                if (manualIOMode == ManualIOMode.DISABLED) return stack;
                if (manualIOMode == ManualIOMode.UNFILTERED) return super.insertItem(slot, stack, simulate);
            }

            if (!itemFilterHandler.test(stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        @NonNull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (io == IO.IN) {
                if (manualIOMode == ManualIOMode.DISABLED) return ItemStack.EMPTY;
                if (manualIOMode == ManualIOMode.UNFILTERED) return super.extractItem(slot, amount, simulate);
            }

            var result = super.extractItem(slot, amount, true);
            if (result.isEmpty() || !itemFilterHandler.test(result)) return ItemStack.EMPTY;
            return simulate ? result : super.extractItem(slot, amount, false);
        }
    }

    // endregion

    // region Fluid Handlers

    protected @Nullable IFluidHandlerModifiable getOwnFluidHandler() {
        return coverHolder.getFluidHandlerCap(attachedSide, false);
    }

    protected @Nullable IFluidHandler getAdjacentFluidHandler() {
        return GTTransferUtils.getAdjacentFluidHandler(coverHolder.getLevel(), coverHolder.getPos(), attachedSide).resolve().orElse(null);
    }

    @Override
    public IFluidHandlerModifiable getFluidHandlerCap(@Nullable IFluidHandlerModifiable defaultValue) {
        if (defaultValue == null) return null;
        if (fluidHandlerWrapper == null || fluidHandlerWrapper.delegate != defaultValue) {
            this.fluidHandlerWrapper = new CoverableFluidHandlerWrapper(defaultValue);
        }

        return fluidHandlerWrapper;
    }

    private class CoverableFluidHandlerWrapper extends FluidHandlerDelegate {
        public CoverableFluidHandlerWrapper(IFluidHandlerModifiable delegate) { super(delegate); }

        @Override
        public int fill(@NonNull FluidStack resource, @NonNull FluidAction action) {
            if (fluidIo == IO.OUT) {
                if (manualIOMode == ManualIOMode.DISABLED) return 0;
                if (manualIOMode == ManualIOMode.UNFILTERED) return super.fill(resource, action);
            }

            if (!fluidFilterHandler.test(resource)) return 0;
            return super.fill(resource, action);
        }

        @Override
        @MethodsReturnNonnullByDefault
        public FluidStack drain(@NonNull FluidStack resource, @NonNull FluidAction action) {
            if (fluidIo == IO.IN) {
                if (manualIOMode == ManualIOMode.DISABLED) return FluidStack.EMPTY;
                if (manualIOMode == ManualIOMode.UNFILTERED) return super.drain(resource, action);
            }

            if (!fluidFilterHandler.test(resource)) return FluidStack.EMPTY;
            return super.drain(resource, action);
        }
    }

    // endregion

    // region Shared Accessors

    @Override
    public ManualIOMode getManualIOMode() {
        return manualIOMode;
    }

    public void setManualIOMode(ManualIOMode mode) {
        this.manualIOMode = mode;
    }

    public boolean isWorkingEnabled() {
        return isWorkingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        if (this.isWorkingEnabled != isWorkingAllowed) {
            this.isWorkingEnabled = isWorkingAllowed;
            subscriptionHandler.updateSubscription();
        }
    }

    // endregion

    // region Robot Arm Accessors

    @Override
    public IO getIo() {
        return io;
    }

    public void setIo(IO io) {
        this.io = io;
    }

    @Override
    public int getTransferRate() {
        return transferRate;
    }

    public void setTransferRate(int transferRate) {
        this.transferRate = transferRate;
    }

    public TransferMode getTransferMode() {
        return transferMode;
    }

    public void setTransferMode(TransferMode mode) {
        this.transferMode = mode;
        configureStackSizeInput();
        if (!isRemote()) configureFilter();
    }

    public FilterHandler<ItemStack, ItemFilter> getItemFilterHandler() {
        return itemFilterHandler;
    }

    // endregion

    // region Fluid Regulator Accessors

    public IO getFluidIo() {
        return fluidIo;
    }

    public void setFluidIo(IO fluidIo) {
        this.fluidIo = fluidIo;
    }

    public int getFluidTransferRate() {
        return fluidTransferRate;
    }

    public void setFluidTransferRate(int fluidTransferRate) {
        this.fluidTransferRate = fluidTransferRate;
    }

    public TransferMode getFluidTransferMode() {
        return fluidTransferMode;
    }

    public void setFluidTransferMode(TransferMode fluidTransferMode) {
        this.fluidTransferMode = fluidTransferMode;
        configureTransferSizeInput();
        if (!isRemote()) configureFluidFilter();
    }

    public BucketMode getBucketMode() {
        return bucketMode;
    }

    public void setBucketMode(BucketMode bucketMode) {
        var oldMultiplier = this.bucketMode.multiplier;
        var newMultiplier = bucketMode.multiplier;
        this.bucketMode = bucketMode;
        if (fluidRateInput == null) return;
        if (oldMultiplier > newMultiplier) fluidRateInput.setValue(getCurrentBucketModeTransferRate());
        fluidRateInput.setMax(maxFluidTransferRate / bucketMode.multiplier);
        if (newMultiplier > oldMultiplier) fluidRateInput.setValue(getCurrentBucketModeTransferRate());
    }

    public BucketMode getTransferBucketMode() {
        return transferBucketMode;
    }

    private void setTransferBucketMode(BucketMode newTransferBucketMode) {
        var oldMultiplier = transferBucketMode.multiplier;
        var newMultiplier = newTransferBucketMode.multiplier;
        this.transferBucketMode = newTransferBucketMode;
        if (transferSizeInput == null) return;
        if (oldMultiplier > newMultiplier) transferSizeInput.setValue(getCurrentBucketModeTransferSize());
        transferSizeInput.setMax(MAX_FLUID_STACK_SIZE / transferBucketMode.multiplier);
        if (newMultiplier > oldMultiplier) transferSizeInput.setValue(getCurrentBucketModeTransferSize());
    }

    public FilterHandler<FluidStack, FluidFilter> getFluidFilterHandler() {
        return fluidFilterHandler;
    }

    // endregion

    // region Item Configuration

    protected void configureFilter() {
        if (itemFilterHandler.getFilter() instanceof SimpleItemFilter filter) {
            filter.setMaxStackSize(filter.isBlackList() ? 1 : transferMode.maxStackSize);
        }

        configureStackSizeInput();
    }

    protected void configureStackSizeInput() {
        if (stackSizeInput == null) return;
        stackSizeInput.setVisible(shouldShowStackSize());
        stackSizeInput.setMin(1);
        stackSizeInput.setMax(transferMode.maxStackSize);
    }

    protected boolean shouldShowStackSize() {
        if (transferMode == TransferMode.TRANSFER_ANY) return false;
        if (!itemFilterHandler.isFilterPresent()) return true;
        return !itemFilterHandler.getFilter().supportsAmounts();
    }

    // endregion

    // region Fluid Configuration

    protected void configureFluidFilter() {
        if (fluidFilterHandler.getFilter() instanceof SimpleFluidFilter filter) {
            filter.setMaxStackSize(fluidTransferMode == TransferMode.TRANSFER_ANY ? 1 : MAX_FLUID_STACK_SIZE);
        }

        configureTransferSizeInput();
    }

    private void configureTransferSizeInput() {
        if (transferSizeInput == null || transferBucketModeInput == null) return;
        transferSizeInput.setVisible(shouldShowTransferSize());
        transferBucketModeInput.setVisible(shouldShowTransferSize());
    }

    private boolean shouldShowTransferSize() {
        if (fluidTransferMode == TransferMode.TRANSFER_ANY) return false;
        if (!fluidFilterHandler.isFilterPresent()) return true;
        return !fluidFilterHandler.getFilter().supportsAmounts();
    }

    private int getCurrentBucketModeTransferRate() {
        return fluidTransferRate / bucketMode.multiplier;
    }

    private void setCurrentBucketModeTransferRate(int rate) {
        setFluidTransferRate(Math.min(Math.max(rate * bucketMode.multiplier, 1), maxFluidTransferRate));
    }

    private int getCurrentBucketModeTransferSize() {
        return fluidGlobalTransferLimit / transferBucketMode.multiplier;
    }

    private void setCurrentBucketModeTransferSize(int size) {
        fluidGlobalTransferLimit = Math.min(Math.max(size * transferBucketMode.multiplier, 0), MAX_FLUID_STACK_SIZE);
    }

    // endregion

    // region GUI

    @Override
    public Widget createUIWidget() {
        var root = new WidgetGroup(0, 0, 176, 137);
        var tabs = new TabContainer(0, 0, 176, 137);

        tabs.addTab(new TabButton(-17, 8, 20, 20).setTexture(
                new GuiTextureGroup(TabContainer.TABS_LEFT.getSubTexture(0, 1f/3, 0.5f, 1f/3), new ItemStackTexture(GTItems.ROBOT_ARM_LV.asStack()).scale(0.60f)),
                new GuiTextureGroup(TabContainer.TABS_LEFT.getSubTexture(0.5f, 1f/3, 0.5f, 1f/3), new ItemStackTexture(GTItems.ROBOT_ARM_LV.asStack()).scale(0.65f))),
                createItemConfigGroup());

        tabs.addTab(new TabButton(-17, 28, 20, 20).setTexture(
                new GuiTextureGroup(TabContainer.TABS_LEFT.getSubTexture(0, 1f/3, 0.5f, 1f/3), new ItemStackTexture(GTItems.FLUID_REGULATOR_LV.asStack()).scale(0.60f)),
                new GuiTextureGroup(TabContainer.TABS_LEFT.getSubTexture(0.5f, 1f/3, 0.5f, 1f/3), new ItemStackTexture(GTItems.FLUID_REGULATOR_LV.asStack()).scale(0.65f))),
                createFluidConfigGroup());

        root.addWidget(tabs);
        root.addWidget(new EnumSelectorWidget<>(146, 107, 20, 20, ManualIOMode.VALUES, manualIOMode, this::setManualIOMode).setHoverTooltips("cover.universal.manual_import_export.mode.description"));
        return root;
    }

    private WidgetGroup createItemConfigGroup() {
        var group = new WidgetGroup(0, 0, 176, 137);
        group.addWidget(new LabelWidget(10, 5, Component.translatable("cover.fluidic_arm.robot_arm.title", GTValues.VN[tier]).getString()));
        group.addWidget(new IntInputWidget(10, 20, 156, 20, () -> this.transferRate, this::setTransferRate).setMin(1).setMax(maxItemTransferRate));
        group.addWidget(new EnumSelectorWidget<>(146, 45, 20, 20, TransferMode.values(), transferMode, this::setTransferMode));
        this.stackSizeInput = new IntInputWidget(64, 45, 80, 20, () -> globalTransferLimit, v -> globalTransferLimit = v);
        configureStackSizeInput();
        group.addWidget(this.stackSizeInput);
        createIoAndFilterWidgets(group, io, this::setIo, itemFilterHandler);
        return group;
    }

    private WidgetGroup createFluidConfigGroup() {
        var group = new WidgetGroup(0, 0, 176, 137);
        group.addWidget(new LabelWidget(10, 5, Component.translatable("cover.fluidic_arm.fluid_regulator.title", GTValues.VN[tier]).getString()));
        group.addWidget(new EnumSelectorWidget<>(146, 45, 20, 20, TransferMode.values(), fluidTransferMode, this::setFluidTransferMode));
        this.fluidRateInput = new IntInputWidget(10, 20, 134, 20, this::getCurrentBucketModeTransferRate, this::setCurrentBucketModeTransferRate).setMin(1);
        setBucketMode(this.bucketMode);
        group.addWidget(this.fluidRateInput);
        group.addWidget(new EnumSelectorWidget<>(146, 20, 20, 20, Arrays.stream(BucketMode.values()).filter(m -> m.multiplier <= maxFluidTransferRate).toList(), bucketMode, this::setBucketMode));
        this.transferSizeInput = new IntInputWidget(35, 45, 84, 20, this::getCurrentBucketModeTransferSize, this::setCurrentBucketModeTransferSize).setMin(0).setMax(Integer.MAX_VALUE);
        this.transferBucketModeInput = new EnumSelectorWidget<>(121, 45, 20, 20, BucketMode.values(), transferBucketMode, this::setTransferBucketMode);
        configureTransferSizeInput();
        group.addWidget(this.transferSizeInput);
        group.addWidget(this.transferBucketModeInput);
        createIoAndFilterWidgets(group, fluidIo, this::setFluidIo, fluidFilterHandler);
        return group;
    }

    private void createIoAndFilterWidgets(WidgetGroup group, IO inputOutput, Consumer<IO> onChanged, FilterHandler<?, ?> filterHandler) {
        group.addWidget(new EnumSelectorWidget<>(10, 45, 20, 20, List.of(IO.IN, IO.OUT), inputOutput, onChanged));
        group.addWidget(filterHandler.createFilterSlotUI(125, 108));
        group.addWidget(filterHandler.createFilterConfigUI(10, 72, 156, 60));
    }

    // endregion
}
