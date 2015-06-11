import java.io.File;


public class RequestFile {
	long startByte;
	File requestFile;
	
	public RequestFile(long startByte, File requestFile) {
		this.startByte = startByte;
		this.requestFile = requestFile;
	}
	public long getStartByte() {
		return startByte;
	}
	public File getRequestFile() {
		return requestFile;
	}
}
