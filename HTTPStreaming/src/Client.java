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
    Integer length; //video duration
    
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    final static int PAUSED = 3;
    static int state = -1; //state == INIT or READY or PLAYING
    Socket HTTPsocket; //socket used to send/receive HTTP
    static InputStream HTTPInputStream;
    static OutputStream HTTPOutputStream;
    static String VideoFileName; //video file to request to the server
    static int HTTP_SND_PORT = 8000;
    
    static int videoSize;
    static int readUntilNow;
    
    static String ServerHost;
    final static String CRLF = "\r\n";
    
    //Decoder do vídeo
    static VideoStream videoDecoder;
    
    //Buffer de frames
    static LinkedList<Frame> frameBuffer;
    
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
        timer = new Timer(33, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
        
        frameBuffer = new LinkedList<>();
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
	    System.out.println(VideoFileName + " requested video");
	    
	    try 
	    {
	    	theClient.HTTPsocket = new Socket(ServerIPAddr, HTTP_SND_PORT);
        	HTTPInputStream = theClient.HTTPsocket.getInputStream();
        	HTTPOutputStream = theClient.HTTPsocket.getOutputStream();
        	state = INIT;
	    } 
	    catch(Exception e) 
	    { 
	    	e.printStackTrace();
	    }
	    
	}
	
	
	//------------------------------------
	//Handler for buttons
	//------------------------------------
		
	//Handler for Setup button
	//-----------------------
	class setupButtonListener implements ActionListener{
	    public void actionPerformed(ActionEvent e){
	        
	        System.out.println("Setup Button pressed !");
	        
	        if (state == INIT)
	        {
	        	//start the timer
	        	send_HTTP_get_request();
	        	int response = parse_HTTP_response_header();
	        	System.out.println("videoSize " + videoSize);
	        	if (response == 200){
	        		try {
			        	videoDecoder = new VideoStream(HTTPInputStream);		
			        	state = READY;
	        		} catch (Exception ex){
	        			ex.printStackTrace();
	        		}
		        	
		        	System.out.println(state);
	        	}
	        } //else if state != INIT then do nothing
	    }
	}
	
	//Handler for Play button
	//-----------------------
	class playButtonListener implements ActionListener {
	    public void actionPerformed(ActionEvent e){
	        
	        System.out.println("Play Button pressed !");
	        try {
		        if (state == READY)
		        {
	        		timer.start();
	        		state = PLAYING;
	        		
		        }//else if state != READY then do nothing
			} catch (Exception e2) {
				e2.printStackTrace();
			}

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
	        	state = READY;
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
	        	    		
	    		try {
	    			
	    			Frame frame = videoDecoder.getnextframe();
	    			if (frame != null){
		    			readUntilNow += frame.getLength();
		    			System.out.println("read until now: " + readUntilNow);
/*		    			if (readUntilNow == videoSize){
		    				state = INIT;
		    				timer.stop();
		    			}
*/			    		//get an Image object from the payload bitstream
						Toolkit toolkit = Toolkit.getDefaultToolkit();
			            //Image image = toolkit.createImage(frame, 0, frame_len);
			            Image image = toolkit.createImage(frame.getImageData(), 0, frame.getLength());
	
			            //display the image as an ImageIcon object
			            icon = new ImageIcon(image);
			            iconLabel.setIcon(icon);
	    			}
	    			else{
	    				state = INIT;
	    				timer.stop();
	    			}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
	       	    	
	       
	    }
	}
	
	//------------------------------------
	//Parse Server Response
	//------------------------------------
	
	public Integer parse_HTTP_response_header(){
		try {
			String HeaderLine = read_line_input_stream(HTTPInputStream);
			System.out.println(HeaderLine);
			
			if (HeaderLine == null) {
				return 0;
			}
			
			StringTokenizer tokens = new StringTokenizer(HeaderLine);
			tokens.nextToken(); //Skip HTTP Version
			Integer responseCode = Integer.parseInt(tokens.nextToken()); //Pega codigo de resposta
			String responseDesc = "";
			for (int i = 0; i < tokens.countTokens(); i++){
				responseDesc = responseDesc + " " + tokens.nextToken(); //Pega descricao do erro
			} 
			
			//Agora vai ler as proximas linhas ate encontrar uma linha que so tenha CRLF, encontra
			//o comprimento do conteudo e retorna esse valor!
			Integer lengthVideo = 0;
			do {
				HeaderLine = read_line_input_stream(HTTPInputStream);
				System.out.println(HeaderLine);
				tokens = new StringTokenizer(HeaderLine);
				if (tokens.hasMoreTokens()) {
					String headerAttr = tokens.nextToken(); //descobre qual o atributo daquela linha
					if (headerAttr.equals("Content-Length:")){
						lengthVideo = Integer.parseInt(tokens.nextToken());
					}
				}
			} while (!HeaderLine.equals(""));
			
			return responseCode;
			
			//return 0; //nao era pra acontecer, mas ne...
			
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
	    	String methodLine = "GET " + VideoFileName + " HTTP/1.0" + CRLF;
		    System.out.print(methodLine);
		    //if (HTTPOutputStream == null) { System.out.println("null");}
			write_line_output_stream(methodLine, HTTPOutputStream);
			String hostLine = "Host: " + ServerHost + CRLF;
			System.out.print(hostLine);
			write_line_output_stream(hostLine, HTTPOutputStream);
			//Escreve o fim do header fields
			write_line_output_stream(CRLF, HTTPOutputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//------------------------------------
	//Read a line of InputStream
	//------------------------------------
	
	public String read_line_input_stream(InputStream stream) throws IOException{
		String constructedLine = "";
		String readChar;
		byte[] byteChar = new byte[1];
		Boolean carrReturn = false;
		while (stream.read(byteChar) == 1){
			readChar = new String(byteChar);
			if (readChar.matches("\r")) {
				carrReturn = true;
			} else if (readChar.matches("\n") && carrReturn) {
				return constructedLine;
			} else {
				constructedLine = constructedLine + readChar;
			}
		}
		return null;	
	}
	
	
	public void write_line_output_stream(String line, OutputStream stream) throws IOException{
		stream.write(line.getBytes());
	}
}
