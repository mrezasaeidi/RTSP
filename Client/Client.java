package Client;

import Shared.RTPpacket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Client {
    private JLabel iconLabel = new JLabel();

    private DatagramSocket RTPsocket;        //socket to be used to send and receive UDP packets
    private static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets

    private Timer timer;
    private byte[] buf;

    private final static int INIT = 0;
    private final static int READY = 1;
    private final static int PLAYING = 2;
    private static int state;
    private Socket RTSPsocket;
    private InetAddress ServerIPAddr;

    //input and output stream filters
    private static BufferedReader RTSPBufferedReader;
    private static BufferedWriter RTSPBufferedWriter;
    private static String sourceName;
    private int RTSPSeqNb = 0;           //Sequence number
    private String RTSPid;              // ID of the RTSP session

    private final static String CRLF = "\r\n";

    private FrameSynchronizer fsynch;

    public Client() {
        JFrame f = new JFrame("Live Stream View");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,0));
        JButton playButton = new JButton("Start");
        playButton.setBackground(Color.GREEN);
        buttonPanel.add(playButton);
        JButton pauseButton = new JButton("Pause");
        pauseButton.setBackground(Color.red);
        buttonPanel.add(pauseButton);
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());

        iconLabel.setIcon(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,500,400);
        buttonPanel.setBounds(0,480,500,100);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(500,700));
        f.setVisible(true);

        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
        buf = new byte[15000];    

        fsynch = new FrameSynchronizer(100);
    }

    public static void main(String argv[]) throws Exception {
        Client theClient = new Client();

        int RTSP_server_port = 8888;
        String ServerHost = "localhost";
        theClient.ServerIPAddr = InetAddress.getByName(ServerHost);

        sourceName = "webcam";//argv[2];

        theClient.RTSPsocket = new Socket(theClient.ServerIPAddr, RTSP_server_port);

        RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSPsocket.getOutputStream()));

        state = INIT;
    }

    class playButtonListener implements ActionListener {
        boolean isSetup = false;

        public void setup(){
            if(isSetup)
            {
                return;
            }

            isSetup = true;

            if (state == INIT) {
                try {
                    RTPsocket = new DatagramSocket(RTP_RCV_PORT);
                    RTPsocket.setSoTimeout(50);
                }
                catch (SocketException se)
                {
                    se.printStackTrace();
                    System.exit(0);
                }

                RTSPSeqNb = 1;
                sendRequest("SETUP");

                if (parseServerResponse() != 200)
                    System.out.println("Invalid Server.Server Response");
                else
                {
                    state = READY;
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            if(!isSetup)
            {
                setup();
            }
            if (state == READY) {
                RTSPSeqNb++;

                sendRequest("PLAY");

                if (parseServerResponse() != 200) {
                    System.out.println("Invalid Server.Server Response");
                }
                else {
                    state = PLAYING;
                    timer.start();
                }
            }
        }
    }

    class pauseButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e){


            if (state == PLAYING) 
            {
                RTSPSeqNb++;

                sendRequest("PAUSE");

                if (parseServerResponse() != 200)
                    System.out.println("Invalid Server.Server Response");
                else 
                {
                    state = READY;
                    timer.stop();
                }
            }
        }
    }

    class timerListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            DatagramPacket rcvdp = new DatagramPacket(buf, buf.length);

            try {
                RTPsocket.receive(rcvdp);

                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                int seqNb = rtp_packet.getsequencenumber();

                System.out.println("Got RTP packet with SeqNum # " + seqNb
                                   + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                                   + rtp_packet.getpayloadtype());

                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                Toolkit toolkit = Toolkit.getDefaultToolkit();
                fsynch.addFrame(toolkit.createImage(payload, 0, payload_length), seqNb);

                ImageIcon icon = new ImageIcon(fsynch.nextFrame());
                iconLabel.setIcon(icon);
            }
            catch (InterruptedIOException iioe) {
                System.out.println("No data");
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private int parseServerResponse() {
        int reply_code = 0;

        try {
            String StatusLine = RTSPBufferedReader.readLine();
            System.out.println("RTSP Client.Client - Received from Server.Server:");
          
            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken();
            reply_code = Integer.parseInt(tokens.nextToken());

            if (reply_code == 200) {
                String SeqNumLine = RTSPBufferedReader.readLine();
                
                String SessionLine = RTSPBufferedReader.readLine();

                tokens = new StringTokenizer(SessionLine);
                String temp = tokens.nextToken();
                if (state == INIT && temp.compareTo("Session:") == 0) {
                    RTSPid = tokens.nextToken();
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
      
        return(reply_code);
    }

    private void sendRequest(String request_type) {
        try {
            RTSPBufferedWriter.write(request_type + " " + sourceName + " RTSP/1.0" + CRLF);

            RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);

            if (request_type == "SETUP") {
                RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
            }
            else if (request_type == "DESCRIBE") {
                RTSPBufferedWriter.write("Accept: application/sdp" + CRLF);
            }
            else {
                //otherwise, write the Session line from the RTSPid field
                RTSPBufferedWriter.write("Session: " + RTSPid + CRLF);
            }

            RTSPBufferedWriter.flush();
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }    
}
