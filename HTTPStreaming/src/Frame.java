
public class Frame {
	private byte[] imageData;
	private int length;
	
	public Frame(byte[] newImageData, int newLength){
		this.imageData = new byte[newLength];
		for (int i = 0; i < newLength; i++){
			this.imageData[i] = newImageData[i]; 
		}
		this.length = newLength;
	}
	
	public byte[] getImageData(){
		return this.imageData;
	}
	
	public int getLength(){
		return this.length;
	}
}
