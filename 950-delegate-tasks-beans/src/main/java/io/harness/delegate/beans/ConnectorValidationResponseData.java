package io.harness.delegate.beans;

import io.harness.connector.ConnectorValidationResult;

public interface ConnectorValidationResponseData extends DelegateResponseData, ManagerResponseData {
  default ConnectorValidationResult getConnectorValidationResult() {
    return null;
  }
}
