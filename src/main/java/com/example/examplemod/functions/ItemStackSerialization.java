/*
 * MIT License
 *
 * Copyright (c) 2020 Hassan Abouelela
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.example.examplemod.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.text.ITextComponent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class ItemStackSerialization {
    private Map<String, Object> itemInfo;
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Class to convert an ItemStack into a string that can be saved and converted back to an ItemStack.
     *
     * @param itemStack The ItemStack to serialize.
     * @param backupFolder The folder to NBT write data to.
     * @param fileNumber A random number to ensure no two items collide.
     *
     * @throws IOException Could not write NBT data to nbtFile.
     */
    ItemStackSerialization(ItemStack itemStack, Path backupFolder, int fileNumber) throws IOException {
        itemInfo = new HashMap<>();
        int ID = Item.getIdFromItem(itemStack.getItem());
        itemInfo.put("Item ID", ID);

        if (itemStack.hasDisplayName()) {
            String serialized = ITextComponent.Serializer.toJson(itemStack.getDisplayName());
            itemInfo.put("Display Name", serialized);
        } else {
            itemInfo.put("Display Name", null);
        }

        if (backupFolder != null && itemStack.hasTag() && itemStack.getTag() != null) {
            if (!Files.exists(backupFolder)) {
                Files.createDirectory(backupFolder);
            }
            if (Files.isWritable(backupFolder)) {
                Path nbtFile = Files.createFile(Paths.get((backupFolder + "/" + ID + fileNumber + ".ser")));
                itemInfo.put("Tags", nbtFile.toAbsolutePath().normalize().toFile());

                CompressedStreamTools.write(itemStack.getTag(), nbtFile.toFile());
            }

        } else {
            itemInfo.put("Tags", null);
        }

        if (itemStack.isRepairable()) {
            itemInfo.put("Repair Cost", itemStack.getRepairCost());
        } else {
            itemInfo.put("Repair Cost", null);
        }

        if (itemStack.isStackable()) {
            itemInfo.put("Size", itemStack.getCount());
        } else {
            itemInfo.put("Size", 1);
        }

        if (itemStack.isDamageable()) {
            itemInfo.put("Damage", itemStack.getDamage());
        } else {
            itemInfo.put("Damage", null);
        }
    }

    /**
     * Convert and ItemStack to a string that can be written.
     *
     * @return A string representation of the stack and most crucial properties.
     * @throws JsonProcessingException Error converting this class into string.
     */
    String serialize() throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.itemInfo);
    }

    /**
     * A utility to convert a string form of a serialized ItemStack into an ItemStack object.
     *
     * @param backupItem A string representation of the serialized ItemStack.
     * @return The ItemStack object represented by the string.
     *
     * @throws IOException Could not load tag serialization file.
     */
    static ItemStack deserialize(String backupItem) throws IOException {
        Map readItem = mapper.readValue(backupItem, Map.class);
        Item item = Item.getItemById((int) readItem.get("Item ID"));
        ItemStack itemStack = new ItemStack(item, (int) readItem.get("Size"));

        if (readItem.get("Tags") != null) {
            File nbtFile = Paths.get((String) readItem.get("Tags")).toFile();
            CompoundNBT nbt = CompressedStreamTools.read(nbtFile);
                if (nbt != null) {
                    itemStack.setTag(nbt);
                }
        }

//        if (readItem.get("Display Name") != null) {
//            ITextComponent textComponent = ITextComponent.Serializer.fromJson((String) readItem.get("Display Name"));
//            itemStack.setDisplayName(textComponent);
//        }

        if (readItem.get("Damage") != null) {
            itemStack.setDamage((int) readItem.get("Damage"));
        }

        if (readItem.get("Repair Cost") != null) {
            itemStack.setRepairCost((int) readItem.get("Repair Cost"));
        }

        return itemStack;
    }
}
