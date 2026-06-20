package com.cyber.fluidic_arm.cover;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IIOCover;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.api.transfer.fluid.ModifiableFluidHandlerWrapper;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.DistributionMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
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
import net.minecraftforge.items.ItemHandlerHelper;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.util.List;

public class FluidicArmCover extends CoverBehavior implements IIOCover, IUICover {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidicArmCover.class, CoverBehavior.MANAGED_FIELD_HOLDER);

    public final int tier;

    // region Robot Arm Fields

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

    // region Fluid Regulator Fields

    public final int maxFluidTransferRate;
    protected int mbLeftToTransferLastSecond;

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
    protected final FilterHandler<FluidStack, FluidFilter> fluidFilterHandler;

    // endregion

    protected final ConditionalSubscriptionHandler subscriptionHandler;

    public FluidicArmCover(CoverDefinition coverDefinition, ICoverable coverHolder, Direction attachedSide, int tier) {
        super(coverDefinition, coverHolder, attachedSide);
        this.tier = tier;

        // Robot Arm
        this.maxItemTransferRate = FARates.itemTransferRate(tier); // 8, 32, 128, 512, 2048, 8192
        this.transferRate = maxItemTransferRate;
        this.itemsLeftToTransferLastSecond = transferRate;
        this.io = IO.OUT;
        this.distributionMode = DistributionMode.INSERT_FIRST;
        this.itemFilterHandler = FilterHandlers.item(this);

        // Fluid Regulator
        this.maxFluidTransferRate = FARates.fluidTransferRate(tier); // 64, 256, 1024, 4096, 16384, 65536
        this.fluidIo = IO.OUT;
        this.fluidTransferRate = maxFluidTransferRate;
        this.mbLeftToTransferLastSecond = fluidTransferRate * 20;
        this.fluidFilterHandler = FilterHandlers.fluid(this);

        this.subscriptionHandler = new ConditionalSubscriptionHandler(coverHolder, this::update, this::isSubscriptionActive);
    }

    @Override
    @MethodsReturnNonnullByDefault
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

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

    protected boolean isSubscriptionActive() {
        return isWorkingEnabled && (getAdjacentItemHandler() != null || getAdjacentFluidHandler() != null);
    }

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

    protected int doTransferFluidsInternal(IFluidHandler sourceInventory, IFluidHandler targetInventory, int platformTransferLimit) {
        return GTTransferUtils.transferFluidsFiltered(sourceInventory, targetInventory, fluidFilterHandler.getFilter(), platformTransferLimit);
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

    // region Fluid Handlers

    protected @Nullable IFluidHandlerModifiable getOwnFluidHandler() {
        return coverHolder.getFluidHandlerCap(attachedSide, false);
    }

    protected @Nullable IFluidHandler getAdjacentFluidHandler() {
        return GTTransferUtils.getAdjacentFluidHandler(coverHolder.getLevel(), coverHolder.getPos(), attachedSide).resolve().orElse(null);
    }

    // endregion

    // region Robot Arm Getters

    @Override
    public IO getIo() {
        return io;
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

    // region Robot Arm Setters

    public void setIo(IO io) {
        this.io = io;
    }

    public void setTransferRate(int transferRate) {
        this.transferRate = transferRate;
    }

    public void setDistributionMode(DistributionMode mode) {
        this.distributionMode = mode;
    }

    // endregion

    // region Fluid Regulator Getters

    public IO getFluidIo() {
        return fluidIo;
    }

    public int getFluidTransferRate() {
        return fluidTransferRate;
    }

    public BucketMode getBucketMode() {
        return bucketMode;
    }

    public FilterHandler<FluidStack, FluidFilter> getFluidFilterHandler() {
        return fluidFilterHandler;
    }

    // endregion

    // region Fluid Regulator Setters

    public void setFluidIo(IO fluidIo) {
        this.fluidIo = fluidIo;
    }

    public void setFluidTransferRate(int fluidTransferRate) {
        this.fluidTransferRate = fluidTransferRate;
    }

    public void setBucketMode(BucketMode bucketMode) {
        this.bucketMode = bucketMode;
    }

    // endregion

    @Override
    public ManualIOMode getManualIOMode() {
        return manualIOMode;
    }

    public void setManualIOMode(ManualIOMode mode) {
        this.manualIOMode = mode;
    }

    @Override
    public Widget createUIWidget() {
        var root = new WidgetGroup(0, 0, 176, 137);
        var tabs = new TabContainer(0, 0, 176, 137);

        tabs.addTab(new TabButton(-17, 8, 20, 20).setTexture(
                new GuiTextureGroup(TabContainer.TABS_LEFT.getSubTexture(0, 1f/3, 0.5f, 1f/3), new TextTexture("I")),
                new GuiTextureGroup(TabContainer.TABS_LEFT.getSubTexture(0.5f, 1f/3, 0.5f, 1f/3), new TextTexture("I"))),
                createItemConfigGroup()
        );

        tabs.addTab(new TabButton(-17, 28, 20, 20).setTexture(
                new GuiTextureGroup(TabContainer.TABS_LEFT.getSubTexture(0, 1f/3, 0.5f, 1f/3), new TextTexture("F")),
                new GuiTextureGroup(TabContainer.TABS_LEFT.getSubTexture(0.5f, 1f/3, 0.5f, 1f/3), new TextTexture("F"))),
                createFluidConfigGroup()
        );

        root.addWidget(tabs);
        return root;
    }

    private WidgetGroup createItemConfigGroup() {
        var group = new WidgetGroup(0, 0, 176, 137);
        group.addWidget(new LabelWidget(10, 5, Component.translatable("cover.fluidic_arm.robot_arm.title", GTValues.VN[tier]).getString()));
        group.addWidget(new IntInputWidget(10, 20, 156, 20, () -> this.transferRate, this::setTransferRate).setMin(1).setMax(maxItemTransferRate));
        group.addWidget(new EnumSelectorWidget<>(10, 45, 20, 20, List.of(IO.IN, IO.OUT), io, this::setIo));
        group.addWidget(new EnumSelectorWidget<>(146, 107, 20, 20, ManualIOMode.VALUES, manualIOMode, this::setManualIOMode)).setHoverTooltips("cover.universal.manual_import_export.mode.description");
        group.addWidget(itemFilterHandler.createFilterSlotUI(125, 108));
        group.addWidget(itemFilterHandler.createFilterConfigUI(10, 72, 156, 60));
        return group;
    }

    private WidgetGroup createFluidConfigGroup() {
        var group = new WidgetGroup(0, 0, 176, 137);
        group.addWidget(new LabelWidget(10, 5, Component.translatable("cover.fluidic_arm.fluid_regulator.title", GTValues.VN[tier]).getString()));
        group.addWidget(new IntInputWidget(10, 20, 156, 20, () -> this.fluidTransferRate, this::setFluidTransferRate).setMin(1).setMax(maxFluidTransferRate));
        group.addWidget(new EnumSelectorWidget<>(10, 45, 20, 20, List.of(IO.IN, IO.OUT), fluidIo, this::setFluidIo));
        group.addWidget(new EnumSelectorWidget<>(146, 107, 20, 20, ManualIOMode.VALUES, manualIOMode, this::setManualIOMode).setHoverTooltips("cover.universal.manual_import_export.mode.description"));
        group.addWidget(fluidFilterHandler.createFilterSlotUI(125, 108));
        group.addWidget(fluidFilterHandler.createFilterConfigUI(10, 72, 156, 60));
        return group;
    }
}
