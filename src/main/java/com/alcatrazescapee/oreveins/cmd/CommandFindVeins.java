/*
 * Part of the Ore Veins Mod by alcatrazEscapee
 * Work under Copyright. Licensed under the GPL-3.0.
 * See the project LICENSE.md for more information.
 */

package com.alcatrazescapee.oreveins.cmd;

import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import com.alcatrazescapee.oreveins.api.IVein;
import com.alcatrazescapee.oreveins.api.IVeinType;
import com.alcatrazescapee.oreveins.vein.VeinRegistry;
import com.alcatrazescapee.oreveins.world.FeatureVeins;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

@ParametersAreNonnullByDefault
public final class CommandFindVeins
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(
                Commands.literal("findveins").requires(source -> source.hasPermissionLevel(2))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 250))
                                        .executes(cmd -> findVeins(cmd.getSource(), StringArgumentType.getString(cmd, "type"), IntegerArgumentType.getInteger(cmd, "radius")))
                                )));
    }

    private static int findVeins(CommandSource source, String veinName, int radius)
    {
        source.sendFeedback(new TextComponentString("Veins Found: "), true);

        final BlockPos pos = new BlockPos(source.getPos());
        final int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        final List<IVein> veins = FeatureVeins.getNearbyVeins(chunkX, chunkZ, source.getWorld().getSeed(), radius);
        final IVeinType type = VeinRegistry.getVein(veinName);
        if (type == null)
        {
            source.sendErrorMessage(new TextComponentString("Vein supplied does not match any valid vein names"));
        }

        // Search for veins matching type
        veins.removeIf(x -> x.getType() != type);
        veins.forEach(x -> source.sendFeedback(new TextComponentString("> Vein: " + x.toString()), true));
        return 1;
    }
}
