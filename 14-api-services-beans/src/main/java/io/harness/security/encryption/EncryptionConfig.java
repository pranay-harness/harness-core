package io.harness.security.encryption;

public interface EncryptionConfig {
  String getUuid();
  EncryptionType getEncryptionType();
  boolean isDefault();
}
