package io.harness.security.encryption;

import lombok.Builder;
import lombok.Data;

/*
 * Encrypted record data implements EncryptedRecord for serialization purposes between the manager and the delegate.
 */

// TODO: I cannot use value here because of mokito not liking final classes. Find a solution that will eliminate this.
@Data
@Builder
public class EncryptedRecordData implements EncryptedRecord {
  private String uuid;
  private String name;
  private String path;
  private String encryptionKey;
  private char[] encryptedValue;
  private String kmsId;
  private EncryptionType encryptionType;
}
