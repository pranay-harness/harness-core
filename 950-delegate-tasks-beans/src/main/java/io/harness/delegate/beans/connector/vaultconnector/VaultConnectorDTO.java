package io.harness.delegate.beans.connector.vaultconnector;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.AccessType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collections;
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
public class VaultConnectorDTO extends ConnectorConfigDTO implements ExecutionCapabilityDemander {
  private String authToken;
  private String basePath;
  private String vaultUrl;
  private boolean isReadOnly;
  private int renewIntervalHours;
  private String secretEngineName;
  private String appRoleId;
  private String secretId;
  private boolean isDefault;
  private int secretEngineVersion;

  @JsonIgnore
  public AccessType getAccessType() {
    return isNotEmpty(appRoleId) ? AccessType.APP_ROLE : AccessType.TOKEN;
  }

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return null;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.emptyList();
  }
}
