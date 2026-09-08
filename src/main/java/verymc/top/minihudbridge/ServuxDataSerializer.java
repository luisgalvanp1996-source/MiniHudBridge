package verymc.top.minihudbridge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public final class ServuxDataSerializer {

    private ServuxDataSerializer() {
    }

    public static byte[] serialize(
            CompoundTag root
    ) throws IOException {

        ByteArrayOutputStream compressed =
                new ByteArrayOutputStream();

        try (
                GZIPOutputStream gzip =
                        new GZIPOutputStream(compressed);

                DataOutputStream output =
                        new DataOutputStream(gzip)
        ) {

            // Root tag type = Compound
            output.writeByte(10);

            // Servux usa DataOutput.writeUTF()
            output.writeUTF("");

            writeCompound(output, root);
        }

        byte[] compressedData =
                compressed.toByteArray();

        ByteArrayOutputStream result =
                new ByteArrayOutputStream(
                        Integer.BYTES
                                + compressedData.length
                );

        DataOutputStream output =
                new DataOutputStream(result);

        output.writeInt(
                compressedData.length
        );

        output.write(compressedData);

        output.flush();

        return result.toByteArray();
    }

private static void writeCompound(
        DataOutputStream output,
        CompoundTag compound
) throws IOException {

    for (String key : compound.keySet()) {

        Tag tag = compound.get(key);

        if (tag == null) {
            continue;
        }

        writeEntry(output, key, tag);
    }

    output.writeByte(0);
}
    private static void writeEntry(
            DataOutputStream output,
            String key,
            Tag tag
    ) throws IOException {

        output.writeByte(
                tag.getId()
        );

        if (tag.getId() == 0) {
            return;
        }

        output.writeUTF(key);

        writeTagPayload(
                output,
                tag
        );
    }

    private static void writeTagPayload(
            DataOutputStream output,
            Tag tag
    ) throws IOException {

        switch (tag.getId()) {

            case 1 -> {
                ByteTag value =
                        (ByteTag) tag;

                output.writeByte(
                        value.byteValue()
                );
            }

            case 2 -> {
                ShortTag value =
                        (ShortTag) tag;

                output.writeShort(
                        value.shortValue()
                );
            }

            case 3 -> {
                IntTag value =
                        (IntTag) tag;

                output.writeInt(
                        value.intValue()
                );
            }

            case 4 -> {
                LongTag value =
                        (LongTag) tag;

                output.writeLong(
                        value.longValue()
                );
            }

            case 5 -> {
                FloatTag value =
                        (FloatTag) tag;

                output.writeFloat(
                        value.floatValue()
                );
            }

            case 6 -> {
                DoubleTag value =
                        (DoubleTag) tag;

                output.writeDouble(
                        value.doubleValue()
                );
            }

            case 7 -> {
                ByteArrayTag value =
                        (ByteArrayTag) tag;

                byte[] data =
                        value.getAsByteArray();

                output.writeInt(
                        data.length
                );

                output.write(data);
            }

            case 8 -> {
                StringTag value =
                        (StringTag) tag;

                output.writeUTF(
                        value.value()
                );
            }

            case 9 -> {
                writeList(
                        output,
                        (ListTag) tag
                );
            }

            case 10 -> {
                writeCompound(
                        output,
                        (CompoundTag) tag
                );
            }

            case 11 -> {
                IntArrayTag value =
                        (IntArrayTag) tag;

                int[] data =
                        value.getAsIntArray();

                output.writeInt(
                        data.length
                );

                for (int number : data) {
                    output.writeInt(number);
                }
            }

            case 12 -> {
                LongArrayTag value =
                        (LongArrayTag) tag;

                long[] data =
                        value.getAsLongArray();

                output.writeInt(
                        data.length
                );

                for (long number : data) {
                    output.writeLong(number);
                }
            }

            default ->
                    throw new IOException(
                            "Tipo NBT desconocido: "
                                    + tag.getId()
                    );
        }
    }

    private static void writeList(
            DataOutputStream output,
            ListTag list
    ) throws IOException {

        int elementType = 0;

        if (!list.isEmpty()) {
            elementType =
                    list.get(0).getId();
        }

        output.writeByte(
                elementType
        );

        output.writeInt(
                list.size()
        );

        for (int i = 0; i < list.size(); i++) {

            Tag tag = list.get(i);

            writeListPayload(
                    output,
                    tag
            );
        }
    }

    private static void writeListPayload(
            DataOutputStream output,
            Tag tag
    ) throws IOException {

        switch (tag.getId()) {

            case 1 ->
                    output.writeByte(
                            ((ByteTag) tag).byteValue()
                    );

            case 2 ->
                    output.writeShort(
                            ((ShortTag) tag).shortValue()
                    );

            case 3 ->
                    output.writeInt(
                            ((IntTag) tag).intValue()
                    );

            case 4 ->
                    output.writeLong(
                            ((LongTag) tag).longValue()
                    );

            case 5 ->
                    output.writeFloat(
                            ((FloatTag) tag).floatValue()
                    );

            case 6 ->
                    output.writeDouble(
                            ((DoubleTag) tag).doubleValue()
                    );

            case 7 -> {
                byte[] data =
                        ((ByteArrayTag) tag)
                                .getAsByteArray();

                output.writeInt(
                        data.length
                );

                output.write(data);
            }

            case 8 ->
                    output.writeUTF(
                            ((StringTag) tag).value()
                    );

            case 9 ->
                    writeList(
                            output,
                            (ListTag) tag
                    );

            case 10 ->
                    writeCompound(
                            output,
                            (CompoundTag) tag
                    );

            case 11 -> {
                int[] data =
                        ((IntArrayTag) tag)
                                .getAsIntArray();

                output.writeInt(
                        data.length
                );

                for (int number : data) {
                    output.writeInt(number);
                }
            }

            case 12 -> {
                long[] data =
                        ((LongArrayTag) tag)
                                .getAsLongArray();

                output.writeInt(
                        data.length
                );

                for (long number : data) {
                    output.writeLong(number);
                }
            }

            default ->
                    throw new IOException(
                            "Tipo NBT de lista desconocido: "
                                    + tag.getId()
                    );
        }
    }
}