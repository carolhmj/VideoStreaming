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

import com.sun.xml.internal.ws.org.objectweb.asm.Label;

public class Server extends JFrame {
    //HTTP variables:
    //----------------
    static ServerSocket server;
    static Socket client;
	static InputStream clientInputStream;
    static OutputStream clientOutputStream; 
    static int HTTP_LISTENING_PORT = 80;
    
    //GUI:
    //----------------
    JLabel label;
    
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
                System.exit(0);
            }});
        
        //GUI:
        label = new JLabel("Waiting for client request...", JLabel.CENTER);
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
     
        while (true){
        	//Initiate TCP connection with the client
            server = new ServerSocket(HTTP_LISTENING_PORT);
            client = server.accept();
            server.close();
            clientInputStream = client.getInputStream();
            clientOutputStream = client.getOutputStream();
        	theServer.label.setText("Got connection request from client" + client.getInetAddress().getHostAddress());
			
			//Wait for client request
			Request request = parse_HTTP_request();
			if (request.getMethod().equals("GET")){
				FileInputStream videoFile = new FileInputStream(request.getRequestedFile());
				int readByte;
				while ((readByte = videoFile.read()) != -1){
					clientOutputStream.write(readByte);
				}
				videoFile.close();
			} else {
				
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
    
    public static void send_HTTP_header_response(int statusCode){
    	
    }
    
	public static String read_line_input_stream(InputStream stream) throws IOException{
		String constructedLine = "";
		String readChar;
		byte[] byteChar = new byte[1];
		boolean carrReturn = false;
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