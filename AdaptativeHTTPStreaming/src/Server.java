//Server
//usage: java Server

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
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
						bw.write(send_HTTP_header_response(404));
					} else {
						bw.write(send_HTTP_header_response(200));
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
						bw.write(send_HTTP_header_response(404));
					} else {
						System.out.println(br.readLine()); // Host [...]
						System.out.println(br.readLine()); // Content-Type [...]
						System.out.println(br.readLine()); // Content-Lenght [...]
						br.readLine();
						int frameToSkip = Integer.parseInt(br.readLine());
						
						bw.write(send_HTTP_header_response(200));
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
					bw.write(send_HTTP_header_response(400));
				}
				
			} catch (SocketException e){
				theServer.label.setText("Error de socket, reinciando");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String send_HTTP_header_response(int code) {
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
		return response;
	
	}

	public static void write_line_output_stream(String line, OutputStream stream) throws IOException {
		stream.write(line.getBytes());
	}
	
}
