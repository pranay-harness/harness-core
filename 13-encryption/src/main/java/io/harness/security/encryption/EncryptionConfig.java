package io.harness.security.encryption;

/**
 * Created by rsingh on 11/3/17.
 */
public interface EncryptionConfig {
  String getUuid();
  EncryptionType getEncryptionType();
  boolean isDefault();
}
