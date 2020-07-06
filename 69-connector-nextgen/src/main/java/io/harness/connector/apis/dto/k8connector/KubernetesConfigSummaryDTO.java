package io.harness.connector.apis.dto.k8connector;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KubernetesConfigSummaryDTO implements ConnectorConfigSummaryDTO {
  String delegateName;
  String masterURL;
}
