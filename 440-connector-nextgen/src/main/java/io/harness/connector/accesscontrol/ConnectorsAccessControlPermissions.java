/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class ConnectorsAccessControlPermissions {
  public static final String VIEW_CONNECTOR_PERMISSION = "core_connector_view";
  public static final String EDIT_CONNECTOR_PERMISSION = "core_connector_edit";
  public static final String DELETE_CONNECTOR_PERMISSION = "core_connector_delete";
  public static final String ACCESS_CONNECTOR_PERMISSION = "core_connector_access";
}
