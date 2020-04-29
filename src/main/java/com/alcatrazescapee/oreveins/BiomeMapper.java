package com.alcatrazescapee.oreveins;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

public final class BiomeMapper {

    private BiomeMapper() {
        throw new IllegalStateException("Utility class");
    }

    private static boolean biomesMapBuilt = false;
    private static boolean biomesTagBuilt = false;
    private static final Map<String, List<String>> biomesMap = new HashMap<>(); // get all biome names with list of biome tags
    private static Map<String, Set<String>> tagMap = new HashMap<>(); // build biome tags map for matchesBiome()

    private static Map<String, List<String>> buildBiomeMap(){
        if(BiomeMapper.biomesMapBuilt) {
            return BiomeMapper.biomesMap;
        }
        Collection<Biome> biomelist = ForgeRegistries.BIOMES.getValuesCollection();

        Iterator<Biome> itr= biomelist.iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (itr.hasNext()) {
            Biome biome = itr.next();
            @SuppressWarnings("ConstantConditions")
            String key = biome.getRegistryName().toString();
            List<String> tags =  BiomeDictionary
                    .getTypes(biome)
                    .stream()
                    .map(BiomeDictionary.Type::getName)
                    .collect(Collectors.toList());
            BiomeMapper.biomesMap.put(key, tags);
        }
        BiomeMapper.biomesMapBuilt = true;
        return BiomeMapper.biomesMap;
    }
    /**
     * Get a HashMap of all available biome names (e.g "minecraft:swampland") and their biome tags (e.g. "WET")
     * including all modded biomes:
     * {"minecraft:swampland: ["WET"], "minecraft:desert": ["HOT", "DRY"]..}
     *
     * Revert that map to get all biomes with biome tag "WET" for instance:
     * {"WET": ["minecraft:swampland", "minecraft:jungle_edge"..]}
     *
     * Return reverted map for biome tag handling in matchesBiome
     * @return biome tag HashMap
     */
    private static Map<String, Set<String>> buildBiomeTags(){
        if(BiomeMapper.biomesTagBuilt) {
            return BiomeMapper.tagMap;
        }
        BiomeMapper.tagMap = buildBiomeMap().entrySet()
                .stream()
                .flatMap(e -> e.getValue().stream()
                        .map(v -> new AbstractMap.SimpleEntry<>(v, e.getKey())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
        Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
        BiomeMapper.biomesTagBuilt = true;
        return BiomeMapper.tagMap;
    }

    public static Map<String, Set<String>> getBiomeTags(){
        return BiomeMapper.buildBiomeTags();
    }

    public static Map<String, List<String>> getBiomeMap(){
        return BiomeMapper.buildBiomeMap();
    }

    public static boolean conditionIsWhitelist(String s, String biomeName, Map<String, List<String>> biomeMap) {
        @SuppressWarnings("squid:S1871")
        List<String> biomepattern = Arrays.asList(s.split("\\s*&\\s*"));

        List<String> allowedtags = new ArrayList<>();
        List<String> unwantedtags = new ArrayList<>();

//                biomepattern.stream().forEach(name -> {
//                    if (name.startsWith("-")) unwantedtags.add(name.substring(1));
//                    else allowedtags.add(name);
//                    });

        biomepattern.forEach(name -> {
            if (name.startsWith("-")) unwantedtags.add(name.substring(1));
            else allowedtags.add(name);
        });

        List<String> biometaglist = biomeMap.get(biomeName);


        if (biometaglist != null) {
            if (biometaglist.stream().anyMatch(unwantedtags::contains)) {
                return false;
            }
            return biometaglist.containsAll(allowedtags);
        }
        return false;
    }

    public static boolean isCapitalized(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLowerCase(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}