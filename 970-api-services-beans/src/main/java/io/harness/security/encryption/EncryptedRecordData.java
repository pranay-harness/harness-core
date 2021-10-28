package io.harness.security.encryption;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/*
 * Encrypted record data implements EncryptedRecord for serialization purposes between the manager and the delegate.
 */

// TODO: I cannot use value here because of mokito not liking final classes. Find a solution that will eliminate this.
@OwnedBy(PL)
@Data
@Builder
@ToString(exclude = {"encryptionKey", "encryptedValue", "backupEncryptionKey", "backupEncryptedValue"})
@TargetModule(HarnessModule._980_COMMONS)
public class EncryptedRecordData implements EncryptedRecord {
  private String uuid;
  private String name;
  private String path;
  private Set<EncryptedDataParams> parameters;
  private String encryptionKey;
  private char[] encryptedValue;
  private String kmsId;
  private EncryptionType encryptionType;
  private char[] backupEncryptedValue;
  private String backupEncryptionKey;
  private String backupKmsId;
  private EncryptionType backupEncryptionType;
  private boolean base64Encoded;
  private AdditionalMetadata additionalMetadata;
  private EncryptedMech encryptedMech;
}
