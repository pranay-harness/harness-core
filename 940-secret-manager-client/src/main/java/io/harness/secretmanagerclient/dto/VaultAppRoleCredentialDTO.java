package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("APP_ROLE")
public class VaultAppRoleCredentialDTO extends VaultCredentialDTO {
  private String appRoleId;
  private String secretId;
}
