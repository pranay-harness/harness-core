package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.security.encryption.EncryptionType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SecretManagerConfigDTO {
  String uuid;
  EncryptionType encryptionType;
  boolean isDefault;
  String accountId;
  int numOfEncryptedValue;
  String encryptedBy;
  Long nextTokenRenewIteration;
  List<String> templatizedFields;
}
