package io.harness.connector.apis.dto.splunkconnector;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SplunkConnectorSummaryDTO implements ConnectorConfigSummaryDTO {
  private String username;
  private String splunkUrl;
}
