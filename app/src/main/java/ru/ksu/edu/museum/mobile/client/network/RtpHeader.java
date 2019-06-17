package ru.ksu.edu.museum.mobile.client.network;

public class RtpHeader {
    private final byte version = 2;
    private final byte payloadType = 26;

    private short sequenceNumber;
    private long timestamp;

    private RtpHeader() {}

    public static RtpHeader create(short id, int timestamp) {
        RtpHeader header = new RtpHeader();
        header.sequenceNumber = id;
        header.timestamp = timestamp;

        return header;
    }

    public static byte[] convertToDatagram(RtpHeader header) {
        byte[] datagram = new byte[8];
        datagram[0] = header.version;
        datagram[1] = header.payloadType;
        datagram[2] = (byte) (header.sequenceNumber >> 8);
        datagram[3] = (byte) (header.sequenceNumber & 0xFF);
        datagram[4] = (byte) (header.timestamp >> 24);
        datagram[5] = (byte) ((header.timestamp >> 16) & 0xFF);
        datagram[6] = (byte) ((header.timestamp >> 8) & 0xFF);
        datagram[7] = (byte) (header.timestamp & 0xFF);

        return datagram;
    }
}
