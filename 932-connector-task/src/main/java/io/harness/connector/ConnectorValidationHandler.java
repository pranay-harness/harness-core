package io.harness.connector;

import io.harness.delegate.beans.connector.ConnectorValidationParams;

/**
 * Validation handler is called by heartbeat during perpetual task on validate to check if connector is working fine.
 */

public interface ConnectorValidationHandler {
  ConnectorValidationResult validate(ConnectorValidationParams connectorValidationParams, String accountIdentifier);
}