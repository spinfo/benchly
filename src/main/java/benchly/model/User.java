package benchly.model;

import java.sql.Timestamp;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.util.ByteSource;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * A minimal user object used for authentication via email
 */
@DatabaseTable(tableName = "user")
public class User extends Model {

	@DatabaseField(columnName = "id", generatedId = true)
	@Expose(deserialize = false)
	private long id;

	@DatabaseField(columnName = "name", index = true)
	@Expose
	private String name = "";
	
	@Expose(serialize = false)
	private String password;

	@DatabaseField(columnName = "passwordHash", index = true, canBeNull = false)
	private String passwordHash = "";

	@DatabaseField(dataType = DataType.BYTE_ARRAY, canBeNull = false)
	private byte[] passwordSalt;

	@DatabaseField(columnName = "email", index = true)
	@Expose
	private String email = "";

	@DatabaseField(columnName = "admin", canBeNull = false)
	@Expose
	private boolean isAdmin = false;

	@DatabaseField(columnName = "isDeleted", canBeNull = false)
	@Expose(deserialize = false)
	private boolean isDeleted = false;

	@DatabaseField(columnName = "createdAt", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private Timestamp createdAt;

	@DatabaseField(columnName = "updatedAt", canBeNull = false, index = true)
	@Expose(deserialize = false)
	private Timestamp updatedAt;

	public User() {
		// Empty constructor for ormlite
		this.createdAt = Timestamp.from(Instant.now());
	}

	public User(String name, String email, String password) {
		this();

		setName(name);
		setEmail(email);
		setPassword(password);
		
		initializeHashAndSalt();
	}
	
	public void initializeHashAndSalt() {
		// generate a random salt for the new user
		ByteSource salt = (new SecureRandomNumberGenerator()).nextBytes();

		// Now hash the plain-text password with the random salt and multiple
		// iterations and then Base64-encode the value (requires less space than Hex):
		String b64 = new Sha256Hash(password, salt, 1024).toBase64();
		setPasswordHash(b64);
		setPasswordSalt(salt.getBytes());
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	
	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public byte[] getPasswordSalt() {
		return passwordSalt;
	}

	public void setPasswordSalt(byte[] passwordSalt) {
		this.passwordSalt = passwordSalt;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isAdmin() {
		return this.isAdmin;
	}

	public void setAdmin(boolean admin) {
		this.isAdmin = admin;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public void setUpdatedAtNow() {
		this.updatedAt = Timestamp.from(Instant.now());
	}

	@Override
	public boolean validate() {
		valid = true;
		
		if (StringUtils.isBlank(this.name)) {
			addError("Name cannot be empty.");
		}
		if (StringUtils.isBlank(this.email)) {
			addError("Email cannot be empty.");
		}

		return isValid();
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

}
