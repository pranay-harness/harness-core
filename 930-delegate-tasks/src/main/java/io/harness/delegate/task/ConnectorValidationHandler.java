/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;

/**
 * Validation handler is called by heartbeat during perpetual task on validate to check if connector is working fine.
 */

public interface ConnectorValidationHandler {
  ConnectorValidationResult validate(ConnectorValidationParams connectorValidationParams, String accountIdentifier);
}
