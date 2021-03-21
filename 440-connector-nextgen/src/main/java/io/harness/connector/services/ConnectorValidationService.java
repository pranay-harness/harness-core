package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorValidationResult;

@OwnedBy(DX)
public interface ConnectorValidationService {
  ConnectorValidationResult validate(ConnectorDTO connector, String accountIdentifier);

  ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}
