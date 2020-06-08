package io.harness.cdng.artifact.bean.connector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DockerhubConnectorConfig implements ConnectorConfig {
  String identifier;
  String registryUrl;
}
