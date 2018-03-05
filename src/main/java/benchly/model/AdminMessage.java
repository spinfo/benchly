package benchly.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="admin_message")
public class AdminMessage {

	@DatabaseField(columnName = "id", generatedId = true)
	private long id;

	@DatabaseField(columnName = "serverContact", foreign = true, foreignAutoRefresh = false, canBeNull = true)
	private ServerContact serverContact;

	@DatabaseField(columnName = "content")
	private String content;

	public AdminMessage() {
	}

	public AdminMessage(String content) {
		this.content = content;
	}

	public AdminMessage(ServerContact contact, String content) {
		this(content);
		this.serverContact = contact;
	}

	public long getId() {
		return id;
	}

	public ServerContact getServerContact() {
		return serverContact;
	}

	public String getContent() {
		return content;
	}

}
