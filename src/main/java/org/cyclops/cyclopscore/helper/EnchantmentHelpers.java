package org.cyclops.cyclopscore.helper;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * This class contains helper methods to get and set certain enchants
 * and enchantment levels on item stacks.
 * @author immortaleeb
 *
 */
public class EnchantmentHelpers {

	/**
	 * Checks if an itemStack has a certain enchantment.
	 * @param itemStack The itemStack to check.
	 * @param enchantment The Enchantment to compare.
	 * @return The id of the enchantment in the enchantmentlist or -1 if it does not apply.
	 */
	public static int doesEnchantApply(ItemStack itemStack, Enchantment enchantment) {
	    if(itemStack != null) {
	        NBTTagList enchantmentList = itemStack.getEnchantmentTagList();
	        if(enchantmentList != null) {
	            for(int i = 0; i < enchantmentList.tagCount(); i++) {
	                if (enchantmentList.getCompoundTagAt(i).getShort("id") == Enchantment.getEnchantmentID(enchantment)) {
	                    return i;
	                }
	            }
	        }
	    }
	    return -1;
	}

	/**
	 * Returns the level of an enchantment given an itemStack and the list id
	 * of the enchantment in the enchantmentlist (see doesEnchantApply() to get
	 * the id in the enchantmentlist)
	 * @param itemStack The itemStack which contains the enchanted item
	 * @param enchantmentListID The id of the enchantment in the enchantment list
	 * @return The level of the enchantment on the given item
	 */
	public static int getEnchantmentLevel(ItemStack itemStack, int enchantmentListID) {
	    NBTTagList enchlist = itemStack.getEnchantmentTagList();
	    return enchlist.getCompoundTagAt(enchantmentListID).getShort("lvl");
	}

	/**
	 * Returns the id of an enchantment given an itemStack and the list id
	 * of the enchantment in the enchantmentlist (see doesEnchantApply() to get
	 * the id in the enchantmentlist)
	 * @param itemStack The itemStack which contains the enchanted item
	 * @param enchantmentListID The id of the enchantment in the enchantment list
	 * @return The id of the enchantment on the given item
	 */
	public static int getEnchantmentID(ItemStack itemStack, int enchantmentListID) {
	    NBTTagList enchlist = itemStack.getEnchantmentTagList();
	    return enchlist.getCompoundTagAt(enchantmentListID).getShort("id");
	}

	/**
	 * Sets the level of an enchantment given an itemStack and the id
	 * of the enchantment in the enchantmentlist (see doesEnchantApply() to get
	 * the id in the enchantmentlist)
	 * Will clear the enchantment if the new level &lt;= 0
	 * @param itemStack The itemStack which contains the enchanted item
	 * @param enchantmentListID The id of the enchantment in the enchantment list
	 * @param level The new level of the enchantment on the given item
	 */
	public static void setEnchantmentLevel(ItemStack itemStack, int enchantmentListID, int level) {
	    NBTTagList enchlist = itemStack.getEnchantmentTagList();
	    if(level <= 0) {
	        enchlist.removeTag(enchantmentListID);
	        if(enchlist.tagCount() == 0) {
	            itemStack.getTagCompound().removeTag("ench");
	        }
	    } else {
	    	NBTTagCompound compound = enchlist.getCompoundTagAt(enchantmentListID);
	        compound.setShort("lvl", (short) level);
	    }
		NBTTagCompound tag = ItemStackHelpers.getSafeTagCompound(itemStack);
		tag.setTag("ench", enchlist);
	}

	/**
	 * Sets the level of an enchantment given an itemStack and the id
	 * of the enchantment in the enchantmentlist (see doesEnchantApply() to get
	 * the id in the enchantmentlist)
	 * Will clear the enchantment if the new level &lt;= 0
	 * @param itemStack The itemStack which contains the enchanted item
	 * @param enchantment The enchantment
	 * @param level The new level of the enchantment on the given item
	 */
	public static void setEnchantmentLevel(ItemStack itemStack, Enchantment enchantment, int level) {
		int existingIndex;
	    if((existingIndex = doesEnchantApply(itemStack, enchantment)) >= 0) {
	    	setEnchantmentLevel(itemStack, existingIndex, level);
	    } else {
	    	itemStack.addEnchantment(enchantment, level);
	    }
	}

}
