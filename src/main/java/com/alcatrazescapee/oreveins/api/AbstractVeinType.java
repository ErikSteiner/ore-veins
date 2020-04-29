package com.alcatrazescapee.oreveins.api;

import com.alcatrazescapee.oreveins.BiomeMapper;
import com.alcatrazescapee.oreveins.OreVeinsConfig;
import com.alcatrazescapee.oreveins.util.IWeightedList;
import com.alcatrazescapee.oreveins.vein.Indicator;
import com.alcatrazescapee.oreveins.vein.VeinRegistry;
import com.google.gson.annotations.SerializedName;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@SuppressWarnings({"unused", "WeakerAccess"})
@ParametersAreNonnullByDefault
public abstract class AbstractVeinType<V extends AbstractVein<?>> implements IVeinType<V> {
    protected int count = 1;
    protected int rarity = 10;
    @SerializedName("min_y")
    protected int minY = 16;
    @SerializedName("max_y")
    protected int maxY = 64;
    @SerializedName("use_relative_y")
    protected boolean useRelativeY = false;
    @SerializedName("vertical_size")
    protected int verticalSize = 8;
    @SerializedName("horizontal_size")
    protected int horizontalSize = 15;
    protected float density = 20;

    @SerializedName("dimensions_is_whitelist")
    protected boolean dimensionIsWhitelist = true;
    @SerializedName("biomes_is_whitelist")
    protected boolean biomesIsWhitelist = true;

    @SerializedName("stone")
    private List<IBlockState> stoneStates = null;
    @SerializedName("ore")
    private IWeightedList<IBlockState> oreStates = null;

    private List<String> biomes = null;
    private List<Integer> dimensions = null;
    private List<ICondition> conditions = null;
    private IWeightedList<Indicator> indicator = null;

    @Nonnull
    @Override
    public IBlockState getStateToGenerate(Random rand) {
        return oreStates.get(rand);
    }

    @Nonnull
    @Override
    public Collection<IBlockState> getOreStates() {
        return oreStates.values();
    }

    @Nullable
    @Override
    public Indicator getIndicator(Random random) {
        return indicator != null ? indicator.get(random) : null;
    }

    @Override
    public boolean canGenerateAt(World world, BlockPos pos) {
        IBlockState stoneState = world.getBlockState(pos);
        if (stoneStates.contains(stoneState)) {
            if (conditions != null) {
                for (ICondition condition : conditions) {
                    if (!condition.test(world, pos)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean inRange(V vein, int xOffset, int zOffset) {
        return xOffset * xOffset + zOffset * zOffset < horizontalSize * horizontalSize * vein.getSize();
    }

    @Override
    public boolean matchesDimension(int id) {
        if (dimensions == null) {
            return id == 0;
        }
        for (int i : dimensions) {
            if (id == i) {
                return dimensionIsWhitelist;
            }
        }
        return !dimensionIsWhitelist;
    }

    @Override
    public boolean matchesBiome(Biome biome) {
        if (biomes == null) return true;
        @SuppressWarnings({"ConstantConditions", "squid:S1871"})
        String biomeName = biome.getRegistryName().toString();
        boolean posTagIsWhitelist = false;

        List<String> jsonBiomes = new ArrayList<>();
        List<String> jsonPosTags = new ArrayList<>();
        List<String> jsonNegTags = new ArrayList<>();
        List<String> jsonConditionTags = new ArrayList<>();

        Map<String, Set<String>> biomeTags = BiomeMapper.getBiomeTags();
        Map<String, List<String>> biomeMap = BiomeMapper.getBiomeMap();

        // build the lists
        for (String s : biomes) {
            if (BiomeMapper.isCapitalized(s) && !s.contains("&")) {
                if (s.startsWith("-")) {
                    jsonNegTags.add(s.substring(1));  // add s to jsonNegTags
                } else {
                    jsonPosTags.add(s);  // add s to jsonPosTags
                }
            } else if (BiomeMapper.isCapitalized(s) && s.contains("&")) {
                jsonConditionTags.add(s);  // add s to jsonConditionTags
            } else if (!BiomeMapper.isCapitalized(s)) {
                jsonBiomes.add(s);  // add s to jsonbiomes
            }
        }

        // check biome name list first
        if (!jsonBiomes.isEmpty() && !jsonBiomes.stream().anyMatch(biomeName::contains)) {
            return false;
        }

        // check positive biome tags
        if (!jsonPosTags.isEmpty()) {
            for (String s : jsonPosTags) {
                if (biomeTags.get(s).stream().anyMatch(str -> str.trim().equals(biomeName))) {
                    posTagIsWhitelist = true;
                    break;
                }
            }
            if (!posTagIsWhitelist) {
                return false;
            }
        }

        // check negative biome tags
        if (!jsonNegTags.isEmpty()) {
            for (String s : jsonNegTags) {
                // s contains the following biomes biomeTags.get(s))
                if (biomeTags.get(s).stream().anyMatch(str -> str.trim().equals(biomeName))) {
                    return false;
                }
            }
        }

        // check condition biome tags
        if (!jsonConditionTags.isEmpty()) {
            for (String s : jsonConditionTags) {
                // if jsonConditionTags s condition for biomeName is false, make false
                if (!BiomeMapper.conditionIsWhitelist(s, biomeName, biomeMap)) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public boolean isValid()
    {
        return oreStates != null && !oreStates.isEmpty() &&
                stoneStates != null && !stoneStates.isEmpty() &&
                (indicator == null || (!indicator.isEmpty() && indicator.values().stream().map(Indicator::isValid).reduce((x, y) -> x && y).orElse(false))) &&
                maxY > minY && (minY >= 0 || useRelativeY) &&
                count > 0 &&
                rarity > 0 &&
                verticalSize > 0 && horizontalSize > 0 && density > 0;

    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return String.format("[%s: Count: %d, Rarity: %d, Y: %d - %d, Size: %d / %d, Density: %2.2f, Ores: %s, Stones: %s]", VeinRegistry.getName(this), count, rarity, minY, maxY, horizontalSize, verticalSize, density, oreStates, stoneStates);
    }

    protected final BlockPos defaultStartPos(int chunkX, int chunkZ, Random rand) {
        int spawnRange = maxY - minY;
        int minRange = minY;
        if (OreVeinsConfig.AVOID_VEIN_CUTOFFS) {
            if (verticalSize * 2 < spawnRange) {
                spawnRange -= verticalSize * 2;
                minRange += verticalSize;
            } else {
                minRange = minY + (maxY - minY) / 2;
                spawnRange = 1;
            }
        }
        return new BlockPos(
                chunkX * 16 + rand.nextInt(16),
                minRange + rand.nextInt(spawnRange),
                chunkZ * 16 + rand.nextInt(16)
        );
    }

    @Override
    public int getRarity() {
        return rarity;
    }

    @Override
    public int getChunkRadius() {
        return 1 + (horizontalSize >> 4);
    }

    @Override
    public boolean useRelativeY() {
        return useRelativeY;
    }
}
