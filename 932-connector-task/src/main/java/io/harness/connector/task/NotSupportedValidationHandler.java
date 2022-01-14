package io.harness.connector.task;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;

import com.google.inject.Singleton;

@Singleton
public class NotSupportedValidationHandler implements ConnectorValidationHandler {
  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.FAILURE)
        .errorSummary("Connector does not support validation through manager")
        .testedAt(System.currentTimeMillis())
        .build();
  }
}