package io.harness.connector.apis.dto.gitconnector;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitConfigSummaryDTO implements ConnectorConfigSummaryDTO {
  private String url;
}
