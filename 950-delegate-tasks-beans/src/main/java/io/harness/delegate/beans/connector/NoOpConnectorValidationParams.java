package io.harness.delegate.beans.connector;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NoOpConnectorValidationParams implements ConnectorValidationParams {
  @Override
  public ConnectorType getConnectorType() {
    // sending random connector;
    return ConnectorType.KUBERNETES_CLUSTER;
  }

  @Override
  public String getConnectorName() {
    return null;
  }
}
