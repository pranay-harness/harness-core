package io.harness.connector.mappers.gitconnectormapper.secretmanagermapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocalConnectorSummaryDTO implements ConnectorConfigSummaryDTO {
  boolean isDefault;
}
