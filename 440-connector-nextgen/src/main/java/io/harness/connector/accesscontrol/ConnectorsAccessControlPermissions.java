/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
