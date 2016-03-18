package org.cyclops.cyclopscore.tileentity;

import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.cyclops.cyclopscore.inventory.INBTInventory;

import java.util.Map;

/**
 * A TileEntity with an internal inventory.
 * @author rubensworks
 *
 */
public abstract class InventoryTileEntityBase extends CyclopsTileEntity implements ISidedInventory {

    protected boolean sendUpdateOnInventoryChanged = false;
    protected final Map<EnumFacing, IItemHandler> sidedInventoryHandlers;

    public InventoryTileEntityBase() {
        this.sidedInventoryHandlers = Maps.newHashMap();
        for(EnumFacing side : EnumFacing.VALUES) {
            addCapabilitySided(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side, new SidedInvWrapper(this, side));
        }
    }
    
    /**
     * Get the internal inventory.
     * @return The inventory.
     */
    public abstract INBTInventory getInventory();
    
    @Override
    public int getSizeInventory() {
        return getInventory().getSizeInventory();
    }
    
    @Override
    public ItemStack getStackInSlot(int slotId) {
        if(slotId >= getSizeInventory() || slotId < 0)
            return null;
        return getInventory().getStackInSlot(slotId);
    }

    @Override
    public ItemStack decrStackSize(int slotId, int count) {
        ItemStack itemStack  = getInventory().decrStackSize(slotId, count);
        onInventoryChanged();
        return itemStack;
    }

    @Override
    public ItemStack removeStackFromSlot(int slotId) {
        return getInventory().removeStackFromSlot(slotId);
    }

    @Override
    public void setInventorySlotContents(int slotId, ItemStack itemstack) {
        getInventory().setInventorySlotContents(slotId, itemstack);
        onInventoryChanged();
    }

    protected void onInventoryChanged() {
        if(isSendUpdateOnInventoryChanged())
            sendUpdate();
    }

    @Override
    public String getName() {
        return getInventory().getName();
    }

    @Override
    public boolean hasCustomName() {
        return getInventory().hasCustomName();
    }

    @Override
    public ITextComponent getDisplayName() {
        return null;
    }

    @Override
    public int getInventoryStackLimit() {
        return getInventory().getInventoryStackLimit();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityPlayer) {
        return worldObj.getTileEntity(getPos()) == this && entityPlayer.getDistanceSq(getPos().add(0.5D, 0.5D, 0.5D)) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer playerIn) {
        getInventory().openInventory(playerIn);
    }

    @Override
    public void closeInventory(EntityPlayer playerIn) {
        getInventory().closeInventory(playerIn);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return getInventory().isItemValidForSlot(index, stack);
    }

    @Override
    public int getField(int id) {
        return getInventory().getField(id);
    }

    @Override
    public void setField(int id, int value) {
        getInventory().setField(id, value);
    }

    @Override
    public int getFieldCount() {
        return getInventory().getFieldCount();
    }

    @Override
    public void clear() {
        getInventory().clear();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        INBTInventory inventory = getInventory();
        if(inventory != null) {
            inventory.readFromNBT(tag);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        INBTInventory inventory = getInventory();
        if(inventory != null) {
            inventory.writeToNBT(tag);
        }
    }
    
    protected boolean canAccess(int slot, EnumFacing side) {
        boolean canAccess = false;
        for(int slotAccess : getSlotsForFace(side)) {
            if(slotAccess == slot)
                canAccess = true;
        }
        return canAccess;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack itemStack, EnumFacing side) {
        return canAccess(slot, side) && this.isItemValidForSlot(slot, itemStack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, EnumFacing side) {
        return canAccess(slot, side);
    }

    /**
     * If this tile should send blockState updates when the inventory has changed.
     * @return If it should send blockState updates.
     */
    public boolean isSendUpdateOnInventoryChanged() {
        return sendUpdateOnInventoryChanged;
    }

    /**
     * If this tile should send blockState updates when the inventory has changed.
     * @param sendUpdateOnInventoryChanged If it should send blockState updates.
     */
    public void setSendUpdateOnInventoryChanged(
            boolean sendUpdateOnInventoryChanged) {
        this.sendUpdateOnInventoryChanged = sendUpdateOnInventoryChanged;
    }
    
}
