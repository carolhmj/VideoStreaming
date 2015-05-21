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
    Integer length; //video duration
    
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
    
    static String ServerHost;
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
	    ServerHost = argv[0];
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
	    state = INIT;
	    
	    
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
	        	send_HTTP_get_request();
	        	length = parse_HTTP_response_header();
	        	state = READY;
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
	           
	                
	            //start the timer
	        	timer.start();
	            
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
	            
	        	timer.stop();
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
	        
	       
	            /*int payload_length = rtp_packet.getpayload_length();
	            byte [] payload = new byte[payload_length];
	            rtp_packet.getpayload(payload);
	            
	            //get an Image object from the payload bitstream
	            Toolkit toolkit = Toolkit.getDefaultToolkit();
	            Image image = toolkit.createImage(payload, 0, payload_length);
	            
	            //display the image as an ImageIcon object
	            icon = new ImageIcon(image);
	            iconLabel.setIcon(icon);*/
	    	
	    	
	       
	    }
	}
	
	//------------------------------------
	//Parse Server Response
	//------------------------------------
	
	public Integer parse_HTTP_response_header(){
		try {
			String HeaderLine = HTTPBufferedReader.readLine();
			System.out.println(HeaderLine);
			
			StringTokenizer tokens = new StringTokenizer(HeaderLine);
			tokens.nextToken(); //Skip HTTP Version
			Integer responseCode = Integer.parseInt(tokens.nextToken()); //Pega código de resposta
			String responseDesc = tokens.nextToken(); //Pega descrição do erro
			
			if (responseCode != 200){
				System.out.println("Resposta HTTP: " + responseCode + responseDesc);
				return 0;
			} 
			
			//Agora vai ler as próximas linhas até encontrar uma linha que só tenha CRLF, encontra
			//o comprimento do conteúdo e retorna esse valor!
			do {
				HeaderLine = HTTPBufferedReader.readLine();
				tokens = new StringTokenizer(HeaderLine);
				String headerAttr = tokens.nextToken(); //descobre qual é o atributo daquela linha
				if (headerAttr.equals("Content-Length:")){
					Integer length = Integer.parseInt(tokens.nextToken());
					return length;
				}
			} while (!HeaderLine.equals(""));
			
			return 0; //não era pra acontecer, mas né...
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	//------------------------------------
	//Send HTTP Request
	//------------------------------------

	public void send_HTTP_get_request()
	{
	    try {
	    	String methodLine = "GET " + VideoFileName + " http\1.1" + CRLF;
		    System.out.print(methodLine);
			HTTPBufferedWriter.write(methodLine);
			String hostLine = "Host: " + ServerHost + CRLF;
			System.out.print(hostLine);
			HTTPBufferedWriter.write(hostLine);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
