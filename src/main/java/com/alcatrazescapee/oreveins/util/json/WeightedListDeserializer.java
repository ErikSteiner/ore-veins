/*
 * Part of the Ore Veins Mod by alcatrazEscapee
 * Work under Copyright. Licensed under the GPL-3.0.
 * See the project LICENSE.md for more information.
 */

package com.alcatrazescapee.oreveins.util.json;

import java.lang.reflect.Type;

import com.google.gson.*;
import net.minecraft.block.state.IBlockState;

import com.alcatrazescapee.oreveins.util.IWeightedList;
import com.alcatrazescapee.oreveins.util.WeightedList;

public class WeightedListDeserializer implements JsonDeserializer<IWeightedList<IBlockState>>
{
    @Override
    public IWeightedList<IBlockState> deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException
    {
        if (json.isJsonPrimitive() || json.isJsonObject())
        {
            IBlockState state = context.deserialize(json, IBlockState.class);
            return IWeightedList.singleton(state);
        }
        else if (json.isJsonArray())
        {
            JsonArray array = json.getAsJsonArray();
            IWeightedList<IBlockState> states = new WeightedList<>();
            for (JsonElement element : array)
            {
                JsonObject obj = element.getAsJsonObject();
                double weight = obj.has("weight") ? obj.get("weight").getAsDouble() : 1;
                states.add(weight, context.deserialize(element, IBlockState.class));
            }
            return states;
        }
        throw new JsonParseException("Unable to parse IBlockState List");
    }
}