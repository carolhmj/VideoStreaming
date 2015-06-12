
//VideoStream

import java.io.*;

public class VideoStream {
    
    InputStream fis; //video file
    int frame_nb; //current frame nb
    
    //-----------------------------------
    //constructor
    //-----------------------------------
    public VideoStream(String filename) throws Exception{
        
        //init variables
        //fis = new FileInputStream("/home/carol/UFC/GitHub/VideoStreaming/HTTPStreaming/video/pacote2"); // o que pode dar errado
    	fis = new FileInputStream(filename);
    	frame_nb = 0;
    }
    
    public VideoStream(InputStream stream){
    	fis = stream;
    	frame_nb = 0;
    }
    
    public VideoStream(File file) throws Exception{
    	fis = new FileInputStream(file);
    }
    
    //-----------------------------------
    // getnextframe
    //returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
    public int getnextframe(byte[] frame) throws Exception
    {
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];
        
        //read current frame length
        fis.read(frame_length,0,5);
        
        //transform frame_length to integer
        length_string = new String(frame_length);
        //System.out.println(length_string + " length string");
        length = Integer.parseInt(length_string);
        //System.out.println(length + " length number");
       
        frame_nb++;
        return(fis.read(frame,0,length));
    }
    
    public Frame getnextframe() throws Exception
    {
    	if (fis.available() == 0) {
    		return null;
    	}
    	
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];
        
        //read current frame length
        frame_length[0] = (byte)fis.read();
        frame_length[1] = (byte)fis.read();
        frame_length[2] = (byte)fis.read();
        frame_length[3] = (byte)fis.read();
        frame_length[4] = (byte)fis.read();
        
		//for (int i = 0; i < 5; i++){
		//	System.out.print(frame_length[i] + " ");
		//}
        //System.out.println("");
        
        //transform frame_length to integer
        length_string = new String(frame_length);
        //System.out.println(length_string + " length string");
        length = Integer.parseInt(length_string);
        //System.out.println(length + " length number");
       
        byte[] frame = new byte[length];
        for (int i=0;i<length;i++) {
	        int read_bytes = fis.read();
	        if (read_bytes == -1)
	        	throw new Exception("EOF");
	        frame[i] = (byte) read_bytes;
        }

        
        frame_nb++;
        return new Frame(frame, length);
    }
    
    public void skipFrames(long nframes) throws Exception{
    	long readFrames = 0;
    	
    	while (readFrames <= nframes){
    		getnextframe(); //calls getnextframe the needed number of times and ignores result
    		readFrames++;
     	}
    }
}
