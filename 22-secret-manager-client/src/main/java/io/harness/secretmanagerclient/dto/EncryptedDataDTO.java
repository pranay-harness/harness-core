package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;
import io.harness.security.encryption.EncryptionType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EncryptedDataDTO {
  SecretType type;
  ValueType valueType;
  String value;
  boolean draft;
  String account;
  String org;
  String project;
  String identifier;
  String secretManager;
  String secretManagerName;
  String name;
  EncryptionType encryptionType;
  List<String> tags;
  long lastUpdatedAt;
  String description;
}
