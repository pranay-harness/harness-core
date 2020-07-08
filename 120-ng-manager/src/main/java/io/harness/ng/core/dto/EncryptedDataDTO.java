package io.harness.ng.core.dto;

import static software.wings.settings.SettingValue.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptionType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.settings.UsageRestrictions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
  SettingVariableTypes type;
  Set<EncryptedDataParent> parents;
  String accountId;
  boolean enabled;
  String kmsId;
  EncryptionType encryptionType;
  long fileSize;
  List<String> appIds;
  List<String> serviceIds;
  List<String> envIds;
  char[] backupEncryptedValue;
  String backupEncryptionKey;
  String backupKmsId;
  EncryptionType backupEncryptionType;
  Set<String> serviceVariableIds;
  Map<String, AtomicInteger> searchTags;
  boolean scopedToAccount;
  UsageRestrictions usageRestrictions;
  Long nextMigrationIteration;
  Long nextAwsToGcpKmsMigrationIteration;
  boolean base64Encoded;
  String encryptedBy;
  int setupUsage;
  long runTimeUsage;
  int changeLog;
  List<String> keywords;
  String uuid;
  String appId;
  String entityYamlPath;
}
