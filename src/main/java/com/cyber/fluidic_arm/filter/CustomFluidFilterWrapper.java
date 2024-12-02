package com.cyber.fluidic_arm.filter;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.ServerWidgetGroup;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.util.IDirtyNotifiable;
import gregtech.api.util.Position;
import gregtech.common.covers.filter.FluidFilter;
import gregtech.common.covers.filter.WidgetGroupFluidFilter;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CustomFluidFilterWrapper {

    private final IDirtyNotifiable dirtyNotifiable;
    private boolean isBlacklistFilter = false;
    private FluidFilter currentFluidFilter;
    private Supplier<Boolean> showTipSupplier;
    private int maxSize = 1000;

    public CustomFluidFilterWrapper(IDirtyNotifiable dirtyNotifiable, int maxSize) {
        this.dirtyNotifiable = dirtyNotifiable;
        this.maxSize = maxSize;
    }

    public CustomFluidFilterWrapper(IDirtyNotifiable dirtyNotifiable) {
        this.dirtyNotifiable = dirtyNotifiable;
    }

    public void initUI(int y, Consumer<Widget> widgetGroup) {
        WidgetGroupFluidFilter widgetGroupFluidFilter = new WidgetGroupFluidFilter(y, this::getFluidFilter, shouldShowTip());
        widgetGroupFluidFilter.setSelfPosition(new Position(0, y));
        widgetGroup.accept(widgetGroupFluidFilter);
    }

    public void blacklistUI(int y, Consumer<Widget> widgetGroup, BooleanSupplier showBlacklistButton) {
        ServerWidgetGroup blacklistButton = new ServerWidgetGroup(() -> getFluidFilter() != null);
        blacklistButton.addWidget(new ToggleButtonWidget(144, y, 20, 20, GuiTextures.BUTTON_BLACKLIST,
                this::isBlacklistFilter, this::setBlacklistFilter).setPredicate(showBlacklistButton).setTooltipText("cover.filter.blacklist"));
        widgetGroup.accept(blacklistButton);
    }

    public void setFluidFilter(FluidFilter fluidFilter) {
        this.currentFluidFilter = fluidFilter;
        if (currentFluidFilter != null) {
            currentFluidFilter.setDirtyNotifiable(dirtyNotifiable);
            currentFluidFilter.setMaxConfigurableFluidSize(maxSize);
        }
    }

    private Supplier<Boolean> shouldShowTip() {
        return showTipSupplier;
    }

    protected void setTipSupplier(Supplier<Boolean> supplier) {
        this.showTipSupplier = supplier;
    }

    public FluidFilter getFluidFilter() {
        return currentFluidFilter;
    }

    public void onFilterInstanceChange() {
        dirtyNotifiable.markAsDirty();
    }

    public void setBlacklistFilter(boolean blacklistFilter) {
        isBlacklistFilter = blacklistFilter;
        dirtyNotifiable.markAsDirty();
    }

    public boolean isBlacklistFilter() {
        return isBlacklistFilter;
    }

    public boolean testFluidStack(FluidStack fluidStack, boolean whitelist) {
        boolean result = true;
        if (currentFluidFilter != null) {
            result = currentFluidFilter.testFluid(fluidStack);
        }
        if (!whitelist) {
            result = !result;
        }
        return result;
    }

    public boolean testFluidStack(FluidStack fluidStack) {
        boolean result = true;
        if (currentFluidFilter != null) {
            result = currentFluidFilter.testFluid(fluidStack);
        }
        if (isBlacklistFilter) {
            result = !result;
        }
        return result;
    }
}
