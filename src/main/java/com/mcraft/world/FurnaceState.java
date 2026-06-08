package com.mcraft.world;

import java.util.Map;

public class FurnaceState {

    public int inputId = 0,  inputQty = 0;
    public int fuelId  = 0,  fuelQty  = 0;
    public int outputId = 0, outputQty = 0;

    public float smeltProgress  = 0f;
    public float fuelRemaining  = 0f;

    private static final Map<Integer, Integer> RECIPES = Map.of(
        Block.IRON_ORE.id, Block.IRON_INGOT.id,
        Block.GOLD_ORE.id, Block.GOLD_INGOT.id
    );

    private static final Map<Integer, Float> FUEL = Map.of(
        Block.COAL.id,      80f,
        Block.PLANKS.id,    15f,
        Block.WOOD_LOG.id,  15f
    );

    private static final float SMELT_TIME = 10f;

    public void tick(float dt) {
        if (inputId == 0) {
            smeltProgress = 0f;
            return;
        }

        Integer outputForInput = RECIPES.get(inputId);
        if (outputForInput == null) {
            smeltProgress = 0f;
            return;
        }

        boolean outputFull = (outputId != 0)
                           && !(outputId == outputForInput && outputQty < 64);
        if (outputFull) {
            smeltProgress = 1f; 
            return;
        }

        if (fuelRemaining <= 0f) {
            Float fuelValue = FUEL.get(fuelId);
            if (fuelValue == null || fuelValue <= 0 || fuelQty <= 0) {
                return;
            }
            fuelRemaining = fuelValue;
            fuelQty--;
            if (fuelQty <= 0) fuelId = 0;
        }

        fuelRemaining  -= dt;
        smeltProgress  += dt / SMELT_TIME;

        if (smeltProgress >= 1.0f) {
            outputId  = outputForInput;
            outputQty = Math.min(64, outputQty + 1);
            inputQty--;
            if (inputQty <= 0) { inputId = 0; }
            smeltProgress = 0f;
        }
    }

    public boolean isLit() { return fuelRemaining > 0f; }

    public boolean canSmelt(int blockId) { return RECIPES.containsKey(blockId); }

    public static float getFuelValue(int blockId) {
        return FUEL.getOrDefault(blockId, 0f);
    }
}