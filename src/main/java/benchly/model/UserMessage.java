package benchly.model;

import com.google.gson.annotations.Expose;

public class UserMessage {

	public static enum Type {
		OK, INFO, WARN, ERROR;
	}

	@Expose
	private final Type type;

	@Expose
	private final String text;

	public UserMessage(Type type, String text) {
		this.type = type;
		this.text = text;
	}

	public Type getType() {
		return type;
	}

	public String getText() {
		return text;
	}
}
