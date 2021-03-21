package io.harness.connector.validator;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

@OwnedBy(DX)
public interface ConnectionValidator<T extends ConnectorConfigDTO> {
  ConnectorValidationResult validate(
      T connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
