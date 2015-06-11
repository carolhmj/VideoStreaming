//Server 
//usage: java Server

import java.awt.BorderLayout;
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
    static int LISTENING_PORT = 8000;
    
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
        label = new JLabel("Listening on port " + LISTENING_PORT + "...", JLabel.CENTER);
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
        
        
        //Initiate TCP connection with the client
        server = new ServerSocket(LISTENING_PORT);
        client = server.accept();
        server.close();
        clientInputStream = client.getInputStream();
        clientOutputStream = client.getOutputStream();
        
        //Get Client IP address
        theServer.label.setText("Received connection request from client " + client.getInetAddress().getHostAddress());
        
		//Wait for the client to return the desired file
		while (true){
			Request request = parse_HTTP_request(); //Faz alguma coisa com o request
			if (request.getMethod().equals("GET")){
				try{
					//If file exists and the server has permission to read, send OK response
					FileInputStream videoFile = new FileInputStream(request.getRequestedFile());
					theServer.label.setText("Sending OK response to requested file " + request.getRequestedFile());
					send_HTTP_header_response(200);
					theServer.label.setText("Sending file...");
					int readByte;
					while ((readByte = videoFile.read()) != -1){
						clientOutputStream.write(readByte);
					}
					videoFile.close();
					
				} catch (Exception e) {
					//else, send file not found status code
					theServer.label.setText("Sending file not found response to requested file " + request.getRequestedFile());
					send_HTTP_header_response(404);
				}	
			} else {
				//If the request wasn't a GET
				theServer.label.setText("Sending bad request response");
				send_HTTP_header_response(400);
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
    
    public static void send_HTTP_header_response(int code){
    	try {
    		String response;
	    	if (code == 200){
	    		response = "HTTP/1.0 200 OK" + CRLF;
	    	} else if (code == 404){
	    		response = "HTTP/1.0 404 Not Found" + CRLF;
	    	} else if (code == 403){
	    		response = "HTTP/1.0 403 Forbidden" + CRLF;
	    	} else {
	    		response = "HTTP/1.0 400 Bad Request" + CRLF;
	    	}
			write_line_output_stream(response, clientOutputStream);
			response = "" + CRLF;
			write_line_output_stream(response, clientOutputStream);
			
    	} catch (IOException e) {
			e.printStackTrace();
		}
	
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