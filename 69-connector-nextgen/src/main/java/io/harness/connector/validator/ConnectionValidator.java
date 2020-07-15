package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

public interface ConnectionValidator<T extends ConnectorConfigDTO> {
  ConnectorValidationResult validate(T connectorDTO, String accountId);
}
