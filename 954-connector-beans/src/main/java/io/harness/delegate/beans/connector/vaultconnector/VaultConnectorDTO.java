package io.harness.delegate.beans.connector.vaultconnector;

import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.exception.WingsException.USER;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.AccessType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString(exclude = {"authToken", "secretId"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class VaultConnectorDTO extends ConnectorConfigDTO {
  private String authToken;
  private String basePath;
  private String vaultUrl;
  private boolean isReadOnly;
  private long renewalIntervalMinutes;
  private boolean secretEngineManuallyConfigured;
  private String secretEngineName;
  private String appRoleId;
  private String secretId;
  private boolean isDefault;
  private int secretEngineVersion;

  public AccessType getAccessType() {
    return hasSome(appRoleId) ? AccessType.APP_ROLE : AccessType.TOKEN;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return null;
  }

  @Override
  public void validate() {
    try {
      new URL(vaultUrl);
    } catch (MalformedURLException malformedURLException) {
      throw new InvalidRequestException("Please check the url and try again.", INVALID_REQUEST, USER);
    }
    if (secretEngineVersion <= 0) {
      throw new InvalidRequestException(
          String.format("Invalid value for secret engine version: %s", secretEngineVersion), INVALID_REQUEST, USER);
    }
    if (renewalIntervalMinutes <= 0) {
      throw new InvalidRequestException(
          String.format("Invalid value for renewal interval: %s", renewalIntervalMinutes), INVALID_REQUEST, USER);
    }
  }
}
