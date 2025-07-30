package com.open.unicorn;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int KEY_LENGTH = 256;

    // In a production environment, this should be stored securely (e.g., in environment variables or a secure key management system)
    // This is a 32-byte (256-bit) key encoded in Base64 for AES-256 encryption
    // 32 bytes = 256 bits for AES-256
    private static final String MASTER_KEY_BASE64 = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=";

    private final SecretKey masterKey;
    private final SecureRandom secureRandom;

    public EncryptionService() {
        // Initialize the master key
        byte[] keyBytes = Base64.getDecoder().decode(MASTER_KEY_BASE64);
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }

        // Generate a random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmParameterSpec);

        // Encrypt
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Combine IV and ciphertext
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        // Return as Base64 string
        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }

        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // Extract IV and ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmParameterSpec);

        // Decrypt
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, "UTF-8");
    }

    // Method to generate a new master key (for development/testing purposes)
    public static String generateNewMasterKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(KEY_LENGTH);
        SecretKey key = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Method to test encryption/decryption
    public boolean testEncryption() {
        try {
            String testData = "Test secret value";
            String encrypted = encrypt(testData);
            String decrypted = decrypt(encrypted);
            return testData.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }
} 