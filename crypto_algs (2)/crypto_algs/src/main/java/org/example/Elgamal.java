package org.example;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;

public class Elgamal {
    private static final String PROVIDER = "BC";
    private SecureRandom random = null;
    private KeyPair keypair = null;
    private Cipher xCipher = null;
    private Cipher sCipher = null;
    private IvParameterSpec sIvSpec = null;
    private Key sKey = null;
    private byte[] keyBlock = null;

    public Elgamal() {
        random = new SecureRandom();
        keypair = createKeyPair();

        // Get instances
        xCipher = getCipherInstance("ElGamal/None/PKCS1Padding");
        sCipher = getCipherInstance("AES/CTR/NoPadding");
    }

    // Provider
    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    private KeyPair createKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("ELGamal", PROVIDER);
            generator.initialize(512, random);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            System.out.println(e.toString());
            return null;
        }
    }
    private Cipher getCipherInstance(final String cipherInstance) {
        try {
            return Cipher.getInstance(cipherInstance, PROVIDER);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e) {
            System.out.println(e.toString());
            return null;
        }
    }
    public byte[] encrypt(byte[] in) {
        try {
            sKey = createKeyForAES(256, random);
            sIvSpec = createCtrIvForAES(0, random);
            xCipher.init(Cipher.ENCRYPT_MODE, keypair.getPublic(), random);
            keyBlock = xCipher.doFinal(packKeyAndIv(sKey, sIvSpec));

            // Encryption step
            sCipher.init(Cipher.ENCRYPT_MODE, sKey, sIvSpec);

            return sCipher.doFinal(in);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public byte[] decrypt(byte[] inputBytes) {
        try {
            // Symmetric key/iv unwrapping step
            xCipher.init(Cipher.DECRYPT_MODE, keypair.getPrivate());
            Object[] keyIv = unpackKeyAndIV(xCipher.doFinal(keyBlock));

            // Decryption step
            sCipher.init(Cipher.DECRYPT_MODE, (Key) keyIv[0], (IvParameterSpec) keyIv[1]);
            return sCipher.doFinal(inputBytes);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static SecretKey createKeyForAES(int bitLength, SecureRandom random) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES", PROVIDER);
            generator.init(256, random);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            System.out.println(e.toString());
            return null;
        }
    }
    public static IvParameterSpec createCtrIvForAES(int messageNumber, SecureRandom random) {
        byte[] ivBytes = new byte[16];
        // initially randomize
        random.nextBytes(ivBytes);
        // set the message number bytes
        ivBytes[0] = (byte) (messageNumber >> 24);
        ivBytes[1] = (byte) (messageNumber >> 16);
        ivBytes[2] = (byte) (messageNumber >> 8);
        ivBytes[3] = (byte) (messageNumber >> 0);
        // set the counter bytes to 1
        for (int i = 0; i != 7; i++) {
            ivBytes[8 + i] = 0;
        }
        ivBytes[15] = 1;
        return new IvParameterSpec(ivBytes);
    }
    private static byte[] packKeyAndIv(Key key, IvParameterSpec ivSpec) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        bOut.write(ivSpec.getIV());
        bOut.write(key.getEncoded());
        return bOut.toByteArray();
    }
    private static Object[] unpackKeyAndIV(byte[] data) {
        return new Object[]{new SecretKeySpec(data, 16, data.length - 16, "AES"), new IvParameterSpec(data, 0, 16)};
    }

}