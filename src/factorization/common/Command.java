package factorization.common;

import java.util.ArrayList;
import java.util.HashMap;

import factorization.shared.Core;
import factorization.weird.ContainerPocket;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public enum Command {
    bagShuffle(1), craftClear(2, true), craftSwirl(3, true), craftBalance(4, true), craftOpen(5, true),
    bagShuffleReverse(6), craftFill(11, true);

    static class name {
        static HashMap<Byte, Command> map = new HashMap<Byte, Command>();
    }
    
    static {
        bagShuffle.setReverse(bagShuffleReverse);
    }

    public byte id;
    boolean executeLocally = false;
    public Command reverse = this;

    Command(int id) {
        this.id = (byte) id;
        name.map.put(this.id, this);
    }

    Command(int id, boolean executeLocally) {
        this(id);
        this.executeLocally = executeLocally;
    }
    
    void setReverse(Command rev) {
        rev.reverse = this;
        this.reverse = rev;
    }

    public static void fromNetwork(EntityPlayer player, byte s, byte arg) {
        Command c = name.map.get(s);
        if (c == null) {
            Core.logWarning("Received invalid command #" + s);
            return;
        }
        c.call(player, arg);
    }

    public void call(EntityPlayer player) {
        call(player, (byte) 0);
    }

    public void call(EntityPlayer player, byte arg) {
        if (player == null) {
            return;
        }
        if (player.worldObj.isRemote) {
            Core.network.sendCommand(player, this, arg);
            if (!executeLocally) {
                return;
            }
        }
        switch (this) {
        case bagShuffle:
            Core.registry.bag_of_holding.useBag(player, false);
            break;
        case bagShuffleReverse:
            Core.registry.bag_of_holding.useBag(player, true);
            break;
        case craftClear:
        case craftSwirl:
        case craftBalance:
        case craftFill:
            if (player.openContainer instanceof ContainerPocket) {
                ((ContainerPocket) player.openContainer).executeCommand(this, arg);
            }
            break;
        case craftOpen:
            Core.registry.pocket_table.tryOpen(player);
            break;
        default:
            Core.logWarning("Command " + this + " is missing handler");
        }
    }
    
}
