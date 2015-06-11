import java.io.File;


public class RequestFile {
	long start;
	File requestFile;
	
	public RequestFile(long start, File requestFile) {
		this.start = start;
		this.requestFile = requestFile;
	}
	public long getStart() {
		return start;
	}
	public File getRequestFile() {
		return requestFile;
	}
}
