/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.services;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorValidationResult;

public interface ConnectorValidationService {
  ConnectorValidationResult validate(ConnectorDTO connector, String accountIdentifier);

  ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}
