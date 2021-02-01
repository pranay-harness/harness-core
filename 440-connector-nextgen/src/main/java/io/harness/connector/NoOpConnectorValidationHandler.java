package io.harness.connector;

import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.task.ConnectorValidationHandler;

import com.google.inject.Singleton;

@Singleton
// to be removed once everyone adheres to validator
public class NoOpConnectorValidationHandler implements ConnectorValidationHandler {
  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    return null;
  }
}
