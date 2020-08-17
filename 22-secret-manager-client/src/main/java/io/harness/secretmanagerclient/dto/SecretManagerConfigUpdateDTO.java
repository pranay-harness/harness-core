package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
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
@JsonSubTypes({ @JsonSubTypes.Type(name = "VAULT", value = VaultConfigUpdateDTO.class) })
public class SecretManagerConfigUpdateDTO {
  private List<String> tags;
  private EncryptionType encryptionType;
  private boolean isDefault;
  private String description;
}
