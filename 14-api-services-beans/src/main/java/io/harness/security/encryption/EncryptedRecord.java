package io.harness.security.encryption;

/**
 * An interface to abstract the basic information available from an encrypted record.
 *
 * @author marklu on 2019-02-04
 */
public interface EncryptedRecord {
  String getUuid();
  String getName();
  String getPath(); // Only relevant if this is a record encrypted by Vault.
  String getEncryptionKey();
  char[] getEncryptedValue();
  String getKmsId();
  EncryptionType getEncryptionType();
}
