/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.validator;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public interface ConnectionValidator<T extends ConnectorConfigDTO> {
  ConnectorValidationResult validate(
      T connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
