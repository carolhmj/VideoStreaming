//Server
//usage: java Server

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Server extends JFrame {
	private static final long serialVersionUID = -8479299801735514606L;
	//HTTP Variables
	static Socket clientSocket;
	static InputStream clientInputStream;
	static OutputStream clientOutputStream;
	static int LISTENING_PORT = 8000;
	
	//GUI
    JLabel label;
    
    final static String CRLF = "\r\n";
    
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
    
    public static void main(String[] args) {
    	//create a Server object
        Server theServer = new Server();
        
        //show GUI:
        theServer.pack();
        theServer.setVisible(true);
        
        while(true) {
	        try {
		        //Initiate TCP connection with the client
	        	theServer.label.setText("Esperando conexão na porta "+LISTENING_PORT);
		        ServerSocket server = new ServerSocket(LISTENING_PORT);
		        clientSocket = server.accept();
		        server.close();
		        clientInputStream = clientSocket.getInputStream();
		        clientOutputStream = clientSocket.getOutputStream();
		        BufferedReader br = new BufferedReader(new InputStreamReader(clientInputStream));
		        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientOutputStream));
				theServer.label.setText("Received connection request from client " + clientSocket.getInetAddress().getHostAddress() + ".");
				
				String[] header = br.readLine().split(" ");
				theServer.label.setText(header[0] + " " + header[1]);
				if (header[0].equals("GET")) {
					while (br.readLine().equals("")) {}
					System.out.println("Começando a mandar");
					File requested = new File("."+header[1]);
					if (!requested.canRead()) {
						System.out.println("Falha:" + requested.exists());
						System.out.println("File: ." + header[1]);
						send_HTTP_header_response(404);
					} else {
						send_HTTP_header_response(200);
						bw.write("Content-Lenght: "+Long.toString(requested.length())+CRLF);
						if (header[1].endsWith(".mjpeg"))
							bw.write("Content-Type: video/x-motion-jpeg"+CRLF);
						else
							bw.write("Content-Type: text/plain"+CRLF);
						bw.write(CRLF);
						FileInputStream fs = new FileInputStream(requested);
						while (fs.available() > 0)
							bw.write(fs.read());
						fs.close();
						bw.flush();
						clientSocket.close();
						System.out.println("Enviado!");
						theServer.label.setText("Enviado!");
					}
				} else if (header[0].equals("POST")) {
					System.out.println("Começando a mandar");
					File requested = new File("."+header[1]);
					if (!requested.canRead()) {
						System.out.println("Falha:" + requested.exists());
						System.out.println("File: ." + header[1]);
						send_HTTP_header_response(404);
					} else {
						System.out.println(br.readLine()); // Host [...]
						System.out.println(br.readLine()); // Content-Type [...]
						System.out.println(br.readLine()); // Content-Lenght [...]
						br.readLine();
						int frameToSkip = Integer.parseInt(br.readLine());
						
						send_HTTP_header_response(200);
						bw.write("Content-Lenght: "+Long.toString(requested.length())+CRLF);
						bw.write("Content-Type: video/x-motion-jpeg"+CRLF);
						bw.write(CRLF);
						bw.flush();
						
						VideoStream videoSend = new VideoStream(requested);
						videoSend.skipFrames(frameToSkip);
						int readVideoByte;
						while ((readVideoByte = videoSend.fis.read()) != -1)
							clientOutputStream.write(readVideoByte);
						
						videoSend.fis.close();
						clientSocket.close();
						System.out.println("Enviado!");
						theServer.label.setText("Enviado!");
					}
				}
				else {
					send_HTTP_header_response(400);
				}
				
	        } catch (SocketException e){
	        	theServer.label.setText("Error de socket, reinciando");
	        } catch (Exception e) {
	        	e.printStackTrace();
			}
        }
    }
    
    public static RequestFile manifest_request_response() throws IOException {
		Request manifReq;
		int responseCode = 0;
	
		manifReq = parse_HTTP_request();
		if (manifReq.getMethod().equals("GET")) {
			try {
				File manifest = new File("."+manifReq.getRequestedFile());
				
				if (!manifest.exists() || !manifest.canRead()){
					responseCode = 404;
					send_HTTP_header_response(404);
					return null;
				}
				
				long len = manifest.length();
				String extraHeaders = new String(); 
				extraHeaders.concat("Content-Length: " + String.valueOf(len) + CRLF);
				extraHeaders.concat("Content-Type: text/plain");
				responseCode = 200;
				send_HTTP_header_response(responseCode, extraHeaders);
				
				return new RequestFile(0, manifest);
				
			} catch (Exception e){
				responseCode = 404;
				send_HTTP_header_response(responseCode);
				return null;
			}
		} else {
			responseCode = 400;
			send_HTTP_header_response(responseCode);
			return null;
		}

	
    }
    
    public static RequestFile video_request_response() throws IOException {
		Request fileReq = null;
		int responseCode = 0;
		File video;
		fileReq = parse_HTTP_request();
		if (fileReq == null) return null;
		if (fileReq.getMethod().equals("POST")){
			String reqBody = fileReq.getRequestBody();
			long reqByte;
			try{
				reqByte = Integer.parseInt(reqBody);
			} catch (NumberFormatException e){
				responseCode = 400;
				send_HTTP_header_response(responseCode);
				return null;
			}
			System.out.println("File:"+fileReq.getRequestedFile());
			video = new File("."+fileReq.getRequestedFile());
			if (!video.exists()){
				responseCode = 404;
				send_HTTP_header_response(responseCode);
				return null;
			}
			long len = video.length();
			String extraHeader = new String();
			extraHeader.concat("Content-Length: " + String.valueOf(len) + CRLF);
			extraHeader.concat("Content-Type: text/plain" + CRLF);
		
			send_HTTP_header_response(200, extraHeader);
			return new RequestFile(reqByte, video);
			
		} else {
			responseCode = 400;
			send_HTTP_header_response(responseCode);
			return null;
		}
	
    }
    
    public static Request parse_HTTP_request() throws IOException{
    	String headerLine = read_line_input_stream(clientInputStream);
    	System.out.println("linha: "+headerLine);
    	StringTokenizer tokens = new StringTokenizer(headerLine);
    	try {
    		String requestedMethod = tokens.nextToken();
	    	if (requestedMethod.equals("GET")){
	    		
	    		String filePath = tokens.nextToken();
	    		tokens.nextToken(); //skips version
	    		do {
	    			headerLine = read_line_input_stream(clientInputStream);
	    		} while(!headerLine.equals(""));
	    		return new Request(requestedMethod, filePath);
	    		
	    	} else if (requestedMethod.equals("POST")){
	    		
	    		String filePath = tokens.nextToken();
	    		tokens.nextToken();
	    		do {
	    			headerLine = read_line_input_stream(clientInputStream);
	    		} while(!headerLine.equals(""));
	    		String requestBody = read_line_input_stream(clientInputStream);
	    		return new Request(requestedMethod, filePath, requestBody);
	    		
	    	}
	    	return null;
    	} catch (NoSuchElementException e) {
    		return null;
    	}
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
//			write_line_output_stream(CRLF, clientOutputStream);
			
    	} catch (IOException e) {
			e.printStackTrace();
		}
	
    }
    
    public static void send_HTTP_header_response(int code, String extraHeaders){
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
			write_line_output_stream(extraHeaders, clientOutputStream);
			write_line_output_stream(CRLF, clientOutputStream);
			
    	} catch (IOException e) {
			e.printStackTrace();
		}
	
    }
    
	public static String read_line_input_stream(InputStream stream) throws IOException {
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
	
	
	public static void write_line_output_stream(String line, OutputStream stream) throws IOException {
		stream.write(line.getBytes());
	}
	
}
