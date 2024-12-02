package com.cyber.fluidic_arm.filter;

import com.cyber.fluidic_arm.widgets.CustomSlotWidget;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.util.IDirtyNotifiable;
import gregtech.common.covers.filter.FilterTypeRegistry;
import gregtech.common.covers.filter.FluidFilter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CustomFluidFilterContainer implements INBTSerializable<NBTTagCompound> {

    private final ItemStackHandler filterInventory;
    private final CustomFluidFilterWrapper filterWrapper;

    private WidgetGroup group;

    public CustomFluidFilterContainer(IDirtyNotifiable dirtyNotifiable, int capacity) {
        this.filterWrapper = new CustomFluidFilterWrapper(dirtyNotifiable, capacity);
        this.filterInventory = new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return FilterTypeRegistry.getFluidFilterForStack(stack) != null;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onLoad() {
                onFilterSlotChange(false);
            }

            @Override
            protected void onContentsChanged(int slot) {
                onFilterSlotChange(true);
            }
        };
    }

    public CustomFluidFilterContainer(IDirtyNotifiable dirtyNotifiable, Supplier<Boolean> showTip, int maxSize) {
        this(dirtyNotifiable, maxSize);
        filterWrapper.setTipSupplier(showTip);
    }

    public CustomFluidFilterContainer(IDirtyNotifiable dirtyNotifiable) {
        this(dirtyNotifiable, 1000);
        filterWrapper.setTipSupplier(() -> false);
    }

    public ItemStackHandler getFilterInventory() {
        return filterInventory;
    }

    public CustomFluidFilterWrapper getFilterWrapper() {
        return filterWrapper;
    }

    public void initUI(int y, Consumer<Widget> widgetGroup, BooleanSupplier displaySupplier) {
        widgetGroup.accept(new LabelWidget(10, y, "cover.pump.fluid_filter.title"));
        widgetGroup.accept(new CustomSlotWidget(filterInventory, 0, 10, y + 15, "Fluid", displaySupplier)
                .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.FILTER_SLOT_OVERLAY));

        this.filterWrapper.initUI(y + 38, widgetGroup);
        this.filterWrapper.blacklistUI(y + 38, widgetGroup, ()-> true);
    }

    protected void onFilterSlotChange(boolean notify) {
        ItemStack filterStack = filterInventory.getStackInSlot(0);
        FluidFilter newFluidFilter = FilterTypeRegistry.getFluidFilterForStack(filterStack);
        FluidFilter currentFluidFilter = filterWrapper.getFluidFilter();
        if (newFluidFilter == null) {
            if (currentFluidFilter != null) {
                filterWrapper.setFluidFilter(null);
                if (notify) filterWrapper.onFilterInstanceChange();
            }
        } else if (currentFluidFilter == null ||
                newFluidFilter.getClass() != currentFluidFilter.getClass()) {
            filterWrapper.setFluidFilter(newFluidFilter);
            if (notify) filterWrapper.onFilterInstanceChange();
        }
    }

    public boolean testFluidStack(FluidStack fluidStack) {
        return filterWrapper.testFluidStack(fluidStack);
    }

    public boolean testFluidStack(FluidStack fluidStack, Boolean whitelist) {
        return filterWrapper.testFluidStack(fluidStack, whitelist);
    }

    public void SetActive(boolean active) {

    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setTag("FilterInventory", filterInventory.serializeNBT());
        tagCompound.setBoolean("IsBlacklist", filterWrapper.isBlacklistFilter());
        if (filterWrapper.getFluidFilter() != null) {
            NBTTagCompound filterInventory = new NBTTagCompound();
            filterWrapper.getFluidFilter().writeToNBT(filterInventory);
            tagCompound.setTag("Filter", filterInventory);
        }
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tagCompound) {
        this.filterInventory.deserializeNBT(tagCompound.getCompoundTag("FilterInventory"));
        this.filterWrapper.setBlacklistFilter(tagCompound.getBoolean("IsBlacklist"));
        if (filterWrapper.getFluidFilter() != null) {
            this.filterWrapper.getFluidFilter().readFromNBT(tagCompound.getCompoundTag("Filter"));
        }
    }
}
