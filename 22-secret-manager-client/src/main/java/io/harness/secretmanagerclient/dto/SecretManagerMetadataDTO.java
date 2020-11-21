package io.harness.secretmanagerclient.dto;

import io.harness.security.encryption.EncryptionType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SecretManagerMetadataDTO {
  private EncryptionType encryptionType;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "encryptionType",
      visible = true)
  @JsonSubTypes(value = { @JsonSubTypes.Type(name = "VAULT", value = VaultMetadataSpecDTO.class) })
  private SecretManagerMetadataSpecDTO spec;

  @Builder
  public SecretManagerMetadataDTO(EncryptionType encryptionType, SecretManagerMetadataSpecDTO spec) {
    this.encryptionType = encryptionType;
    this.spec = spec;
  }
}
