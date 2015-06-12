/* ------------------
 Client
 usage: java Client [Server hostname] [Listening Port] [Video file requested]
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
    JButton resolutionButton = new JButton("Resolution");
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
    final static int WAITING = 4;
    static int state = -1; //state == INIT or READY or PLAYING
    Socket HTTPsocket; //socket used to send/receive HTTP
    static InputStream HTTPInputStream;
    static OutputStream HTTPOutputStream;
    static String VideoFileName; //video file to request to the server
    static String ManifestFileName; //nome do manifesto
    //static int HTTP_SND_PORT = 8000;
    static int port;
    
    static int videoSize;
    static int readUntilNow;
    
    static String ServerHost;
    final static String CRLF = "\r\n";
    
    //Decoder do vídeo
    static VideoStream videoDecoder;
    
    //Buffer de frames
    static LinkedList<Frame> frameBuffer;
    static String[] resolutions = {"A","B","C"};
    static String currentResolution; 
    
    //Manifesto
    Map<String,String> manifesto;

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
        buttonPanel.add(resolutionButton);
        setupButton.addActionListener(new setupButtonListener());
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());
        tearButton.addActionListener(new tearButtonListener());
        resolutionButton.addActionListener(new resolutionButtonListener());

        //Image display label
        iconLabel.setIcon(null);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,480,280);
        buttonPanel.setBounds(0,280,500,50);
/*        JOptionPane.showMessageDialog(f,
        	    "Eggs are not supposed to be green.");*/
        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(500,370));
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
	    
	    port = Integer.parseInt(argv[1]);
	    
	    //get video filename to request:
	    ManifestFileName = argv[2];
	    System.out.println(ManifestFileName + " pedindo manifesto na porta " + port);
	    try 
	    {
	    	theClient.HTTPsocket = new Socket(ServerIPAddr, port);
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
	        try{
		        if (state == INIT)
		        {
		        	if (HTTPsocket.isClosed()) {
		        		System.out.println("socket is closed");
		        		//Re-send a connection request 
		        		InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);
		        		HTTPsocket = new Socket(ServerIPAddr, port);
		            	HTTPInputStream = HTTPsocket.getInputStream();
		            	HTTPOutputStream = HTTPsocket.getOutputStream();
		        	}
		        	//start the timer
		        	send_HTTP_get_request(ManifestFileName);
		        	parse_HTTP_response_header();
		        	state = WAITING;        	
		        	System.out.println(state);
		        } //else if state != INIT then do nothing
	    } catch (Exception e1){
	    	e1.printStackTrace();
	    }
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
	
	//Handler for Resolution button
	//-----------------------
	class resolutionButtonListener implements ActionListener {
		public void actionPerformed (ActionEvent e) {
			System.out.println("Select resolutions!");
			timer.stop();
			currentResolution = (String) JOptionPane.showInputDialog(null,"Choose:","Select resolution",JOptionPane.QUESTION_MESSAGE,null,resolutions,resolutions[0]);
			timer.start();
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
			    		//get an Image object from the payload bitstream
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
	    				HTTPsocket.close();
	    			}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
	       	    	
	       
	    }
	}
	
	//------------------------------------
	//Parse Server Response
	//------------------------------------
	
	public int parse_HTTP_response_header() throws IOException{
		String HeaderLine = read_line_input_stream(HTTPInputStream);
		System.out.println(HeaderLine);
		
		if (HeaderLine == null) {
			return 0;
		}
		
		StringTokenizer tokens = new StringTokenizer(HeaderLine);
		tokens.nextToken(); //Skip HTTP Version
		int responseCode = Integer.parseInt(tokens.nextToken()); //Pega codigo de resposta
		
		if (responseCode != 200) {
			throw new IOException("HTTP não foi OK 200, recebi: "+Integer.toString(responseCode))
		}
		String responseDesc = "";
		for (int i = 0; i < tokens.countTokens(); i++){
			responseDesc = responseDesc + " " + tokens.nextToken(); //Pega descricao do erro
		} 
		
		//Agora vai ler as proximas linhas ate encontrar uma linha que so tenha CRLF, encontra
		//o comprimento do conteudo e retorna esse valor!
		int lengthVideo = 0;
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
		
		return lengthVideo;
	}
	
	public void parse_manifest_response(){
		try {
			parse_HTTP_response_header();
			BufferedReader is = new BufferedReader(new InputStreamReader(HTTPInputStream));
			int qtdVideos = Integer.parseInt(is.readLine());
			for (int i = 0; i < qtdVideos; i++) {
				String[] valores = is.readLine().split(",");
				manifesto.put(valores[0], valores[1]);
			}
		} catch (IOException e) {
			System.out.println(e.getStackTrace());
			System.exit(1);
		}
	}
	
	//------------------------------------
	//Send HTTP Request
	//------------------------------------

	public void send_HTTP_get_request(String VideoFileName)
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
