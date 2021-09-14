/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector;

import io.harness.connector.ConnectorValidationResult;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorHeartbeatDelegateResponse {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  String name;
  ConnectorValidationResult connectorValidationResult;
}
