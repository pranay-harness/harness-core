package io.harness.batch.processing.metrics;

import io.harness.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorType;

import lombok.Value;

@Value
public class CEConnectorsTelemetry {
  ConnectorType connectorType;
  ConnectivityStatus status;
}
