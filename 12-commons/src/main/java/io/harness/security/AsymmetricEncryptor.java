package io.harness.security;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class AsymmetricEncryptor {
  private PublicKey publicKey;

  public AsymmetricEncryptor() {
    try {
      final byte[] bytes = Resources.toByteArray(AsymmetricEncryptor.class.getResource("/test_public_key.base64"));
      byte[] publicKeyMaterial = Base64.decodeBase64(bytes);

      X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyMaterial);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      publicKey = kf.generatePublic(spec);
    } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException exception) {
      throw new RuntimeException("", exception);
    }
  }

  public byte[] encryptText(String msg) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
                                               NoSuchPaddingException, NoSuchAlgorithmException {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    return cipher.doFinal(msg.getBytes(Charsets.UTF_8));
  }
}