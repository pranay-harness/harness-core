package io.harness.connector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.task.ConnectorValidationHandler;

import com.google.inject.Singleton;

@Singleton
// to be removed once everyone adheres to validator
@OwnedBy(DX)
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
