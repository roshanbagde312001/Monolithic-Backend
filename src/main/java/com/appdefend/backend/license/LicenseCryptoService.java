package com.appdefend.backend.license;

import com.appdefend.backend.config.LicenseProperties;
import com.appdefend.backend.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LicenseCryptoService {
    private final ObjectMapper objectMapper;
    private final PublicKey publicKey;

    public LicenseCryptoService(ObjectMapper objectMapper, LicenseProperties licenseProperties) {
        this.objectMapper = objectMapper.copy()
            .findAndRegisterModules()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.publicKey = readPublicKey(licenseProperties.getPublicKey());
    }

    public LicensePayload readPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, LicensePayload.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid license payload JSON");
        }
    }

    public String canonicalize(LicensePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to canonicalize license payload");
        }
    }

    public boolean verify(String canonicalPayload, String signatureBase64) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "License signature verification failed");
        }
    }

    public String fingerprint() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                if (i > 0) {
                    builder.append(':');
                }
                builder.append(String.format("%02x", digest[i]));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to compute public key fingerprint");
        }
    }

    public String decryptBundle(String encryptedPayloadBase64, String ivBase64, String saltBase64, String passphrase) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] cipherText = Base64.getDecoder().decode(encryptedPayloadBase64);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to decrypt license bundle");
        }
    }

    public EncryptedLicenseBundle encryptBundle(String canonicalPayload, String signatureBase64, String passphrase) {
        try {
            byte[] salt = java.security.SecureRandom.getInstanceStrong().generateSeed(16);
            byte[] iv = java.security.SecureRandom.getInstanceStrong().generateSeed(12);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8));

            return new EncryptedLicenseBundle(
                Base64.getEncoder().encodeToString(encrypted),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(salt),
                signatureBase64
            );
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to encrypt license bundle");
        }
    }

    private PublicKey readPublicKey(String publicKeyPem) {
        try {
            String normalized = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(normalized);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid configured license public key");
        }
    }
}
