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
@ToString(exclude = {"authToken", "secretId", "sinkPath"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultConfigUpdateDTO extends SecretManagerConfigUpdateDTO {
  private String authToken;
  private String basePath;
  private String namespace;
  private String sinkPath;
  private boolean useVaultAgent;
  private String vaultUrl;
  private boolean isReadOnly;
  private long renewalIntervalMinutes;
  private String secretEngineName;
  private int secretEngineVersion;
  private String appRoleId;
  private String secretId;
}
