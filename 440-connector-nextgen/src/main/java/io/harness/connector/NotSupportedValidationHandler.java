package io.harness.connector;

import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.task.ConnectorValidationHandler;

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