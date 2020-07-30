package io.harness.connector.apis.dto.appdynamicsconnector;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppDynamicsConfigSummaryDTO implements ConnectorConfigSummaryDTO {
  private String username;
  private String accountname;
  private String controllerUrl;
}
