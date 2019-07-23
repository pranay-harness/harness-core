package io.harness.security;

import javax.crypto.SecretKey;

/**
 * Created by mike@ on 4/24/17.
 */
public interface EncryptionInterface {
  SecretKey getSecretKey();

  byte[] encrypt(byte[] content);

  byte[] decrypt(byte[] encrypted);
}
