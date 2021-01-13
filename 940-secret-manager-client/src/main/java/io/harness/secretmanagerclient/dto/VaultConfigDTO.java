package io.harness.secretmanagerclient.dto;

import static io.harness.eraro.ErrorCode.*;
import static io.harness.exception.WingsException.*;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.MalformedURLException;
import java.net.URL;
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
