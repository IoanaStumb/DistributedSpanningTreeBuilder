package cluster;

import java.util.StringJoiner;

public class Message {
	public int identifier;
	public String action;
	public String optionalContent;
	
	public Message(int identifier, String message) {
		this.identifier = identifier;
		this.action = message;
	}
	
	public Message(int identifier, String message, String optionalContent) {
		this.identifier = identifier;
		this.action = message;
		this.optionalContent = optionalContent;
	}
	
	@Override
	public String toString() {
		StringJoiner stringJoiner = new StringJoiner(" | ");
		stringJoiner.add(String.valueOf(this.identifier));
		stringJoiner.add(this.action);
		
		if (this.optionalContent != null && this.optionalContent != "") {
			stringJoiner.add(this.optionalContent);
		}
		
		return stringJoiner.toString();
	}
	
	public byte[] toBytes() {
		return this.toString().getBytes();
	}
	
	public static Message convertToMessage(String input) {
		String[] tokens = input.split("\\|");
		
		if (tokens.length == 2) {
			return new Message(Integer.parseInt(tokens[0].trim()), tokens[1].trim());
		}	
		else {
			return new Message(Integer.parseInt(tokens[0].trim()), tokens[1].trim(), tokens[2].trim());
		}			
	}
	
	public static Message convertToMessage(byte[] input, int inputOffset, int inputLength) {
		String message = new String(input, inputOffset, inputLength);
		return Message.convertToMessage(message);
	}
}
