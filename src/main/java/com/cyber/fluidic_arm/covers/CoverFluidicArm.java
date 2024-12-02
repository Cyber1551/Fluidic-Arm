package com.cyber.fluidic_arm.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.cyber.fluidic_arm.filter.CustomFluidFilterContainer;
import com.cyber.fluidic_arm.filter.CustomItemFilterContainer;
import com.google.common.math.IntMath;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.capability.impl.FluidHandlerDelegate;
import gregtech.api.capability.impl.ItemHandlerDelegate;
import gregtech.api.cover.*;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.*;
import gregtech.api.util.GTTransferUtils;
import gregtech.api.util.ItemStackHashStrategy;
import gregtech.api.util.TextFormattingUtil;
import gregtech.client.renderer.texture.Textures;
import gregtech.client.renderer.texture.cube.SimpleSidedCubeRenderer;
import gregtech.common.covers.DistributionMode;
import gregtech.common.covers.IIOMode;
import gregtech.common.covers.ManualImportExportMode;
import gregtech.common.covers.TransferMode;
import gregtech.common.covers.filter.FluidFilter;
import gregtech.common.covers.filter.SmartItemFilter;
import gregtech.common.pipelike.itempipe.net.ItemNetHandler;
import gregtech.common.pipelike.itempipe.tile.TileEntityItemPipe;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.message.FormattedMessage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class CoverFluidicArm extends CoverBase implements ITickable, CoverWithUI, IControllable {

    public final int tier;

    private CoverMode coverMode;

    protected final int maxItemTransferRate;
    protected final int maxFluidTransferRate;
    private int transferRate;
    private int fluidTransferRate;

    private ConveyorMode conveyorMode;
    protected PumpMode pumpMode;

    protected int itemsLeftToTransferLastSecond;
    protected int fluidLeftToTransferLastSecond;

    private CoverableItemHandlerWrapper itemHandlerWrapper;
    private CoverableFluidHandlerWrapper fluidHandlerWrapper;

    protected DistributionMode distributionMode;
    private BucketMode bucketMode;

    protected final CustomItemFilterContainer itemFilterContainer;
    protected final CustomFluidFilterContainer fluidFilterContainer;

    protected ManualImportExportMode manualImportExportMode = ManualImportExportMode.DISABLED;

    protected TransferMode transferMode;
    protected int itemsTransferBuffered;

    protected TransferMode fluidTransferMode;
    protected int fluidTransferAmount = 0;

    protected boolean isWorkingAllowed = true;

    public static final int UPDATE_CONVEYOR_MODE = GregtechDataCodes.assignId();
    public static final int UPDATE_PUMP_MODE = GregtechDataCodes.assignId();
    public static final int UPDATE_COVER_TYPE_MODE = GregtechDataCodes.assignId();

    public CoverFluidicArm(CoverDefinition definition, CoverableView coverableView, EnumFacing attachedSide, int tier, int itemsPerSecond, int mbPerTick) {
        super(definition, coverableView, attachedSide);
        this.tier = tier;
        this.maxItemTransferRate = itemsPerSecond;
        this.maxFluidTransferRate = mbPerTick;
        this.transferMode = TransferMode.TRANSFER_ANY;
        this.fluidTransferMode = TransferMode.TRANSFER_ANY;
        this.transferRate = maxItemTransferRate;
        this.fluidTransferRate = maxFluidTransferRate;
        this.itemsLeftToTransferLastSecond = transferRate;
        this.fluidLeftToTransferLastSecond = fluidTransferRate;
        this.conveyorMode = ConveyorMode.EXPORT;
        this.distributionMode = DistributionMode.INSERT_FIRST;
        this.itemFilterContainer = new CustomItemFilterContainer(this);
        this.fluidFilterContainer = new CustomFluidFilterContainer(this, this::shouldShowTip, maxFluidTransferRate * 100);
        this.pumpMode = PumpMode.EXPORT;
        this.bucketMode = BucketMode.MILLI_BUCKET;
        this.coverMode = CoverMode.ITEM;

        this.itemFilterContainer.setMaxStackSize(1);
    }

    protected boolean shouldShowTip() {
        return this.fluidTransferMode != TransferMode.TRANSFER_ANY;
    }

    public void setTransferRate(int transferRate) {
        this.transferRate = MathHelper.clamp(transferRate, 1, maxItemTransferRate);
        CoverableView coverable = getCoverableView();
        coverable.markDirty();

        if (getWorld() != null && getWorld().isRemote) {
            // tile at cover holder pos
            TileEntity te = getTileEntityHere();
            if (te instanceof TileEntityItemPipe) {
                ((TileEntityItemPipe) te).resetTransferred();
            }
            // tile neighbour to holder pos at attached side
            te = getNeighbor(getAttachedSide());
            if (te instanceof TileEntityItemPipe) {
                ((TileEntityItemPipe) te).resetTransferred();
            }
        }
    }

    public int getTransferRate() {
        return transferRate;
    }

    protected void adjustTransferRate(int amount) {
        setTransferRate(MathHelper.clamp(transferRate + amount, 1, maxItemTransferRate));
    }


    public void setFluidTransferRate(int fluidTransferRate) {
        this.fluidTransferRate = fluidTransferRate;
        markDirty();
    }

    public int getFluidTransferRate() {
        return fluidTransferRate;
    }

    protected void adjustFluidTransferRate(int amount) {
        amount *= this.bucketMode == BucketMode.BUCKET ? 1000 : 1;
        setFluidTransferRate(MathHelper.clamp(fluidTransferRate + amount, 1, maxFluidTransferRate));
    }

    public void setPumpMode(PumpMode pumpMode) {
        this.pumpMode = pumpMode;
        writeCustomData(UPDATE_PUMP_MODE, buf -> buf.writeEnumValue(pumpMode));
        markDirty();
    }

    public PumpMode getPumpMode() {
        return pumpMode;
    }

    protected int doTransferFluidsInternal(IFluidHandler myFluidHandler, IFluidHandler fluidHandler, int transferLimit) {
        IFluidHandler sourceHandler;
        IFluidHandler destHandler;

        if (pumpMode == PumpMode.IMPORT) {
            sourceHandler = fluidHandler;
            destHandler = myFluidHandler;
        } else if (pumpMode == PumpMode.EXPORT) {
            sourceHandler = myFluidHandler;
            destHandler = fluidHandler;
        } else {
            return 0;
        }
        switch (fluidTransferMode) {
            case TRANSFER_ANY:
                return GTTransferUtils.transferFluids(sourceHandler, destHandler, transferLimit, fluidFilterContainer::testFluidStack);
            case KEEP_EXACT:
                return doKeepExact(transferLimit, sourceHandler, destHandler, fluidFilterContainer::testFluidStack, this.fluidTransferAmount);
            case TRANSFER_EXACT:
                return doTransferExact(transferLimit, sourceHandler, destHandler, fluidFilterContainer::testFluidStack, this.fluidTransferAmount);
        }
        return 0;
    }

    protected int doTransferExact(int transferLimit, IFluidHandler sourceHandler, IFluidHandler destHandler, Predicate<FluidStack> fluidFilter, int supplyAmount) {
        int fluidLeftToTransfer = transferLimit;
        for (IFluidTankProperties tankProperties : sourceHandler.getTankProperties()) {
            FluidStack sourceFluid = tankProperties.getContents();
            if (this.fluidFilterContainer.getFilterWrapper().getFluidFilter() != null && this.fluidTransferMode != TransferMode.TRANSFER_ANY) {
                supplyAmount = this.fluidFilterContainer.getFilterWrapper().getFluidFilter().getFluidTransferLimit(sourceFluid);
            }
            if (fluidLeftToTransfer < supplyAmount)
                break;
            if (sourceFluid == null || sourceFluid.amount == 0 || !fluidFilter.test(sourceFluid)) continue;
            sourceFluid.amount = supplyAmount;
            if (GTTransferUtils.transferExactFluidStack(sourceHandler, destHandler, sourceFluid.copy())) {
                fluidLeftToTransfer -= sourceFluid.amount;
            }
            if (fluidLeftToTransfer == 0) break;
        }
        return transferLimit - fluidLeftToTransfer;
    }

    protected int doKeepExact(final int transferLimit,
                              final IFluidHandler sourceHandler,
                              final IFluidHandler destHandler,
                              final Predicate<FluidStack> fluidFilter,
                              int keepAmount) {

        if (sourceHandler == null || destHandler == null || fluidFilter == null)
            return 0;

        final Map<FluidStack, Integer> sourceFluids =
                collectDistinctFluids(sourceHandler, IFluidTankProperties::canDrain, fluidFilter);
        final Map<FluidStack, Integer> destFluids =
                collectDistinctFluids(destHandler, IFluidTankProperties::canFill, fluidFilter);

        int transferred = 0;
        for (FluidStack fluidStack : sourceFluids.keySet()) {
            if (transferred >= transferLimit)
                break;

            if (this.fluidFilterContainer.getFilterWrapper().getFluidFilter() != null && fluidTransferMode != TransferMode.TRANSFER_ANY) {
                keepAmount = this.fluidFilterContainer.getFilterWrapper().getFluidFilter().getFluidTransferLimit(fluidStack);
            }

            // if fluid needs to be moved to meet the Keep Exact value
            int amountInDest;
            if ((amountInDest = destFluids.getOrDefault(fluidStack, 0)) < keepAmount) {

                // move the lesser of the remaining transfer limit and the difference in actual vs keep exact amount
                int amountToMove = Math.min(transferLimit - transferred,
                        keepAmount - amountInDest);

                // Nothing to do here, try the next fluid.
                if (amountToMove <= 0)
                    continue;

                // Simulate a drain of this fluid from the source tanks
                FluidStack drainedResult = sourceHandler.drain(copyFluidStackWithAmount(fluidStack, amountToMove), false);

                // Can't drain this fluid. Try the next one.
                if (drainedResult == null || drainedResult.amount <= 0 || !fluidStack.equals(drainedResult))
                    continue;

                // account for the possibility that the drain might give us less than requested
                final int drainable = Math.min(amountToMove, drainedResult.amount);

                // Simulate a fill of the drained amount
                int fillResult = destHandler.fill(copyFluidStackWithAmount(fluidStack, drainable), false);

                // Can't fill, try the next fluid.
                if (fillResult <= 0)
                    continue;

                // This Fluid can be drained and filled, so let's move the most that will actually work.
                int fluidToMove = Math.min(drainable, fillResult);
                FluidStack drainedActual = sourceHandler.drain(copyFluidStackWithAmount(fluidStack, fluidToMove), true);

                // Account for potential error states from the drain
                if (drainedActual == null)
                    throw new RuntimeException("Misbehaving fluid container: drain produced null after simulation succeeded");

                if (!fluidStack.equals(drainedActual))
                    throw new RuntimeException("Misbehaving fluid container: drain produced a different fluid than the simulation");

                if (drainedActual.amount != fluidToMove)
                    throw new RuntimeException(new FormattedMessage(
                            "Misbehaving fluid container: drain expected: {}, actual: {}",
                            fluidToMove,
                            drainedActual.amount).getFormattedMessage());


                // Perform Fill
                int filledActual = destHandler.fill(copyFluidStackWithAmount(fluidStack, fluidToMove), true);

                // Account for potential error states from the fill
                if (filledActual != fluidToMove)
                    throw new RuntimeException(new FormattedMessage(
                            "Misbehaving fluid container: fill expected: {}, actual: {}",
                            fluidToMove,
                            filledActual).getFormattedMessage());

                // update the transferred amount
                transferred += fluidToMove;
            }
        }

        return transferred;
    }

    private static FluidStack copyFluidStackWithAmount(FluidStack fs, int amount) {
        FluidStack fs2 = fs.copy();
        fs2.amount = amount;
        return fs2;
    }

    private static Map<FluidStack, Integer> collectDistinctFluids(IFluidHandler handler,
                                                                  Predicate<IFluidTankProperties> tankTypeFilter,
                                                                  Predicate<FluidStack> fluidTypeFilter) {

        final Map<FluidStack, Integer> summedFluids = new Object2IntOpenHashMap<>();
        Arrays.stream(handler.getTankProperties())
                .filter(tankTypeFilter)
                .map(IFluidTankProperties::getContents)
                .filter(Objects::nonNull)
                .filter(fluidTypeFilter)
                .forEach(fs -> {
                    summedFluids.putIfAbsent(fs, 0);
                    summedFluids.computeIfPresent(fs, (k, v) -> v + fs.amount);
                });

        return summedFluids;
    }

    public void setBucketMode(BucketMode bucketMode) {
        this.bucketMode = bucketMode;
        if (this.bucketMode == BucketMode.BUCKET) {
            setFluidTransferRate(fluidTransferRate / 1000 * 1000);
            setFluidTransferAmount(fluidTransferAmount / 1000 * 1000);
        }

        markDirty();
    }

    public BucketMode getBucketMode() {
        return bucketMode;
    }

    public void setConveyorMode(ConveyorMode conveyorMode) {
        this.conveyorMode = conveyorMode;
        writeCustomData(GregtechDataCodes.UPDATE_COVER_MODE, buf -> buf.writeEnumValue(conveyorMode));
        markDirty();
    }

    public ConveyorMode getConveyorMode() {
        return conveyorMode;
    }

    public DistributionMode getDistributionMode() {
        return distributionMode;
    }

    public void setDistributionMode(DistributionMode distributionMode) {
        this.distributionMode = distributionMode;
        markDirty();
    }

    public ManualImportExportMode getManualImportExportMode() {
        return manualImportExportMode;
    }

    public void setManualImportExportMode(ManualImportExportMode manualImportExportMode) {
        this.manualImportExportMode = manualImportExportMode;
        markDirty();
    }

    public int getCoverMode() {
        return this.coverMode.ordinal();
    }

    public void setCoverMode(int coverMode) {
        this.coverMode = CoverMode.VALUES[coverMode];
        markDirty();
    }

    public String getCoverModeTooltip(int mode) {
        return CoverMode.VALUES[mode].getName();
    }

    public CustomItemFilterContainer getItemFilterContainer() {
        return itemFilterContainer;
    }

    public CustomFluidFilterContainer getFluidFilterContainer() {
        return fluidFilterContainer;
    }

    @Override
    public void update() {
        CoverableView coverable = getCoverableView();
        long timer = coverable.getOffsetTimer();
        if (timer % 5 == 0 && isWorkingAllowed && itemsLeftToTransferLastSecond > 0) {
            EnumFacing side = getAttachedSide();
            TileEntity tileEntity = coverable.getNeighbor(side);
            IItemHandler itemHandler = tileEntity == null ? null : tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            IItemHandler myItemHandler = coverable.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
            if (itemHandler != null && myItemHandler != null) {
                int totalTransferred = doTransferItems(itemHandler, myItemHandler, itemsLeftToTransferLastSecond);
                this.itemsLeftToTransferLastSecond -= totalTransferred;
            }
        }

        if (isWorkingAllowed && fluidLeftToTransferLastSecond > 0) {
            this.fluidLeftToTransferLastSecond -= doTransferFluids(fluidLeftToTransferLastSecond);
        }

        if (timer % 20 == 0) {
            this.itemsLeftToTransferLastSecond = transferRate;
            this.fluidLeftToTransferLastSecond = fluidTransferRate;
        }
    }

    protected int doTransferItems(IItemHandler itemHandler, IItemHandler myItemHandler, int maxTransferAmount) {
        if (conveyorMode == ConveyorMode.EXPORT && itemHandler instanceof ItemNetHandler && transferMode == TransferMode.KEEP_EXACT) {
            return 0;
        }
        if (conveyorMode == ConveyorMode.IMPORT && myItemHandler instanceof ItemNetHandler && transferMode == TransferMode.KEEP_EXACT) {
            return 0;
        }
        switch (transferMode) {
            case TRANSFER_ANY:
                return doTransferItemsAny(itemHandler, myItemHandler, maxTransferAmount);
            case TRANSFER_EXACT:
                return doTransferExact(itemHandler, myItemHandler, maxTransferAmount);
            case KEEP_EXACT:
                return doKeepExact(itemHandler, myItemHandler, maxTransferAmount);
            default:
                return 0;
        }
    }

    protected int doTransferExact(IItemHandler itemHandler, IItemHandler myItemHandler, int maxTransferAmount) {
        Map<ItemStack, TypeItemInfo> sourceItemAmount = doCountSourceInventoryItemsByType(itemHandler, myItemHandler);
        Iterator<ItemStack> iterator = sourceItemAmount.keySet().iterator();
        while (iterator.hasNext()) {
            TypeItemInfo sourceInfo = sourceItemAmount.get(iterator.next());
            int itemAmount = sourceInfo.totalCount;
            int itemToMoveAmount = itemFilterContainer.getSlotTransferLimit(sourceInfo.filterSlot);

            // if smart item filter
            if (itemFilterContainer.getFilterWrapper().getItemFilter() instanceof SmartItemFilter) {
                if (itemFilterContainer.getTransferStackSize() > 1 && itemToMoveAmount * 2 <= itemAmount) {
                    // get the max we can extract from the item filter variable
                    int maxMultiplier = Math.floorDiv(maxTransferAmount, itemToMoveAmount);

                    // multiply up to the total count of all the items
                    itemToMoveAmount *= Math.min(itemFilterContainer.getTransferStackSize(), maxMultiplier);
                }
            }

            if (itemAmount >= itemToMoveAmount) {
                sourceInfo.totalCount = itemToMoveAmount;
            } else {
                iterator.remove();
            }
        }

        int itemsTransferred = 0;
        int maxTotalTransferAmount = maxTransferAmount + itemsTransferBuffered;
        boolean notEnoughTransferRate = false;
        for (TypeItemInfo itemInfo : sourceItemAmount.values()) {
            if (maxTotalTransferAmount >= itemInfo.totalCount) {
                boolean result = doTransferItemsExact(itemHandler, myItemHandler, itemInfo);
                itemsTransferred += result ? itemInfo.totalCount : 0;
                maxTotalTransferAmount -= result ? itemInfo.totalCount : 0;
            } else {
                notEnoughTransferRate = true;
            }
        }
        //if we didn't transfer anything because of too small transfer rate, buffer it
        if (itemsTransferred == 0 && notEnoughTransferRate) {
            itemsTransferBuffered += maxTransferAmount;
        } else {
            //otherwise, if transfer succeed, empty transfer buffer value
            itemsTransferBuffered = 0;
        }
        return Math.min(itemsTransferred, maxTransferAmount);
    }

    protected int doKeepExact(IItemHandler itemHandler, IItemHandler myItemHandler, int maxTransferAmount) {
        Map<Object, GroupItemInfo> currentItemAmount = doCountDestinationInventoryItemsByMatchIndex(itemHandler, myItemHandler);
        Map<Object, GroupItemInfo> sourceItemAmounts = doCountDestinationInventoryItemsByMatchIndex(myItemHandler, itemHandler);
        Iterator<Object> iterator = sourceItemAmounts.keySet().iterator();
        while (iterator.hasNext()) {
            Object filterSlotIndex = iterator.next();
            GroupItemInfo sourceInfo = sourceItemAmounts.get(filterSlotIndex);
            int itemToKeepAmount = itemFilterContainer.getSlotTransferLimit(sourceInfo.filterSlot);

            // only run multiplier for smart item
            if (itemFilterContainer.getFilterWrapper().getItemFilter() instanceof SmartItemFilter) {
                if (itemFilterContainer.getTransferStackSize() > 1 && itemToKeepAmount * 2 <= sourceInfo.totalCount) {
                    // get the max we can keep from the item filter variable
                    int maxMultiplier = Math.floorDiv(sourceInfo.totalCount, itemToKeepAmount);

                    // multiply up to the total count of all the items
                    itemToKeepAmount *= Math.min(itemFilterContainer.getTransferStackSize(), maxMultiplier);
                }
            }

            int itemAmount = 0;
            if (currentItemAmount.containsKey(filterSlotIndex)) {
                GroupItemInfo destItemInfo = currentItemAmount.get(filterSlotIndex);
                itemAmount = destItemInfo.totalCount;
            }
            if (itemAmount < itemToKeepAmount) {
                sourceInfo.totalCount = itemToKeepAmount - itemAmount;
            } else {
                iterator.remove();
            }
        }
        return doTransferItemsByGroup(itemHandler, myItemHandler, sourceItemAmounts, maxTransferAmount);
    }

    protected int doTransferItemsAny(IItemHandler itemHandler, IItemHandler myItemHandler, int maxTransferAmount) {
        if (conveyorMode == ConveyorMode.IMPORT) {
            return moveInventoryItems(itemHandler, myItemHandler, maxTransferAmount);
        } else if (conveyorMode == ConveyorMode.EXPORT) {
            return moveInventoryItems(myItemHandler, itemHandler, maxTransferAmount);
        }
        return 0;
    }

    protected int doTransferItemsByGroup(IItemHandler itemHandler, IItemHandler myItemHandler, Map<Object, GroupItemInfo> itemInfos, int maxTransferAmount) {
        if (conveyorMode == ConveyorMode.IMPORT) {
            return moveInventoryItems(itemHandler, myItemHandler, itemInfos, maxTransferAmount);
        } else if (conveyorMode == ConveyorMode.EXPORT) {
            return moveInventoryItems(myItemHandler, itemHandler, itemInfos, maxTransferAmount);
        }
        return 0;
    }

    protected Map<Object, GroupItemInfo> doCountDestinationInventoryItemsByMatchIndex(IItemHandler itemHandler, IItemHandler myItemHandler) {
        if (conveyorMode == ConveyorMode.IMPORT) {
            return countInventoryItemsByMatchSlot(myItemHandler);
        } else if (conveyorMode == ConveyorMode.EXPORT) {
            return countInventoryItemsByMatchSlot(itemHandler);
        }
        return Collections.emptyMap();
    }

    protected Map<ItemStack, TypeItemInfo> doCountSourceInventoryItemsByType(IItemHandler itemHandler, IItemHandler myItemHandler) {
        if (conveyorMode == ConveyorMode.IMPORT) {
            return countInventoryItemsByType(itemHandler);
        } else if (conveyorMode == ConveyorMode.EXPORT) {
            return countInventoryItemsByType(myItemHandler);
        }
        return Collections.emptyMap();
    }

    protected boolean doTransferItemsExact(IItemHandler itemHandler, IItemHandler myItemHandler, TypeItemInfo itemInfo) {
        if (conveyorMode == ConveyorMode.IMPORT) {
            return moveInventoryItemsExact(itemHandler, myItemHandler, itemInfo);
        } else if (conveyorMode == ConveyorMode.EXPORT) {
            return moveInventoryItemsExact(myItemHandler, itemHandler, itemInfo);
        }
        return false;
    }

    protected static boolean moveInventoryItemsExact(IItemHandler sourceInventory, IItemHandler targetInventory, TypeItemInfo itemInfo) {
        //first, compute how much can we extract in reality from the machine,
        //because totalCount is based on what getStackInSlot returns, which may differ from what
        //extractItem() will return
        ItemStack resultStack = itemInfo.itemStack.copy();
        int totalExtractedCount = 0;
        int itemsLeftToExtract = itemInfo.totalCount;

        for (int i = 0; i < itemInfo.slots.size(); i++) {
            int slotIndex = itemInfo.slots.get(i);
            ItemStack extractedStack = sourceInventory.extractItem(slotIndex, itemsLeftToExtract, true);
            if (!extractedStack.isEmpty() &&
                    ItemStack.areItemsEqual(resultStack, extractedStack) &&
                    ItemStack.areItemStackTagsEqual(resultStack, extractedStack)) {
                totalExtractedCount += extractedStack.getCount();
                itemsLeftToExtract -= extractedStack.getCount();
            }
            if (itemsLeftToExtract == 0) {
                break;
            }
        }
        //if amount of items extracted is not equal to the amount of items we
        //wanted to extract, abort item extraction
        if (totalExtractedCount != itemInfo.totalCount) {
            return false;
        }
        //adjust size of the result stack accordingly
        resultStack.setCount(totalExtractedCount);

        //now, see how much we can insert into destination inventory
        //if we can't insert as much as itemInfo requires, and remainder is empty, abort, abort
        ItemStack remainder = GTTransferUtils.insertItem(targetInventory, resultStack, true);
        if (!remainder.isEmpty()) {
            return false;
        }

        //otherwise, perform real insertion and then remove items from the source inventory
        GTTransferUtils.insertItem(targetInventory, resultStack, false);

        //perform real extraction of the items from the source inventory now
        itemsLeftToExtract = itemInfo.totalCount;
        for (int i = 0; i < itemInfo.slots.size(); i++) {
            int slotIndex = itemInfo.slots.get(i);
            ItemStack extractedStack = sourceInventory.extractItem(slotIndex, itemsLeftToExtract, false);
            if (!extractedStack.isEmpty() &&
                    ItemStack.areItemsEqual(resultStack, extractedStack) &&
                    ItemStack.areItemStackTagsEqual(resultStack, extractedStack)) {
                itemsLeftToExtract -= extractedStack.getCount();
            }
            if (itemsLeftToExtract == 0) {
                break;
            }
        }
        return true;
    }

    protected int moveInventoryItems(@Nonnull IItemHandler sourceInventory, IItemHandler targetInventory, Map<Object, GroupItemInfo> itemInfos, int maxTransferAmount) {
        int itemsLeftToTransfer = maxTransferAmount;
        for (int i = 0; i < sourceInventory.getSlots(); i++) {
            ItemStack itemStack = sourceInventory.getStackInSlot(i);
            if (itemStack.isEmpty()) {
                continue;
            }

            Object matchSlotIndex = itemFilterContainer.matchItemStack(itemStack);
            if (matchSlotIndex == null || !itemInfos.containsKey(matchSlotIndex)) {
                continue;
            }

            GroupItemInfo itemInfo = itemInfos.get(matchSlotIndex);

            ItemStack extractedStack = sourceInventory.extractItem(i, Math.min(itemInfo.totalCount, itemsLeftToTransfer), true);

            ItemStack remainderStack = GTTransferUtils.insertItem(targetInventory, extractedStack, true);
            int amountToInsert = extractedStack.getCount() - remainderStack.getCount();

            if (amountToInsert > 0) {
                extractedStack = sourceInventory.extractItem(i, amountToInsert, false);

                if (!extractedStack.isEmpty()) {

                    GTTransferUtils.insertItem(targetInventory, extractedStack, false);
                    itemsLeftToTransfer -= extractedStack.getCount();
                    itemInfo.totalCount -= extractedStack.getCount();

                    if (itemInfo.totalCount == 0) {
                        itemInfos.remove(matchSlotIndex);
                        if (itemInfos.isEmpty()) {
                            break;
                        }
                    }
                    if (itemsLeftToTransfer == 0) {
                        break;
                    }
                }
            }
        }
        return maxTransferAmount - itemsLeftToTransfer;
    }

    protected int moveInventoryItems(IItemHandler sourceInventory, IItemHandler targetInventory, int maxTransferAmount) {
        int itemsLeftToTransfer = maxTransferAmount;
        for (int srcIndex = 0; srcIndex < sourceInventory.getSlots(); srcIndex++) {
            ItemStack sourceStack = sourceInventory.extractItem(srcIndex, itemsLeftToTransfer, true);
            if (sourceStack.isEmpty()) {
                continue;
            }
            if (!itemFilterContainer.testItemStack(sourceStack)) {
                continue;
            }
            ItemStack remainder = GTTransferUtils.insertItem(targetInventory, sourceStack, true);
            int amountToInsert = sourceStack.getCount() - remainder.getCount();

            if (amountToInsert > 0) {
                sourceStack = sourceInventory.extractItem(srcIndex, amountToInsert, false);
                if (!sourceStack.isEmpty()) {
                    GTTransferUtils.insertItem(targetInventory, sourceStack, false);
                    itemsLeftToTransfer -= sourceStack.getCount();

                    if (itemsLeftToTransfer == 0) {
                        break;
                    }
                }
            }
        }
        return maxTransferAmount - itemsLeftToTransfer;
    }

    @Nonnull
    protected Map<ItemStack, TypeItemInfo> countInventoryItemsByType(@Nonnull IItemHandler inventory) {
        Map<ItemStack, TypeItemInfo> result = new Object2ObjectOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());
        for (int srcIndex = 0; srcIndex < inventory.getSlots(); srcIndex++) {
            ItemStack itemStack = inventory.getStackInSlot(srcIndex);
            if (itemStack.isEmpty()) {
                continue;
            }
            Object transferSlotIndex = itemFilterContainer.matchItemStack(itemStack);
            if (transferSlotIndex == null) {
                continue;
            }

            if (!result.containsKey(itemStack)) {
                TypeItemInfo itemInfo = new TypeItemInfo(itemStack.copy(), transferSlotIndex, new IntArrayList(), 0);
                itemInfo.totalCount += itemStack.getCount();
                itemInfo.slots.add(srcIndex);
                result.put(itemStack.copy(), itemInfo);
            } else {
                TypeItemInfo itemInfo = result.get(itemStack);
                itemInfo.totalCount += itemStack.getCount();
                itemInfo.slots.add(srcIndex);
            }
        }
        return result;
    }

    @Nonnull
    protected Map<Object, GroupItemInfo> countInventoryItemsByMatchSlot(@Nonnull IItemHandler inventory) {
        Map<Object, GroupItemInfo> result = new Object2ObjectOpenHashMap<>();
        for (int srcIndex = 0; srcIndex < inventory.getSlots(); srcIndex++) {
            ItemStack itemStack = inventory.getStackInSlot(srcIndex);
            if (itemStack.isEmpty()) {
                continue;
            }
            Object transferSlotIndex = itemFilterContainer.matchItemStack(itemStack);
            if (transferSlotIndex == null) {
                continue;
            }
            if (!result.containsKey(transferSlotIndex)) {
                GroupItemInfo itemInfo = new GroupItemInfo(transferSlotIndex, new ObjectOpenCustomHashSet<>(ItemStackHashStrategy.comparingAllButCount()), 0);
                itemInfo.itemStackTypes.add(itemStack.copy());
                itemInfo.totalCount += itemStack.getCount();
                result.put(transferSlotIndex, itemInfo);
            } else {
                GroupItemInfo itemInfo = result.get(transferSlotIndex);
                itemInfo.itemStackTypes.add(itemStack.copy());
                itemInfo.totalCount += itemStack.getCount();
            }
        }
        return result;
    }


    protected int doTransferFluids(int transferLimit) {
        TileEntity tileEntity = getNeighbor(getAttachedSide());
        IFluidHandler fluidHandler = tileEntity == null ? null : tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, getAttachedSide().getOpposite());
        IFluidHandler myFluidHandler = getCoverableView().getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, getAttachedSide());
        if (fluidHandler == null || myFluidHandler == null) {
            return 0;
        }
        return doTransferFluidsInternal(myFluidHandler, fluidHandler, transferLimit);
    }

    protected boolean checkInputFluid(FluidStack fluidStack) {
        return fluidFilterContainer.testFluidStack(fluidStack);
    }

    public int getBuffer() {
        return itemsTransferBuffered;
    }

    public void buffer(int amount) {
        itemsTransferBuffered += amount;
    }

    public void clearBuffer() {
        itemsTransferBuffered = 0;
    }

    public void setTransferMode(TransferMode transferMode) {
        this.transferMode = transferMode;
        this.getCoverableView().markDirty();
        this.itemFilterContainer.setMaxStackSize(transferMode.maxStackSize);
    }

    public TransferMode getTransferMode() {
        return transferMode;
    }

    public void setFluidTransferMode(TransferMode fluidTransferMode) {
        this.fluidTransferMode = fluidTransferMode;
        markDirty();
    }

    public TransferMode getFluidTransferMode() {
        return fluidTransferMode;
    }

    public String getFluidTransferAmountString() {
        return Integer.toString(this.bucketMode == BucketMode.BUCKET ? fluidTransferAmount / 1000 : fluidTransferAmount);
    }

    private String getFluidTransferSizeString() {
        int val = fluidTransferAmount;
        if (this.bucketMode == BucketMode.BUCKET) {
            val /= 1000;
        }
        return val == -1 ? "" : TextFormattingUtil.formatLongToCompactString(val);
    }

    public boolean shouldDisplayAmountSlider() {
        if (transferMode == TransferMode.TRANSFER_ANY) {
            return false;
        }
        return this.itemFilterContainer.showGlobalTransferLimitSlider();
    }

    private boolean shouldDisplayFluidAmountSlider() {
        if (this.fluidFilterContainer.getFilterWrapper().getFluidFilter() != null) {
            return false;
        }
        return this.fluidTransferMode == TransferMode.TRANSFER_EXACT || this.fluidTransferMode == TransferMode.KEEP_EXACT;
    }


    @Override
    public boolean canAttach(@Nonnull CoverableView coverable, @Nonnull EnumFacing side) {
        boolean canHandleItems = coverable.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getAttachedSide()) != null;
        boolean canHandleFluids = coverable.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, getAttachedSide()) != null;
        return canHandleItems && canHandleFluids;
    }

    @Override
    public boolean canInteractWithOutputSide() {
        return true;
    }

    @Override
    public void onRemoval() {
        dropInventoryContents(itemFilterContainer.getFilterInventory());
        dropInventoryContents(fluidFilterContainer.getFilterInventory());
    }

    @Override
    public void renderCover(@Nonnull CCRenderState renderState, @Nonnull Matrix4 translation, IVertexOperation[] pipeline, @Nonnull Cuboid6 plateBox, @Nonnull BlockRenderLayer layer) {
        if (conveyorMode == ConveyorMode.EXPORT) {
            Textures.PUMP_OVERLAY.renderSided(getAttachedSide(), plateBox, renderState, pipeline, translation);
        } else {
            Textures.PUMP_OVERLAY_INVERTED.renderSided(getAttachedSide(), plateBox, renderState, pipeline,
                    translation);
        }
    }

    @Nonnull
    @Override
    public EnumActionResult onScrewdriverClick(@Nonnull EntityPlayer playerIn, @Nonnull EnumHand hand, @Nonnull CuboidRayTraceResult hitResult) {
        if (!getCoverableView().getWorld().isRemote) {
            openUI((EntityPlayerMP) playerIn);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, T defaultValue) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (defaultValue == null) {
                return null;
            }
            IItemHandler delegate = (IItemHandler) defaultValue;
            if (itemHandlerWrapper == null || itemHandlerWrapper.delegate != delegate) {
                this.itemHandlerWrapper = new CoverableItemHandlerWrapper(delegate);
            }
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandlerWrapper);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (defaultValue == null) {
                return null;
            }
            IFluidHandler delegate = (IFluidHandler) defaultValue;
            if (fluidHandlerWrapper == null || fluidHandlerWrapper.delegate != delegate) {
                this.fluidHandlerWrapper = new CoverableFluidHandlerWrapper(delegate);
            }
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandlerWrapper);
        }
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        }

        return defaultValue;
    }

    protected String getItemModeUITitle() {
        return "cover.robotic_arm.title";
    }
    protected String getFluidModeUITitle() {
        return "cover.fluid_regulator.title";
    }

    public ModularUI buildUI(ModularUI.Builder builder, EntityPlayer player) {
        WidgetGroup roboticArmUI = createItemUI();
        WidgetGroup fluidRegulatorUI = createFluidUI();

        ServerWidgetGroup roboticArmGroup = new ServerWidgetGroup(() -> this.coverMode == CoverMode.ITEM);
        roboticArmGroup.addWidget(roboticArmUI);

        ServerWidgetGroup fluidRegulatorGroup = new ServerWidgetGroup(() -> this.coverMode == CoverMode.FLUID);
        fluidRegulatorGroup.addWidget(fluidRegulatorUI);

        builder.widget(roboticArmGroup);
        builder.widget(fluidRegulatorGroup);
        return builder.build(this, player);
    }

    @Override
    public ModularUI createUI(EntityPlayer player) {
        WidgetGroup primaryGroup = new WidgetGroup();

        TextureArea buttonTexture = com.cyber.fluidic_arm.gui.GuiTextures.BUTTON_SWITCH_MODE;

        primaryGroup.addWidget(new ImageCycleButtonWidget(125, 167, 18, 18, buttonTexture,
                2, this::getCoverMode, this::setCoverMode)
                .setTooltipHoverString(this::getCoverModeTooltip));

        primaryGroup.addWidget(new CycleButtonWidget(7, 166, 116, 20,
                ManualImportExportMode.class, this::getManualImportExportMode, this::setManualImportExportMode)
                .setTooltipHoverString("cover.universal.manual_import_export.mode.description"));

        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 176, 190 + 82)
                .widget(primaryGroup)
                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 190);

        return buildUI(builder, player);
    }

    public WidgetGroup createItemUI() {
        WidgetGroup primaryGroup = new WidgetGroup();
        primaryGroup.addWidget(new LabelWidget(10, 5, getItemModeUITitle(), GTValues.VN[tier]));

        primaryGroup.addWidget(new IncrementButtonWidget(136, 20, 30, 20, 1, 8, 64, 512, this::adjustTransferRate)
                .setDefaultTooltip()
                .setShouldClientCallback(false));
        primaryGroup.addWidget(new IncrementButtonWidget(10, 20, 30, 20, -1, -8, -64, -512, this::adjustTransferRate)
                .setDefaultTooltip()
                .setShouldClientCallback(false));

        primaryGroup.addWidget(new ImageWidget(40, 20, 96, 20, GuiTextures.DISPLAY));

        primaryGroup.addWidget(new TextFieldWidget2(42, 26, 92, 20, () -> String.valueOf(transferRate), val -> {
                    if (val != null && !val.isEmpty())
                        setTransferRate(MathHelper.clamp(Integer.parseInt(val), 1, maxItemTransferRate));
                })
                        .setNumbersOnly(1, maxItemTransferRate)
                        .setMaxLength(4)
                        .setPostFix("cover.conveyor.transfer_rate")
        );

        primaryGroup.addWidget(new CycleButtonWidget(10, 45, 75, 20,
                ConveyorMode.class, this::getConveyorMode, this::setConveyorMode));

        if (getTileEntityHere() instanceof TileEntityItemPipe ||
                getNeighbor(getAttachedSide()) instanceof TileEntityItemPipe) {
            final ImageCycleButtonWidget distributionModeButton = new ImageCycleButtonWidget(149, 166, 20, 20, GuiTextures.DISTRIBUTION_MODE, 3,
                    () -> distributionMode.ordinal(),
                    val -> setDistributionMode(DistributionMode.values()[val]))
                    .setTooltipHoverString(val -> DistributionMode.values()[val].getName());
            primaryGroup.addWidget(distributionModeButton);
        }

        primaryGroup.addWidget(new CycleButtonWidget(91, 45, 75, 20,
                TransferMode.class, this::getTransferMode, this::setTransferMode)
                .setTooltipHoverString("cover.robotic_arm.transfer_mode.description"));

        ServerWidgetGroup stackSizeGroup = new ServerWidgetGroup(this::shouldDisplayAmountSlider);
        stackSizeGroup.addWidget(new ImageWidget(111, 70, 35, 20, GuiTextures.DISPLAY));

        stackSizeGroup.addWidget(new IncrementButtonWidget(146, 70, 20, 20, 1, 8, 64, 512, itemFilterContainer::adjustTransferStackSize)
                .setDefaultTooltip()
                .setTextScale(0.7f)
                .setShouldClientCallback(false));
        stackSizeGroup.addWidget(new IncrementButtonWidget(91, 70, 20, 20, -1, -8, -64, -512, itemFilterContainer::adjustTransferStackSize)
                .setDefaultTooltip()
                .setTextScale(0.7f)
                .setShouldClientCallback(false));

        stackSizeGroup.addWidget(new TextFieldWidget2(113, 77, 31, 20, () -> String.valueOf(itemFilterContainer.getTransferStackSize()), val -> {
                    if (val != null && !val.isEmpty())
                        itemFilterContainer.setTransferStackSize(MathHelper.clamp(Integer.parseInt(val), 1, transferMode.maxStackSize));
                })
                        .setNumbersOnly(1, transferMode.maxStackSize)
                        .setMaxLength(4)
                        .setScale(0.9f)
        );


        primaryGroup.addWidget(stackSizeGroup);
        this.itemFilterContainer.initUI(70, primaryGroup::addWidget, () -> this.coverMode == CoverMode.ITEM);
        return primaryGroup;
    }

    public WidgetGroup createFluidUI() {
        WidgetGroup primaryGroup = new WidgetGroup();
        WidgetGroup filterGroup = new WidgetGroup();
        primaryGroup.addWidget(new LabelWidget(10, 5, getFluidModeUITitle(), GTValues.VN[tier]));

        primaryGroup.addWidget(new IncrementButtonWidget(136, 20, 30, 20, 1, 10, 100, 1000, this::adjustFluidTransferRate)
                .setDefaultTooltip()
                .setShouldClientCallback(false));
        primaryGroup.addWidget(new IncrementButtonWidget(10, 20, 30, 20, -1, -10, -100, -1000, this::adjustFluidTransferRate)
                .setDefaultTooltip()
                .setShouldClientCallback(false));

        primaryGroup.addWidget(new ImageWidget(40, 20, 66, 20, GuiTextures.DISPLAY));

        TextFieldWidget2 textField = new TextFieldWidget2(45, 26, 60, 20, () -> bucketMode == BucketMode.BUCKET ? Integer.toString(fluidTransferRate / 1000) : Integer.toString(fluidTransferRate), val -> {
            if (val != null && !val.isEmpty()) {
                int amount = Integer.parseInt(val);
                if (this.bucketMode == BucketMode.BUCKET) {
                    amount = IntMath.saturatedMultiply(amount, 1000);
                }
                setFluidTransferRate(amount);
            }
        })
                .setCentered(true)
                .setNumbersOnly(1, bucketMode == BucketMode.BUCKET ? maxFluidTransferRate / 1000 : maxFluidTransferRate)
                .setMaxLength(8);
        primaryGroup.addWidget(textField);

        primaryGroup.addWidget(new CycleButtonWidget(106, 20, 30, 20,
                BucketMode.class, this::getBucketMode, mode -> {
            if (mode != bucketMode) {
                setBucketMode(mode);
            }
        }));

        primaryGroup.addWidget(new CycleButtonWidget(10, 45, 75, 20,
                PumpMode.class, this::getPumpMode, this::setPumpMode));

        filterGroup.addWidget(new CycleButtonWidget(91, 45, 75, 20,
                TransferMode.class, this::getFluidTransferMode, this::setFluidTransferMode)
                .setTooltipHoverString("cover.fluid_regulator.transfer_mode.description"));

        ServerWidgetGroup stackSizeGroup = new ServerWidgetGroup(this::shouldDisplayFluidAmountSlider);
        stackSizeGroup.addWidget(new ImageWidget(111, 70, 35, 20, GuiTextures.DISPLAY));

        stackSizeGroup.addWidget(new IncrementButtonWidget(146, 70, 20, 20, 1, 10, 100, 1000, this::adjustFluidTransferSize)
                .setDefaultTooltip()
                .setTextScale(0.7f)
                .setShouldClientCallback(false));
        stackSizeGroup.addWidget(new IncrementButtonWidget(91, 70, 20, 20, -1, -10, -100, -1000, this::adjustFluidTransferSize)
                .setDefaultTooltip()
                .setTextScale(0.7f)
                .setShouldClientCallback(false));

        stackSizeGroup.addWidget(new TextFieldWidget2(113, 75, 31, 20, this::getFluidTransferAmountString, val -> {
            if (val != null && !val.isEmpty()) {
                int amount = Integer.parseInt(val);
                if (this.bucketMode == BucketMode.BUCKET) {
                    amount = IntMath.saturatedMultiply(amount, 1000);
                }
                setFluidTransferAmount(amount);
            }
        })
                .setCentered(true)
                .setNumbersOnly(1, fluidTransferMode == TransferMode.TRANSFER_EXACT ? maxFluidTransferRate : Integer.MAX_VALUE)
                .setMaxLength(10)
                .setScale(0.6f));

        stackSizeGroup.addWidget(new SimpleTextWidget(129, 85, "", 0xFFFFFF, () -> bucketMode.localeName).setScale(0.6f));


        primaryGroup.addWidget(stackSizeGroup);
        primaryGroup.addWidget(filterGroup);
        this.fluidFilterContainer.initUI(70, primaryGroup::addWidget, () -> this.coverMode == CoverMode.FLUID);
        return primaryGroup;
    }

    public void adjustFluidTransferSize(int amount) {
        if (bucketMode == BucketMode.BUCKET)
            amount *= 1000;
        switch (this.fluidTransferMode) {
            case TRANSFER_EXACT:
                setFluidTransferAmount(MathHelper.clamp(this.fluidTransferAmount + amount, 0, this.maxFluidTransferRate));
                break;
            case KEEP_EXACT:
                setFluidTransferAmount(MathHelper.clamp(this.fluidTransferAmount + amount, 0, Integer.MAX_VALUE));
                break;
        }
    }

    public void setFluidTransferAmount(int fluidTransferAmount) {
        this.fluidTransferAmount = fluidTransferAmount;
        markDirty();
    }

    @Override
    public boolean isWorkingEnabled() {
        return isWorkingAllowed;
    }

    @Override
    public void setWorkingEnabled(boolean isActivationAllowed) {
        this.isWorkingAllowed = isActivationAllowed;
    }

    @Override
    public void readCustomData(int id, @Nonnull PacketBuffer packetBuffer) {
        super.readCustomData(id, packetBuffer);
        if (id == UPDATE_COVER_TYPE_MODE) {
            this.coverMode = packetBuffer.readEnumValue(CoverMode.class);
        }
        if (id == UPDATE_CONVEYOR_MODE) {
            this.conveyorMode = packetBuffer.readEnumValue(ConveyorMode.class);
        }
        if (id == UPDATE_PUMP_MODE) {
            this.pumpMode = packetBuffer.readEnumValue(PumpMode.class);
        }
        getCoverableView().scheduleRenderUpdate();
    }

    @Override
    public void writeInitialSyncData(@Nonnull PacketBuffer packetBuffer) {
        super.writeInitialSyncData(packetBuffer);
        packetBuffer.writeEnumValue(conveyorMode);
        packetBuffer.writeEnumValue(distributionMode);
        packetBuffer.writeEnumValue(pumpMode);
        packetBuffer.writeInt(coverMode.ordinal());
    }

    @Override
    public void readInitialSyncData(@Nonnull PacketBuffer packetBuffer) {
        super.readInitialSyncData(packetBuffer);
        this.conveyorMode = packetBuffer.readEnumValue(ConveyorMode.class);
        this.distributionMode = packetBuffer.readEnumValue(DistributionMode.class);
        this.pumpMode = packetBuffer.readEnumValue(PumpMode.class);
        this.coverMode = CoverMode.values()[packetBuffer.readInt()];
    }


    @Override
    public void writeToNBT(@Nonnull NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setInteger("CoverMode", coverMode.ordinal());
        tagCompound.setInteger("TransferRate", transferRate);
        tagCompound.setInteger("ConveyorMode", conveyorMode.ordinal());
        tagCompound.setInteger("DistributionMode", distributionMode.ordinal());
        tagCompound.setBoolean("WorkingAllowed", isWorkingAllowed);
        tagCompound.setInteger("ManualImportExportMode", manualImportExportMode.ordinal());
        tagCompound.setTag("ItemFilter", this.itemFilterContainer.serializeNBT());

        tagCompound.setInteger("FluidTransferRate", fluidTransferRate);
        tagCompound.setInteger("PumpMode", pumpMode.ordinal());
        tagCompound.setBoolean("WorkingAllowed", isWorkingAllowed);
        tagCompound.setInteger("ManualImportExportMode", manualImportExportMode.ordinal());
        tagCompound.setTag("FluidFilter", this.fluidFilterContainer.serializeNBT());

        tagCompound.setInteger("TransferMode", transferMode.ordinal());
        tagCompound.setInteger("FluidTransferMode", fluidTransferMode.ordinal());
        tagCompound.setInteger("FluidTransferAmount", fluidTransferAmount);
        tagCompound.setTag("filterv2", new NBTTagCompound());
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        this.coverMode = CoverMode.values()[tagCompound.getInteger("CoverMode")];
        this.transferRate = tagCompound.getInteger("TransferRate");
        this.conveyorMode = ConveyorMode.values()[tagCompound.getInteger("ConveyorMode")];
        this.distributionMode = DistributionMode.values()[tagCompound.getInteger("DistributionMode")];
        this.isWorkingAllowed = tagCompound.getBoolean("WorkingAllowed");
        this.manualImportExportMode = ManualImportExportMode.values()[tagCompound.getInteger("ManualImportExportMode")];
        this.itemFilterContainer.deserializeNBT(tagCompound.getCompoundTag("ItemFilter"));

        this.fluidTransferRate = tagCompound.getInteger("FluidTransferRate");
        this.pumpMode = PumpMode.values()[tagCompound.getInteger("PumpMode")];
        this.isWorkingAllowed = tagCompound.getBoolean("WorkingAllowed");
        this.manualImportExportMode = ManualImportExportMode.values()[tagCompound.getInteger("ManualImportExportMode")];
        this.fluidFilterContainer.deserializeNBT(tagCompound.getCompoundTag("FluidFilter"));

        this.transferMode = TransferMode.values()[tagCompound.getInteger("TransferMode")];
        this.fluidTransferMode = TransferMode.values()[tagCompound.getInteger("FluidTransferMode")];
        //legacy NBT tag
        if (!tagCompound.hasKey("filterv2") && tagCompound.hasKey("FluidTransferAmount")) {
            FluidFilter filter = getFluidFilterContainer().getFilterWrapper().getFluidFilter();
            if (filter != null) {
                filter.configureFilterTanks(tagCompound.getInteger("FluidTransferAmount"));
            }
        }
        this.fluidTransferAmount = tagCompound.getInteger("FluidTransferAmount");
    }

    @Nonnull
    @Override
    @SideOnly(Side.CLIENT)
    protected TextureAtlasSprite getPlateSprite() {
        return Textures.VOLTAGE_CASINGS[this.tier].getSpriteOnSide(SimpleSidedCubeRenderer.RenderSide.SIDE);
    }

    private class CoverableItemHandlerWrapper extends ItemHandlerDelegate {

        public CoverableItemHandlerWrapper(IItemHandler delegate) {
            super(delegate);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (conveyorMode == ConveyorMode.EXPORT && manualImportExportMode == ManualImportExportMode.DISABLED) {
                return stack;
            }
            if (manualImportExportMode == ManualImportExportMode.FILTERED && !itemFilterContainer.testItemStack(stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (conveyorMode == ConveyorMode.IMPORT && manualImportExportMode == ManualImportExportMode.DISABLED) {
                return ItemStack.EMPTY;
            }
            if (manualImportExportMode == ManualImportExportMode.FILTERED) {
                ItemStack result = super.extractItem(slot, amount, true);
                if (result.isEmpty() || !itemFilterContainer.testItemStack(result)) {
                    return ItemStack.EMPTY;
                }
                return simulate ? result : super.extractItem(slot, amount, false);
            }
            return super.extractItem(slot, amount, simulate);
        }
    }

    protected static class GroupItemInfo {
        public final Object filterSlot;
        public final Set<ItemStack> itemStackTypes;
        public int totalCount;

        public GroupItemInfo(Object filterSlot, Set<ItemStack> itemStackTypes, int totalCount) {
            this.filterSlot = filterSlot;
            this.itemStackTypes = itemStackTypes;
            this.totalCount = totalCount;
        }
    }

    protected static class TypeItemInfo {

        public final ItemStack itemStack;
        public final Object filterSlot;
        public final IntList slots;
        public int totalCount;

        public TypeItemInfo(ItemStack itemStack, Object filterSlot, IntList slots, int totalCount) {
            this.itemStack = itemStack;
            this.filterSlot = filterSlot;
            this.slots = slots;
            this.totalCount = totalCount;
        }
    }

    private class CoverableFluidHandlerWrapper extends FluidHandlerDelegate {

        public CoverableFluidHandlerWrapper(IFluidHandler delegate) {
            super(delegate);
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (pumpMode == PumpMode.EXPORT && manualImportExportMode == ManualImportExportMode.DISABLED) {
                return 0;
            }
            if (!checkInputFluid(resource) && manualImportExportMode == ManualImportExportMode.FILTERED) {
                return 0;
            }
            return super.fill(resource, doFill);
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (pumpMode == PumpMode.IMPORT && manualImportExportMode == ManualImportExportMode.DISABLED) {
                return null;
            }
            if (manualImportExportMode == ManualImportExportMode.FILTERED && !checkInputFluid(resource)) {
                return null;
            }
            return super.drain(resource, doDrain);
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (pumpMode == PumpMode.IMPORT && manualImportExportMode == ManualImportExportMode.DISABLED) {
                return null;
            }
            if (manualImportExportMode == ManualImportExportMode.FILTERED) {
                FluidStack result = super.drain(maxDrain, false);
                if (result == null || result.amount <= 0 || !checkInputFluid(result)) {
                    return null;
                }
                return doDrain ? super.drain(maxDrain, true) : result;
            }
            return super.drain(maxDrain, doDrain);
        }
    }
}
