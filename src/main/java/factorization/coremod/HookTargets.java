package factorization.coremod;

import factorization.api.Coord;
import factorization.common.FactorizationKeyHandler;
import factorization.common.FzConfig;
import factorization.shared.Core;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class HookTargets {
    public static void keyTyped(char chr, int keysym) {
        //Core.logInfo("KeyTyped: %s %s", chr, keysym); //NORELEASE
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        if (FzConfig.pocket_craft_anywhere) {
            if (FactorizationKeyHandler.pocket_key.getKeyCode() == keysym) {
                Core.registry.pocket_table.tryOpen(mc.thePlayer);
            }
        }
    }
    
    public static void diamondExploded(Object dis, World world, int x, int y, int z) {
        if (dis != Blocks.diamond_block) return;
        if (world.isRemote) {
            return;
        }
        Coord c = new Coord(world, x, y, z);
        //if (c.isAir()) return;
        c.setAir();
        int i = 18;
        while (i > 0) {
            int spawn = world.rand.nextInt(3) + 2;
            spawn = Math.min(spawn, i);
            i -= spawn;
            EntityItem ent = c.spawnItem(new ItemStack(Core.registry.diamond_shard, spawn));
            ent.invulnerable = true;
            ent.motionX = randShardVelocity(world);
            ent.motionY = randShardVelocity(world);
            ent.motionZ = randShardVelocity(world);
        }
    }
    
    static double randShardVelocity(World world) {
        double r = world.rand.nextGaussian()/4;
        double max = 0.3;
        if (r > max) {
            r = max;
        } else if (r < -max) {
            r = -max;
        }
        return r;
    }
}
