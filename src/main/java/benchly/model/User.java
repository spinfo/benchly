package benchly.model;

import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Based off
 * https://git.allions.net/Robin/spark_java_with_apache_shiro_integration/src/master/src/main/java/com/ondev/robin/shiro_spark/user/User.java
 * 
 * TODO: Check if all fields are used
 */
@DatabaseTable(tableName = "user")
public class User {

	@Expose
	@DatabaseField(columnName = "id", generatedId = true)
	private long id;

	@Expose
	@DatabaseField
	private String alias = "";

	@Expose
	@DatabaseField(columnName = "name", index = true)
	private String name = "";

	@DatabaseField(columnName = "passwordHash", index = true)
	private String passwordHash = "";

	@DatabaseField(dataType = DataType.BYTE_ARRAY)
	private byte[] passwordSalt;

	@Expose
	@DatabaseField(columnName = "email", index = true)
	private String email = "";

	@DatabaseField(columnName = "verifiactionId", index = true)
	private String verificationId;

	@DatabaseField
	private boolean activated = false;

	private Subject subject;

	public User() {
		// Empty constructor for ormlite
	}

	public User(String name, String email, String password) {
		setName(name);
		setEmail(email);

		// generate a random salt for the new user
		ByteSource salt = (new SecureRandomNumberGenerator()).nextBytes();

		// Now hash the plain-text password with the random salt and multiple
		// iterations and then Base64-encode the value (requires less space than Hex):
		String b64 = new Sha256Hash(password, salt, 1024).toBase64();
		setPasswordHash(b64);
		setPasswordSalt(salt.getBytes());
	}

	// Check if the subject is authenticated
	public boolean isLoggedIn() {
		if (getSubject() != null) {
			if (getSubject().isAuthenticated()) {
				return true;
			}
		}
		return false;
	}

	public long getId() {
		return id;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
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

	public String getVerificationId() {
		return verificationId;
	}

	public void setVerificationId(String verificationId) {
		this.verificationId = verificationId;
	}

	public boolean isActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	public Subject getSubject() {
		return subject;
	}

	public void setSubject(Subject subject) {
		this.subject = subject;
	}

}
