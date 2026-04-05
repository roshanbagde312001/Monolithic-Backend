package com.appdefend.backend.license;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class OfflineLicenseGenerator {
    private OfflineLicenseGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Usage: OfflineLicenseGenerator <privateKeyPemPath> <deploymentId> <customerName> <maxNamedUsers> <validUntil> [bundlePassphrase]");
            System.exit(1);
        }

        String privateKeyPem = Files.readString(Path.of(args[0]));
        String deploymentId = args[1];
        String customerName = args[2];
        int maxNamedUsers = Integer.parseInt(args[3]);
        LocalDate validUntil = LocalDate.parse(args[4]);
        String bundlePassphrase = args.length >= 6 ? args[5] : null;

        LicensePayload payload = new LicensePayload(
            "LIC-" + System.currentTimeMillis(),
            customerName,
            "licensing@" + customerName.toLowerCase().replace(" ", "") + ".local",
            deploymentId,
            "ENTERPRISE",
            LocalDate.now(),
            validUntil,
            15,
            maxNamedUsers,
            List.of("RBAC", "OFFLINE_MODE", "GITLAB", "GITHUB_ACTIONS", "JENKINS", "BAMBOO", "REDIS_CACHE"),
            Map.of("issuedBy", "App Defend Licensing", "airGapped", true, "supportLevel", "24x7"),
            OffsetDateTime.now(),
            "App Defend Licensing Authority"
        );

        ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String canonicalPayload = mapper.writeValueAsString(payload);
        String signature = sign(canonicalPayload, privateKeyPem);

        System.out.println("Payload JSON:");
        System.out.println(canonicalPayload);
        System.out.println();
        System.out.println("Signature:");
        System.out.println(signature);

        if (bundlePassphrase != null && !bundlePassphrase.isBlank()) {
            EncryptedLicenseBundle bundle = encryptBundle(canonicalPayload, signature, bundlePassphrase);
            System.out.println();
            System.out.println("Encrypted Bundle JSON:");
            System.out.println(mapper.writeValueAsString(bundle));
        }
    }

    private static String sign(String payload, String privateKeyPem) throws Exception {
        String normalized = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private static EncryptedLicenseBundle encryptBundle(String canonicalPayload, String signatureBase64, String passphrase) throws Exception {
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] salt = random.generateSeed(16);
        byte[] iv = random.generateSeed(12);

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
    }
}
