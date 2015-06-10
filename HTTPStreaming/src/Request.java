
public class Request {
	String method;
	String requestedFile;
	
	public Request(String method, String requestedFile) {
		this.method = method;
		this.requestedFile = requestedFile;
	}
	
	public String getMethod() {
		return method;
	}

	public String getRequestedFile() {
		return requestedFile;
	}
	
}
