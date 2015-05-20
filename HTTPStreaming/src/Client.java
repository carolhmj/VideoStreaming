/* ------------------
 Client
 usage: java Client [Server hostname] [Video file requested]
 ---------------------- */

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.Timer;


public class Client {

    //GUI
    //----
    JFrame f = new JFrame("Client");
    JButton setupButton = new JButton("Setup");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Teardown");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;
    
    //HTTP
    Timer timer; //timer used to read from buffer
    byte[] buf; //buffer used to store data received from the server
  
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    static int state; //state == INIT or READY or PLAYING
    Socket HTTPsocket; //socket used to send/receive HTTP
    static BufferedReader HTTPBufferedReader;
    static BufferedWriter HTTPBufferedWriter;
    static String VideoFileName; //video file to request to the server
    static int HTTP_RCV_PORT = 25000;
    static int HTTP_SND_PORT = 80;
    
    final static String CRLF = "\r\n";
    
    public Client() {
        
        //build GUI
        //--------------------------
        
        //Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        
        //Buttons
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
        setupButton.addActionListener(new setupButtonListener());
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());
        tearButton.addActionListener(new tearButtonListener());
        
        //Image display label
        iconLabel.setIcon(null);
        
        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);
        
        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390,370));
        f.setVisible(true);
        
        //init timer
        //--------------------------
        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
        
        //allocate enough memory for the buffer used to receive data from the server
        buf = new byte[15000];
    }

	//------------------------------------
	//main
	//------------------------------------
	public static void main(String argv[]) throws Exception
	{
	    //Create a Client object
	    Client theClient = new Client();
	    
	    //get server RTSP port and IP address from the command line
	    //------------------
	    String ServerHost = argv[0];
	    InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);
	    
	    //get video filename to request:
	    VideoFileName = argv[1];
	    
	    try 
	    {
	    	theClient.HTTPsocket = new Socket(ServerIPAddr, HTTP_SND_PORT); 
	    } 
	    catch(Exception e) 
	    { 
	    	e.printStackTrace();
	    }
	    state = READY;
	    
	    
	}
	
	
	//------------------------------------
	//Handler for buttons
	//------------------------------------
		
	//Handler for Setup button
	//-----------------------
	class setupButtonListener implements ActionListener{
	    public void actionPerformed(ActionEvent e){
	        
	        //System.out.println("Setup Button pressed !");
	        
	        if (state == INIT)
	        {
	            /*try{
	            	
	            }
	            catch (SocketException se)
	            {
	                System.out.println("Socket exception: "+se);
	                System.exit(0);
	            }*/
	            
	           
	            
	        }//else if state != INIT then do nothing
	    }
	}
	
	//Handler for Play button
	//-----------------------
	class playButtonListener implements ActionListener {
	    public void actionPerformed(ActionEvent e){
	        
	        System.out.println("Play Button pressed !");
	        
	        if (state == READY)
	        {
	            //increase RTSP sequence number
	            RTSPSeqNb++;
	            
	            
	            //Send PLAY message to the server
	            System.out.println("sending play request...");
	            send_RTSP_request("PLAY");
	            
	            //Wait for the response
	            if (parse_server_response() != 200)
	                System.out.println("Invalid Server Response");
	            else
	            {
	                //change RTSP state and print out new state
	                state = PLAYING;
	                System.out.println("New RTSP state: PLAYING");
	                
	                //start the timer
	                timer.start();
	            }
	        }//else if state != READY then do nothing
	    }
	}
	
	
	//Handler for Pause button
	//-----------------------
	class pauseButtonListener implements ActionListener {
	    public void actionPerformed(ActionEvent e){
	        
	        System.out.println("Pause Button pressed !");
	        
	        if (state == PLAYING)
	        {
	            //increase RTSP sequence number
	            RTSPSeqNb++;
	            
	            //Send PAUSE message to the server
	            send_RTSP_request("PAUSE");
	            
	            //Wait for the response
	            if (parse_server_response() != 200)
	                System.out.println("Invalid Server Response");
	            else
	            {
	                //change RTSP state and print out new state
	                state = READY;
	                System.out.println("New RTSP state: READY");
	                
	                //stop the timer
	                timer.stop();
	            }
	        }
	        //else if state != PLAYING then do nothing
	    }
	}
	
	//Handler for Teardown button
	//-----------------------
	class tearButtonListener implements ActionListener {
	    public void actionPerformed(ActionEvent e){
	        
	        System.out.println("Teardown Button pressed !");	        
	        timer.stop();
	        try {
				HTTPsocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	        System.exit(0);
	    }
	}
	
	
	//------------------------------------
	//Handler for timer
	//------------------------------------
	
	class timerListener implements ActionListener {
	    public void actionPerformed(ActionEvent e) {
	        
	        //Construct a DatagramPacket to receive data from the UDP socket
	        rcvdp = new DatagramPacket(buf, buf.length);
	        
	        try{
	            //receive the DP from the socket:
	            RTPsocket.receive(rcvdp);
	            
	            //create an RTPpacket object from the DP
	            RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
	            
	            //print important header fields of the RTP packet received:
	            //System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.getTimeStamp()+" ms, of type "+rtp_packet.getpayloadtype());
	            
	            //print header bitstream:
	            rtp_packet.printheader();
	            
	            //get the payload bitstream from the RTPpacket object
	            int payload_length = rtp_packet.getpayload_length();
	            byte [] payload = new byte[payload_length];
	            rtp_packet.getpayload(payload);
	            
	            //get an Image object from the payload bitstream
	            Toolkit toolkit = Toolkit.getDefaultToolkit();
	            Image image = toolkit.createImage(payload, 0, payload_length);
	            
	            //display the image as an ImageIcon object
	            icon = new ImageIcon(image);
	            iconLabel.setIcon(icon);
	        }
	        catch (InterruptedIOException iioe){
	        	//iioe.printStackTrace();
	            //System.out.println("Nothing to read");
	        }
	        catch (IOException ioe) {
	            System.out.println("Exception caught: "+ioe);
	        }
	    }
	}
	
	//------------------------------------
	//Parse Server Response
	//------------------------------------
	
	private int parse_HTTP_response_header(){
		try {
			String HeaderLine = HTTPBufferedReader.readLine();
			System.out.println(HeaderLine);
			
			StringTokenizer tokens = new StringTokenizer(HeaderLine);
			tokens.nextToken(); //Skip HTTP Version
			Integer responseCode = Integer.parseInt(tokens.nextToken());
			
			if (responseCode != 200){
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	//------------------------------------
	//Send HTTP Request
	//------------------------------------
	
	//.............
	//TO COMPLETE
	//.............
	
	private void send_RTSP_request(String request_type)
	{
	    try{
	        //Use the RTSPBufferedWriter to write to the RTSP socket
	        
	    	String request_line = request_type + " " + VideoFileName + " " + "RTSP/1.0" + CRLF;
	    	//write the request line:
	        RTSPBufferedWriter.write(request_line);
	        
	        //write the CSeq line: 
	        Integer CSeq = RTSPSeqNb; 
	        String CSeq_line = "CSeq: " + CSeq.toString() + CRLF;
	        
	        RTSPBufferedWriter.write(CSeq_line);
	        
	        String transport_line;
	        //check if request_type is equal to "SETUP" and in this case write the Transport: line advertising to the server the port used to receive the RTP packets RTP_RCV_PORT
	        if (request_type.compareTo("SETUP") == 0){
	        	transport_line = "Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF;
	        }
	        //otherwise, write the Session line from the RTSPid field
	        else {
	        	transport_line = "Session: " + RTSPid + CRLF;
	        }
	        RTSPBufferedWriter.write(transport_line);
	        
	        RTSPBufferedWriter.flush();
	    }
	    catch(Exception ex)
	    {
	        System.out.println("Exception caught: "+ex);
	        System.exit(0);
	    }
	}
	
}
