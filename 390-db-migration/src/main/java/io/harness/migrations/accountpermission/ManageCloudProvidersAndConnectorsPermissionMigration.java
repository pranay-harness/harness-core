/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
