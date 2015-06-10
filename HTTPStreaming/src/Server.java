//Server 
//usage: java Server

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;


public class Server extends JFrame {
    //HTTP variables:
    //----------------
    static ServerSocket server;
    static Socket client;
	static InputStream clientInputStream;
    static OutputStream clientOutputStream; 
    InetAddress ClientIPAddr; //Client IP address
    static int HTTP_LISTENING_PORT = 8000;
    
    //GUI:
    //----------------
    JLabel label;
    
    //Video variables:
    //----------------
    int imagenb = 0; //image nb of the image currently transmitted
    int totalframes = 0; //total number of frames to stop the video
    static VideoStream video; //VideoStream object used to access video frames
    
    Timer timer; //timer used to send the images at the video frame rate 
    
    final static String CRLF = "\r\n";
    
    //--------------------------------
    //Constructor
    //--------------------------------
    public Server(){
        
        //init Frame
        super("Server");
        
        //Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //stop the timer and exit
                timer.stop();
                System.exit(0);
            }});
        
        //GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);
    }
    
    //------------------------------------
    //main
    //------------------------------------
    public static void main(String argv[]) throws Exception
    {
        //create a Server object
        Server theServer = new Server();
        
        //show GUI:
        theServer.pack();
        theServer.setVisible(true);
        
        
        //Initiate TCP connection with the client for the RTSP session
        server = new ServerSocket(HTTP_LISTENING_PORT);
        client = server.accept();
        server.close();
        clientInputStream = client.getInputStream();
        clientOutputStream = client.getOutputStream();
        
        //Get Client IP address
        theServer.ClientIPAddr = client.getInetAddress();
        
        //Get Requested Manifest and send it to the client
        //String requestedManifest = parse_HTTP_request();
        //System.out.println(requestedManifest);
        //send_HTTP_header_response(200);
        //BufferedReader manifestReader = new BufferedReader(new FileReader(requestedManifest));
        //FileReader manifestReader = new FileReader(requestedManifest);
        //char[] line;
		//while(manifestReader.read(line) != -1){
        //	write_line_output_stream(new String(line), clientOutputStream);
        //}
		
		//Wait for the client to return the desired file
		while (true){
			Request request = parse_HTTP_request(); //Faz alguma coisa com o request
			send_HTTP_header_response(request);
			if (request.getMethod().equals("GET")){
				FileInputStream videoFile = new FileInputStream(request.getRequestedFile());
				int readByte;
				while ((readByte = videoFile.read()) != -1){
					clientOutputStream.write(readByte);
				}
				videoFile.close();
			} 
		}
    }
    
    //----------------------
    //Parse HTTP Request
    //----------------------    
    public static Request parse_HTTP_request() throws IOException{
    	String headerLine = read_line_input_stream(clientInputStream);
    	System.out.println(headerLine);
    	StringTokenizer tokens = new StringTokenizer(headerLine);
    	String requestedMethod = tokens.nextToken();
    	if (requestedMethod.equals("GET")){
    		String filePath = tokens.nextToken();
    		tokens.nextToken(); //pula a versão
    		do {
    			headerLine = read_line_input_stream(clientInputStream);
    		} while(!headerLine.equals(""));
    		return new Request(requestedMethod, filePath);
    	} //por enquanto nao faz nada se o metodo nao for GET
    	return null;
    }
    
    public static void send_HTTP_header_response(Request request){
    	try {
	    	if (request.getMethod().equals("GET")){
	    		String response = "HTTP/1.1 200 OK" + CRLF;
    			write_line_output_stream(response, clientOutputStream);
    			response = "" + CRLF;
    			write_line_output_stream(response, clientOutputStream);
	    	}
    	} catch (IOException e) {
			e.printStackTrace();
		}
	
    }
    
	public static String read_line_input_stream(InputStream stream) throws IOException{
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
	
	
	public static void write_line_output_stream(String line, OutputStream stream) throws IOException{
		stream.write(line.getBytes());
	}
}