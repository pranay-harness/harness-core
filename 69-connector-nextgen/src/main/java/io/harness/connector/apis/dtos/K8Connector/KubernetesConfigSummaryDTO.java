package io.harness.connector.apis.dtos.K8Connector;

import io.harness.connector.apis.dtos.connector.ConnectorConfigSummaryDTO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KubernetesConfigSummaryDTO implements ConnectorConfigSummaryDTO {
  String delegateName;
  String masterURL;
}
