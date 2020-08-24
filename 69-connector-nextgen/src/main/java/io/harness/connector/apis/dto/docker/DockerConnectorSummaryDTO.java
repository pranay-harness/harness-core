package io.harness.connector.apis.dto.docker;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DockerConnectorSummaryDTO implements ConnectorConfigSummaryDTO {
  String dockerRegistryUrl;
}
