
public class Request {
	String method;
	String requestedFile;
	String requestBody;
	
	public Request(String method, String requestedFile) {
		this.method = method;
		this.requestedFile = requestedFile;
	}
	
	public Request(String method, String requestedFile, String requestBody){
		this.method = method;
		this.requestedFile = requestedFile;
		this.requestBody = requestBody;
	}
	
	public String getMethod() {
		return method;
	}

	public String getRequestedFile() {
		return requestedFile;
	}
	
	public String getRequestBody() {
		return requestBody;
	}
	
}
