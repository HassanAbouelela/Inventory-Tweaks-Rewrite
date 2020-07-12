/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.functions;

import net.minecraft.entity.MobEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBT;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ArmorEquip {
    static int[] getBestArmor(ArrayList<Entry<ItemStack, Integer>> inventory) {
        ItemStack helmet = null;
        ItemStack chestPlate = null;
        ItemStack leggings = null;
        ItemStack boots = null;

        int[] result = {-1, -1, -1, -1};

        for (Entry<ItemStack, Integer> entry: inventory) {
            ItemStack itemStack = entry.getKey();

            // Basic check to block near-breaking armor
            if (itemStack.getMaxDamage() - itemStack.getDamage() < 4) continue;

            switch (MobEntity.getSlotForItemStack(itemStack)) {
                case HEAD:
                    if (helmet == null || compareArmor(helmet, itemStack) == itemStack) {
                        helmet = itemStack;
                        result[0] = entry.getValue();
                    }

                    break;

                case CHEST:
                    if (chestPlate == null || compareArmor(chestPlate, itemStack) == itemStack) {
                        chestPlate = itemStack;
                        result[1] = entry.getValue();
                    }

                    break;

                case LEGS:
                    if (leggings == null || compareArmor(leggings, itemStack) == itemStack) {
                        leggings = itemStack;
                        result[2] = entry.getValue();
                    }

                    break;

                case FEET:
                    if (boots == null || compareArmor(boots, itemStack) == itemStack) {
                        boots = itemStack;
                        result[3] = entry.getValue();
                    }
            }
        }

        return result;
    }

    private static ItemStack compareArmor(ItemStack itemStack1, ItemStack itemStack2) {
        /*
         * 10 Point-Scale
         * 3 Points: Durability (With Un-breaking Enchant)
         * 5 Points: Raw Protection (With Protection Enchant)
         * 2 Point: Most extra enchants (Depth Strider, Respiration, etc)
         *
         */

        int score1 = 0;
        int score2 = 0;

        if (itemStack1.isDamageable() && itemStack2.isDamageable()) {
            int durability1 = calculateDurability(itemStack1);
            int durability2 = calculateDurability(itemStack2);

            if (durability1 > durability2) {
                score1 += 3;
            } else if (durability2 > durability1) {
                score2 += 3;
            }

        } else if (itemStack1.isDamageable()) {
            score2 += 3;
        } else {
            score1 += 3;
        }

        float defense1 = calculateDefense(itemStack1);
        float defense2 = calculateDefense(itemStack2);

        if (defense1 > defense2) {
            score1 += 5;
        } else if (defense2 > defense1) {
            score2 += 5;
        }

        int extraEnchants1 = calculateExtraEnchants(itemStack1);
        int extraEnchants2 = calculateExtraEnchants(itemStack2);

        if (extraEnchants1 > extraEnchants2) {
            score1 += 2;
        } else if (extraEnchants2 > extraEnchants1) {
            score2 += 2;
        }

        return score2 > score1 ? itemStack2 : itemStack1;
    }

    private static int calculateDurability(ItemStack itemStack) {
        // Formula for un-breaking effectiveness from: https://minecraft.gamepedia.com/Unbreaking
        int unbreakingLevel = Functions.getTagLevel("minecraft:unbreaking", itemStack);
        return (2 - (60 + (40 / (unbreakingLevel + 1))) / 100) * (itemStack.getMaxDamage() - itemStack.getDamage());
    }

    private static float calculateDefense(ItemStack itemStack) {
        // Protection bonus calculation from: https://minecraft.gamepedia.com/Protection
        float enchantBonus = 0;

        if (Functions.getTagLevel("minecraft:protection", itemStack) != 0) {
            enchantBonus = (float) 4 * Functions.getTagLevel("minecraft:protection", itemStack) / 100;

        } else if (Functions.getTagLevel("minecraft:blast_protection", itemStack) != 0) {
            enchantBonus = (float) 8 * Functions.getTagLevel("minecraft:blast_protection", itemStack) / 100;

        } else if (Functions.getTagLevel("minecraft:fire_protection", itemStack) != 0) {
            enchantBonus = (float) 15 * Functions.getTagLevel("minecraft:fire_protection", itemStack) / 100;

        } else if (Functions.getTagLevel("minecraft:projectile_protection", itemStack) != 0) {
            enchantBonus = (float) 8 * Functions.getTagLevel("minecraft:projectile_protection", itemStack) / 100;
        }

        ArmorItem armorItem = (ArmorItem) itemStack.getItem();
        return (armorItem.getDamageReduceAmount() + armorItem.getToughness()) * (1 + enchantBonus);
    }

    private static int calculateExtraEnchants(ItemStack itemStack) {
        int score = 0;
        for (INBT nbt: itemStack.getEnchantmentTagList()) {
            if (nbt.getString().toLowerCase().contains("curse")) {
                score--;
            } else {
                Matcher matcher = Pattern.compile("(?<=lvl:)([0-9]+)").matcher(nbt.getString());

                if (matcher.find()) {
                    score += Integer.parseInt(matcher.group());
                }
            }
        }

        return score;
    }
}
