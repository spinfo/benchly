package benchly.model;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.Expose;

import benchly.database.TestEntrySetup;

/**
 * This handles how a storage credential may be exposed to the outside world. (By encrypting
 * the actual credential alongside a salt that was used to generate an appropriate key for
 * the encryption. The shared secret is only used indirectly to generate the encrypted credential.)
 */
public class StorageCredential {
	
	private static final Logger LOG = LoggerFactory.getLogger(StorageCredential.class);

	@Expose(deserialize = false)
	private String encryptedCredential;

	@Expose(deserialize = false)
	private String salt;

	protected StorageCredential(String plainCredential) { 
		char[] sharedSecret = TestEntrySetup.THE_SHARED_SECRET.toCharArray();
		int keySize = 256;
		int iterations = 60000;
		
		// generate a salt
		byte[] saltBytes = (new SecureRandomNumberGenerator()).nextBytes().getBytes();
		
		// generate a key from from the salt and the shared secret suitable for encryption 
		PBEKeySpec pbe = new PBEKeySpec(sharedSecret, saltBytes, iterations, keySize);
		Key key;
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			key = keyFactory.generateSecret(pbe);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			LOG.error("Unexpected error during key generation: " + e.getMessage());
			return;
		}

		// encrypt the credential using the key
		AesCipherService aes = new AesCipherService();
		aes.setKeySize(keySize);
		this.encryptedCredential = aes.encrypt(plainCredential.getBytes(), key.getEncoded()).toHex();
		this.salt = Hex.encodeToString(saltBytes);
	}

	public String getEnryptedCredential() {
		return encryptedCredential;
	}

	public String getSalt() {
		return salt;
	}
	
	

}
