package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.security.encryption.EncryptionType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class SecretManagerMetadataRequestDTO {
  @NotNull private EncryptionType encryptionType;
  private String orgIdentifier;
  private String projectIdentifier;
  @NotNull private String identifier;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "encryptionType",
      visible = true)
  @JsonSubTypes(value = { @JsonSubTypes.Type(name = "VAULT", value = VaultMetadataRequestSpecDTO.class) })
  @Valid
  @NotNull
  private SecretManagerMetadataRequestSpecDTO spec;

  @Builder
  public SecretManagerMetadataRequestDTO(EncryptionType encryptionType, String orgIdentifier, String projectIdentifier,
      String identifier, SecretManagerMetadataRequestSpecDTO spec) {
    this.encryptionType = encryptionType;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.identifier = identifier;
    this.spec = spec;
  }
}
