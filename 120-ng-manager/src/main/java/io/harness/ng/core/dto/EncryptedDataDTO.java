package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptionType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedDataDTO {
  String name;
  String encryptionKey;
  char[] encryptedValue;
  String path;
  Set<EncryptedDataParams> parameters;
  String accountId;
  boolean enabled;
  String kmsId;
  EncryptionType encryptionType;
  long fileSize;
  char[] backupEncryptedValue;
  String backupEncryptionKey;
  String backupKmsId;
  EncryptionType backupEncryptionType;
  boolean scopedToAccount;
  boolean base64Encoded;
  String uuid;
  String entityYamlPath;
}
