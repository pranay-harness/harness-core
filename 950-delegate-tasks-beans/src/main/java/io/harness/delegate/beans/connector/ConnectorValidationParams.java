package io.harness.delegate.beans.connector;

/**
 * Marker interface for connector heartbeat data which will be sent to delegate via rest call and Validation Handler's
 * validate will be called in perpetual task. If the implementors of this interface must also implement {@link
 * io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander}.
 */
public interface ConnectorValidationParams {
  ConnectorType getConnectorType();
  String getConnectorName();
}
