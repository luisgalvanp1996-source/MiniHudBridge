package verymc.top.minihudbridge;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class ServuxStructuresPacket {

    public static final int PROTOCOL_VERSION = 3;

    public enum Type {
        PACKET_S2C_METADATA(1),
        PACKET_S2C_STRUCTURE_DATA(2),
        PACKET_C2S_STRUCTURES_REGISTER(3),
        PACKET_C2S_STRUCTURES_UNREGISTER(4),
        PACKET_S2C_STRUCTURE_DATA_START(5),
        PACKET_S2C_SPAWN_METADATA(10),
        PACKET_C2S_REQUEST_SPAWN_METADATA(11),
        PACKET_S2C_WEATHER_DATA(12);

        private final int id;

        Type(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Type fromId(int id) {
            for (Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }

            throw new IllegalArgumentException(
                    "Unknown Servux structures packet type: " + id
            );
        }
    }

    private final Type type;
    private final CompoundTag nbt;
    private final byte[] buffer;

    public ServuxStructuresPacket(Type type, CompoundTag nbt) {
        this.type = type;
        this.nbt = nbt;
        this.buffer = null;
    }

    public ServuxStructuresPacket(Type type, byte[] buffer) {
        this.type = type;
        this.nbt = null;
        this.buffer = buffer;
    }

    public Type getType() {
        return type;
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public boolean hasNbt() {
        return nbt != null;
    }

    public boolean hasBuffer() {
        return buffer != null;
    }

    public byte[] encode() {
        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());

        try {
            out.writeVarInt(type.getId());

            if (type == Type.PACKET_S2C_STRUCTURE_DATA) {
                if (buffer != null) {
                    out.writeBytes(buffer);
                }
            } else {
                out.writeNbt(nbt != null ? nbt : new CompoundTag());
            }

            byte[] result = new byte[out.readableBytes()];
            out.readBytes(result);

            return result;
        } finally {
            out.release();
        }
    }

    public static ServuxStructuresPacket decode(byte[] data) {
        FriendlyByteBuf input =
                new FriendlyByteBuf(Unpooled.wrappedBuffer(data));

        try {
            int id = input.readVarInt();
            Type type = Type.fromId(id);

            if (type == Type.PACKET_S2C_STRUCTURE_DATA) {
                byte[] remaining = new byte[input.readableBytes()];
                input.readBytes(remaining);

                return new ServuxStructuresPacket(type, remaining);
            }

            CompoundTag nbt = input.readNbt();

            if (nbt == null) {
                nbt = new CompoundTag();
            }

            return new ServuxStructuresPacket(type, nbt);

        } finally {
            input.release();
        }
    }
}