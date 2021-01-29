package io.harness.delegate.beans.connector;

/**
 * Marker interface for connector heartbeat dto.
 */
public interface ConnectorValidationParams {
  ConnectorType getConnectorType();
  String getConnectorName();
}
