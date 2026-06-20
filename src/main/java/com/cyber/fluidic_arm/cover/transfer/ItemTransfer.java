package com.cyber.fluidic_arm.cover.transfer;

import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Map;

public class ItemTransfer {
    private ItemTransfer() { }

    public static Map<ItemStack, TypeItemInfo> countByType(IItemHandler inventory, ItemFilter filter) {
        Map<ItemStack, TypeItemInfo> result = new Object2ObjectOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());

        for (var srcIndex = 0; srcIndex < inventory.getSlots(); srcIndex++) {
            var itemStack = inventory.getStackInSlot(srcIndex);
            if (itemStack.isEmpty() || !filter.test(itemStack)) continue;

            var itemInfo = result.computeIfAbsent(itemStack, s -> new TypeItemInfo(s, new IntArrayList(), 0));
            itemInfo.totalCount += itemStack.getCount();
            itemInfo.slots.add(srcIndex);
        }

        return result;
    }

    public static Map<ItemStack, GroupItemInfo> countByMatchSlot(IItemHandler inventory, ItemFilter filter) {
        Map<ItemStack, GroupItemInfo> result = new Object2ObjectOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());

        for (int srcIndex = 0; srcIndex < inventory.getSlots(); srcIndex++) {
            var itemStack = inventory.getStackInSlot(srcIndex);
            if (itemStack.isEmpty() || !filter.test(itemStack)) continue;

            var itemInfo = result.computeIfAbsent(itemStack, s -> new GroupItemInfo(s, 0));
            itemInfo.totalCount += itemStack.getCount();
        }

        return result;
    }

    public static int moveAny(IItemHandler sourceInventory, IItemHandler targetInventory, ItemFilter filter, int maxTransferAmount) {
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

    public static int moveByGroup(IItemHandler sourceInventory, IItemHandler targetInventory, Map<ItemStack, GroupItemInfo> itemInfos, ItemFilter filter, int maxTransferAmount) {
        var itemsLeftToTransfer = maxTransferAmount;

        for (var i = 0; i < sourceInventory.getSlots(); i++) {
            var itemStack = sourceInventory.getStackInSlot(i);
            if (itemStack.isEmpty() || !filter.test(itemStack) || !itemInfos.containsKey(itemStack)) continue;

            var itemInfo = itemInfos.get(itemStack);
            var extractedStack = sourceInventory.extractItem(i, Math.min(itemInfo.totalCount, itemsLeftToTransfer), true);
            var remainderStack = ItemHandlerHelper.insertItemStacked(targetInventory, extractedStack, true);
            var amountToInsert = extractedStack.getCount() - remainderStack.getCount();

            if (amountToInsert > 0) {
                extractedStack = sourceInventory.extractItem(i, amountToInsert, false);
                if (!extractedStack.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(targetInventory, extractedStack, false);
                    itemsLeftToTransfer -= extractedStack.getCount();
                    itemInfo.totalCount -= extractedStack.getCount();

                    if (itemInfo.totalCount == 0) {
                        itemInfos.remove(itemStack);
                        if (itemInfos.isEmpty()) break;
                    }

                    if (itemsLeftToTransfer == 0) break;
                }
            }
        }

        return maxTransferAmount - itemsLeftToTransfer;
    }

    public static boolean moveExact(IItemHandler sourceInventory, IItemHandler targetInventory, TypeItemInfo itemInfo) {
        var resultStack = itemInfo.itemStack.copy();

        var totalExtractedCount = extractFromSlots(sourceInventory, resultStack, itemInfo.slots, itemInfo.totalCount, true);
        if (totalExtractedCount != itemInfo.totalCount) return false;

        resultStack.setCount(totalExtractedCount);
        var remainder = ItemHandlerHelper.insertItem(targetInventory, resultStack, true);
        if (!remainder.isEmpty()) return false;

        ItemHandlerHelper.insertItem(targetInventory, resultStack, false);
        extractFromSlots(sourceInventory, resultStack, itemInfo.slots, itemInfo.totalCount, false);

        return true;
    }

    private static int extractFromSlots(IItemHandler inventory, ItemStack matchStack, IntList slots, int totalToExtract, boolean simulate) {
        var remaining = totalToExtract;
        for (var i = 0; i < slots.size(); i++) {
            var extracted = inventory.extractItem(slots.getInt(i), remaining, simulate);
            if (!extracted.isEmpty() && GTUtil.isSameItemSameTags(matchStack, extracted)) {
                remaining -= extracted.getCount();
            }
            if (remaining == 0) break;
        }

        // How many items were extracted
        return totalToExtract - remaining;
    }

    public static class TypeItemInfo {
        public final ItemStack itemStack;
        public final IntList slots;
        public int totalCount;

        public TypeItemInfo(ItemStack itemStack, IntList slots, int totalCount) {
            this.itemStack = itemStack;
            this.slots = slots;
            this.totalCount = totalCount;
        }
    }

    public static class GroupItemInfo {
        public final ItemStack itemStack;
        public int totalCount;

        public GroupItemInfo(ItemStack itemStack, int totalCount) {
            this.itemStack = itemStack;
            this.totalCount = totalCount;
        }
    }
}