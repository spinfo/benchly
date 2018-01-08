package benchly.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserMessages {

	public static enum Type {
		OK, INFO, WARN, ERROR;
	}

	public class Message {

		private final Type type;
		private final String text;

		public Message(Type type, String text) {
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

	private List<Message> messages;

	public UserMessages() {
		messages = new ArrayList<>(2);
	}
	
	public void addMessage(Type type, String message) {
		this.messages.add(new Message(type, message));
	}

	public List<Message> getMessages() {
		return Collections.unmodifiableList(this.messages);
	}

}
