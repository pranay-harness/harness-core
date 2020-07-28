package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptionType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.wings.settings.SettingVariableTypes;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedDataDTO {
  String name;
  String path;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  String secretManagerIdentifier;
  String secretManagerName;
  String secretManagerId;
  EncryptionType encryptionType;
  SettingVariableTypes type;
  long fileSize;
  String id;
  List<String> tags;
  private long lastUpdatedAt;
  private String description;
}
