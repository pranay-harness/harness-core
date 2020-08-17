package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import io.harness.security.encryption.AccessType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(Include.NON_NULL)
public class VaultConnectorSummaryDTO implements ConnectorConfigSummaryDTO {
  String vaultUrl;
  String secretEngineName;
  AccessType accessType;
  boolean isDefault;
  boolean isReadOnly;
  int renewalIntervalHours;
}
