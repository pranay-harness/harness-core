/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.accountpermission;

import software.wings.security.PermissionAttribute;

import java.util.HashSet;
import java.util.Set;

public class ManageCloudProvidersAndConnectorsPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    Set<PermissionAttribute.PermissionType> permissionTypes = new HashSet<>();
    permissionTypes.add(PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS);
    permissionTypes.add(PermissionAttribute.PermissionType.MANAGE_CONNECTORS);
    return permissionTypes;
  }
}
