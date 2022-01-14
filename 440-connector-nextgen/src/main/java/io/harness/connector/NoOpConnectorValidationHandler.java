package io.harness.connector;

import io.harness.connector.task.ConnectorValidationHandler;
import io.harness.delegate.beans.connector.ConnectorValidationParams;

import com.google.inject.Singleton;

@Singleton
// to be removed once everyone adheres to validator
public class NoOpConnectorValidationHandler implements ConnectorValidationHandler {
  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(System.currentTimeMillis())
        .build();
  }
}
