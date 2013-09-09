package factorization.common;

import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.notify.Notify;

public class TileEntityCaliometricBurner extends TileEntityFactorization implements IDataSerializable {
    ItemStack stomache;
    int foodQuality = 0;
    int ticksUntilNextDigestion = 0;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CALIOMETRIC_BURNER;
    }

    @Override
    public String getInvName() {
        return "Caliometric Burner";
    }
    
    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        stomache = data.as(Share.PRIVATE, prefix + "stomache").putItemStack(stomache);
        foodQuality = data.as(Share.PRIVATE, prefix + "food").putInt(foodQuality);
        ticksUntilNextDigestion = data.as(Share.PRIVATE, prefix + "digest").putInt(ticksUntilNextDigestion);
        return this;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        try {
            (new DataOutNBT(tag)).as(Share.PRIVATE, "").put(this);
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        try {
            (new DataInNBT(tag)).as(Share.PRIVATE, "").put(this);
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i == 0) {
            return stomache;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (i == 0) {
            stomache = itemstack;
        }
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        if (itemstack == null) {
            return false;
        }
        return getFoodValue(itemstack) > 0;
    }

    private static final int[] nomslots = new int[] {0}, emptySlots = new int[] {};
    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (ForgeDirection.getOrientation(side).offsetY != 0) {
            return emptySlots; //Food goes in through the teeth
        }
        return nomslots;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        if (dir.offsetY != 0) {
            return BlockIcons.caliometric_top;
        }
        return BlockIcons.caliometric_side;
    }

    @Override
    void doLogic() {
        needLogic();
        if (ticksUntilNextDigestion > 0 && foodQuality > 0) {
            Coord here = getCoord();
            if (here.isPowered()) {
                return;
            }
            for (Coord c : here.getRandomNeighborsAdjacent()) {
                TileEntitySolarBoiler boiler = c.getTE(TileEntitySolarBoiler.class);
                if (boiler == null) {
                    continue;
                }
                boiler.applyHeat(foodQuality);
                break;
            }
        }
        ticksUntilNextDigestion--;
        if (ticksUntilNextDigestion <= 0) {
            foodQuality = consumeFood();
        }
    }
    
    @Override
    int getLogicSpeed() {
        return 1;
    }
    
    int consumeFood() {
        stomache = FactorizationUtil.normalize(stomache);
        if (stomache == null) {
            return 0;
        }
        ticksUntilNextDigestion = 20*60;
        int ret = getFoodValue(stomache);
        stomache = FactorizationUtil.normalDecr(stomache);
        onInventoryChanged();
        Sound.caliometricDigest.playAt(this);
        return ret;
    }
    
    int getFoodValue(ItemStack is) {
        if (is == null) {
            return 0;
        }
        Item it = is.getItem();
        int heal = 0;
        double sat = 0;
        if (it instanceof ItemFood) {
            ItemFood nom = (ItemFood) it;
            heal = nom.getHealAmount();
            sat = nom.getSaturationModifier();
        } else if (it == Item.cake) {
            heal = 2*6;
            sat = 0.1F;
        }
        heal += Math.min(0, heal*2*sat);
        return heal*(heal/2);
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        if (worldObj.isRemote) {
            return true;
        }
        ItemStack is = entityplayer.getHeldItem();
        if (is == null) {
            info(entityplayer);
            return false;
        }
        is = FactorizationUtil.openInventory(this, ForgeDirection.NORTH).push(is);
        entityplayer.setCurrentItemOrArmor(0, is);
        info(entityplayer);
        onInventoryChanged();
        return true;
    }
    
    void info(EntityPlayer entityplayer) {
        if (stomache == null || stomache.stackSize == 0) {
            Notify.send(entityplayer, this, "Empty");
            return;
        }
        Notify.withItem(stomache);
        Notify.send(entityplayer, this, stomache.stackSize + " {ITEM_NAME}");
    }

}