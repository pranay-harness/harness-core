package io.harness.security.encryption;

public interface EncryptionConfig {
  String getUuid();
  String getName();
  EncryptionType getEncryptionType();
  void setEncryptionType(EncryptionType encryptionType);
  boolean isDefault();
  void setDefault(boolean isDefault);
}
