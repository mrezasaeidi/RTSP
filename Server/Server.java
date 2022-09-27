package Server;

import Server.stream.FrameStream;
import Server.stream.MjpegStream;
import Server.stream.WebCamStream;
import Shared.RTPpacket;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.StringTokenizer;
import java.util.UUID;

public class Server  {
    private DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    private DatagramPacket senddp; //UDP packet containing the video frames

    private InetAddress ClientIPAddr;
    private int RTP_dest_port = 0;

    int imagenb = 0;
    FrameStream frameStream;

    Timer timer;
    byte[] buf;
    final int sendDelay = 100;

    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;

    private static int state;
    private Socket RTSPsocket;

    private static BufferedReader RTSPBufferedReader;
    private static BufferedWriter RTSPBufferedWriter;
    private static String sourceName;
    private static String RTSPid = UUID.randomUUID().toString();
    private int RTSPSeqNb = 0;
    
    final static String newLine = "\r\n";

    public Server() {
        timer = new Timer(sendDelay, e -> {
                //update current imagenb
                imagenb++;
                try {
                    int image_length = frameStream.getnextframe(buf);

                    RTPpacket rtp_packet = new RTPpacket( imagenb, buf, image_length);

                    int packet_length = rtp_packet.getlength();

                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.getpacket(packet_bits);

                    senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
                    RTPsocket.send(senddp);

                    System.out.println("Send frame #" + imagenb + ", Frame size: " + image_length + " (" + buf.length + ")");

                }
                catch(Exception ex) {
                    ex.printStackTrace();
                    System.exit(0);
                }
        });
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        buf = new byte[20000];
    }

    public static void main(String[] argv) throws Exception
    {
        Server server = new Server();
        int RTSPport = 8888;

        ServerSocket listenSocket = new ServerSocket(RTSPport);
        server.RTSPsocket = listenSocket.accept();
        listenSocket.close();

        server.ClientIPAddr = server.RTSPsocket.getInetAddress();

        state = INIT;

        RTSPBufferedReader = new BufferedReader(new InputStreamReader(server.RTSPsocket.getInputStream()) );
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(server.RTSPsocket.getOutputStream()) );

        int request_type;
        boolean done = false;
        while(!done) {
            request_type = server.parseRequest(); //blocking

            if (request_type == SETUP) {
                done = true;
                state = READY;

                server.sendResponse();

                if(sourceName.equalsIgnoreCase("webcam"))
                {
                    server.frameStream = new WebCamStream();
                }else // its video file name
                {
                    server.frameStream = new MjpegStream(sourceName);
                }

                server.RTPsocket = new DatagramSocket();
            }
        }

        while(true) {
            request_type = server.parseRequest(); //blocking

            if ((request_type == PLAY) && (state == READY)) {

                server.sendResponse();

                server.timer.start();
                state = PLAYING;
            }
            else if ((request_type == PAUSE) && (state == PLAYING)) {
                server.sendResponse();

                server.timer.stop();
                state = READY;
            }
        }
    }

    private int parseRequest() {
        int request_type = -1;
        try {
            String RequestLine = RTSPBufferedReader.readLine();

            StringTokenizer tokens = new StringTokenizer(RequestLine);
            String request_type_string = tokens.nextToken();

            if ((request_type_string).compareTo("SETUP") == 0)
                request_type = SETUP;
            else if ((request_type_string).compareTo("PLAY") == 0)
                request_type = PLAY;
            else if ((request_type_string).compareTo("PAUSE") == 0)
                request_type = PAUSE;

            if (request_type == SETUP) {
                sourceName = tokens.nextToken();
            }

            String SeqNumLine = RTSPBufferedReader.readLine();
            tokens = new StringTokenizer(SeqNumLine);
            tokens.nextToken();
            RTSPSeqNb = Integer.parseInt(tokens.nextToken());

            String LastLine = RTSPBufferedReader.readLine();


            tokens = new StringTokenizer(LastLine);
            if (request_type == SETUP) {
                for (int i=0; i<3; i++)
                    tokens.nextToken();
                RTP_dest_port = Integer.parseInt(tokens.nextToken());
            }
            else {
                tokens.nextToken();
                RTSPid = tokens.nextToken();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
      
        return(request_type);
    }

    private void sendResponse() {
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK"+ newLine);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+ newLine);
            RTSPBufferedWriter.write("Session: "+RTSPid+ newLine);
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server.Server - Sent response to Client.Client.");
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }
}
