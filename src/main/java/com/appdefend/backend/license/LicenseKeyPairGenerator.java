package com.appdefend.backend.license;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class LicenseKeyPairGenerator {
    private LicenseKeyPairGenerator() {
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        System.out.println("Private Key PEM:");
        System.out.println(toPem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        System.out.println();
        System.out.println("Public Key PEM:");
        System.out.println(toPem("PUBLIC KEY", pair.getPublic().getEncoded()));
    }

    private static String toPem(String type, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }
}
