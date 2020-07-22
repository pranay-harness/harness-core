package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(exclude = {"authToken", "secretId"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NGVaultConfigDTO extends NGSecretManagerConfigDTO {
  private String authToken;
  private String basePath;
  private String vaultUrl;
  private String name;
  private boolean isReadOnly;
  private int renewIntervalHours;
  private String secretEngineName;
  private String appRoleId;
  private String secretId;
}
