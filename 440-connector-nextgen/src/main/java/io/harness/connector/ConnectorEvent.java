/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class ConnectorEvent {
  public static final String CONNECTOR_CREATED = "ConnectorCreated";
  public static final String CONNECTOR_UPDATED = "ConnectorUpdated";
  public static final String CONNECTOR_DELETED = "ConnectorDeleted";
}
