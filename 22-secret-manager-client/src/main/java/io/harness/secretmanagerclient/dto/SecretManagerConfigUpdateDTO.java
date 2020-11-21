package io.harness.secretmanagerclient.dto;

import io.harness.security.encryption.EncryptionType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "encryptionType",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(name = "VAULT", value = VaultConfigUpdateDTO.class)
  , @JsonSubTypes.Type(name = "GCP_KMS", value = GcpKmsConfigUpdateDTO.class)
})
public class SecretManagerConfigUpdateDTO {
  private List<String> tags;
  private EncryptionType encryptionType;
  private boolean isDefault;
  private String description;
}
