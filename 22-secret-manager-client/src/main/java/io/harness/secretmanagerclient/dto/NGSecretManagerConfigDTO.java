package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.security.encryption.EncryptionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "encryptionType",
    visible = true)
@JsonSubTypes({
  @Type(name = "LOCAL", value = NGLocalConfigDTO.class), @Type(name = "VAULT", value = NGVaultConfigDTO.class),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class NGSecretManagerConfigDTO {
  private String uuid;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private List<String> tags;
  private String identifier;
  private EncryptionType encryptionType;
  private boolean isDefault;
}
