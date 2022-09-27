package Shared;//class Shared.RTPpacket

import java.util.Arrays;

public class RTPpacket{

    static int HEADER_SIZE = 12;

    public int Version;
    public int Padding;
    public int Extension;
    public int CC;
    public int Marker;
    public int PayloadType;
    public int SequenceNumber;
    public int TimeStamp;
    public int Ssrc;

    public byte[] header;

    public int payload_size;
    public byte[] payload;

    public RTPpacket(int Framenb, byte[] data, int data_length){
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 1337;
        SequenceNumber = Framenb;

        header = new byte[HEADER_SIZE];

        header[0] = (byte)(Version << 6 | Padding << 5 | Extension << 4 | CC);
        header[1] = (byte)(Marker << 7 | PayloadType & 0x000000FF);
        header[2] = (byte)(SequenceNumber >> 8);
        header[3] = (byte)(SequenceNumber & 0xFF); 
        header[4] = (byte)(TimeStamp >> 24);
        header[5] = (byte)(TimeStamp >> 16);
        header[6] = (byte)(TimeStamp >> 8);
        header[7] = (byte)(TimeStamp & 0xFF);
        header[8] = (byte)(Ssrc >> 24);
        header[9] = (byte)(Ssrc >> 16);
        header[10] = (byte)(Ssrc >> 8);
        header[11] = (byte)(Ssrc & 0xFF);

        payload_size = data_length;
        payload = new byte[data_length];

        payload = Arrays.copyOf(data, payload_size);
    }

    public RTPpacket(byte[] packet, int packet_size)
    {
        //fill default fields:
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        if (packet_size >= HEADER_SIZE) 
        {
            header = new byte[HEADER_SIZE];
            for (int i=0; i < HEADER_SIZE; i++)
                header[i] = packet[i];

            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];
            for (int i=HEADER_SIZE; i < packet_size; i++)
                payload[i-HEADER_SIZE] = packet[i];

            Version = (header[0] & 0xFF) >>> 6;
            PayloadType = header[1] & 0x7F;
            SequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
            TimeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);
        }
    }

    public int getpayload(byte[] data) {

        for (int i=0; i < payload_size; i++)
            data[i] = payload[i];

        return(payload_size);
    }

    public int getpayload_length() {
        return(payload_size);
    }

    public int getlength() {
        return(payload_size + HEADER_SIZE);
    }

    public int getpacket(byte[] packet)
    {
        for (int i=0; i < HEADER_SIZE; i++)
            packet[i] = header[i];
        for (int i=0; i < payload_size; i++)
            packet[i+HEADER_SIZE] = payload[i];

        return(payload_size + HEADER_SIZE);
    }


    public int gettimestamp() {
        return(TimeStamp);
    }

    public int getsequencenumber() {
        return(SequenceNumber);
    }

    public int getpayloadtype() {
        return(PayloadType);
    }

}