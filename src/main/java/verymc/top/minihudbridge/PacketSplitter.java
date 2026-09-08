package verymc.top.minihudbridge;

import java.util.Arrays;

public final class PacketSplitter {

    private PacketSplitter() {
    }

    /*
     * Servux 26.1.2:
     *
     * MAX_TOTAL_PER_PACKET_S2C = 1 MiB
     * MAX_PAYLOAD_PER_PACKET_S2C = 1 MiB - 5
     *
     * Los primeros bytes del primer fragmento contienen:
     *
     *     VarInt(totalLength)
     *     datos...
     *
     * El VarInt NO se descuenta del payload.
     */
    public static final int MAX_PAYLOAD = 1_048_571;

    public interface Sender {
        void send(byte[] data);
    }

    public static void send(byte[] data, Sender sender) {

        int totalLength = data.length;

        if (totalLength == 0) {
            return;
        }

        int offset = 0;
        boolean first = true;

        while (offset < totalLength) {

            /*
             * Servux calcula el tamaño del fragmento
             * únicamente con respecto al payload.
             */
            int dataLength =
                    Math.min(
                            totalLength - offset,
                            MAX_PAYLOAD
                    );

            byte[] packet;

            if (first) {

                /*
                 * Primer fragmento:
                 *
                 * VarInt(totalLength)
                 * datos del payload
                 */
                byte[] lengthBytes =
                        writeVarInt(totalLength);

                packet =
                        new byte[
                                lengthBytes.length
                                        + dataLength
                        ];

                System.arraycopy(
                        lengthBytes,
                        0,
                        packet,
                        0,
                        lengthBytes.length
                );

                System.arraycopy(
                        data,
                        offset,
                        packet,
                        lengthBytes.length,
                        dataLength
                );

                first = false;

            } else {

                /*
                 * Fragmentos siguientes:
                 *
                 * solamente datos.
                 */
                packet =
                        Arrays.copyOfRange(
                                data,
                                offset,
                                offset + dataLength
                        );
            }

            offset += dataLength;

            sender.send(packet);
        }
    }

    private static byte[] writeVarInt(int value) {

        byte[] buffer = new byte[5];

        int index = 0;

        while ((value & 0xFFFFFF80) != 0) {

            buffer[index++] =
                    (byte) ((value & 0x7F) | 0x80);

            value >>>= 7;
        }

        buffer[index++] = (byte) value;

        return Arrays.copyOf(
                buffer,
                index
        );
    }
}