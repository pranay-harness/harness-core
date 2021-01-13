package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class VaultConfigDTO extends SecretManagerConfigDTO {
  private String authToken;
  private String basePath;
  private String vaultUrl;
  @JsonProperty("readOnly") private boolean isReadOnly;
  private long renewalIntervalMinutes;
  private String secretEngineName;
  private String appRoleId;
  private String secretId;
  private int secretEngineVersion;
}
