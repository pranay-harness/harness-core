package io.harness.delegate.beans;

import io.harness.connector.ConnectorValidationResult;

public interface ConnectorValidationResponseData {
  default ConnectorValidationResult getConnectorValidationResponse() {
    return null;
  }
}
