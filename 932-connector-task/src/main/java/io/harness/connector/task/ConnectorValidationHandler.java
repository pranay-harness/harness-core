package io.harness.connector.task;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.ConnectorValidationResponseData;
import io.harness.delegate.beans.connector.ConnectorValidationParams;

/**
 * Validation handler is called by heartbeat during perpetual task on validate to check if connector is working fine.
 */

public interface ConnectorValidationHandler {
  ConnectorValidationResult validate(ConnectorValidationParams connectorValidationParams, String accountIdentifier);

  default ConnectorValidationResponseData validateConnector(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    return null;
  }
}