package io.harness.secretmanagerclient.dto;

import io.harness.security.encryption.AccessType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("VAULT")
public class VaultMetadataRequestSpecDTO extends SecretManagerMetadataRequestSpecDTO {
  @NotNull private String url;
  @NotNull private AccessType accessType;
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "accessType", visible = true)
  @JsonSubTypes(value =
      {
        @JsonSubTypes.Type(name = "APP_ROLE", value = VaultAppRoleCredentialDTO.class)
        , @JsonSubTypes.Type(name = "TOKEN", value = VaultAuthTokenCredentialDTO.class)
      })
  @Valid
  private VaultCredentialDTO spec;

  @Builder
  public VaultMetadataRequestSpecDTO(String url, AccessType accessType, VaultCredentialDTO spec) {
    this.url = url;
    this.accessType = accessType;
    this.spec = spec;
  }
}
