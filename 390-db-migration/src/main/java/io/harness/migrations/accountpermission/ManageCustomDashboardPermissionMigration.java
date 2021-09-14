/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.accountpermission;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CUSTOM_DASHBOARDS;

import software.wings.security.PermissionAttribute;

import com.google.common.collect.Sets;
import java.util.Set;

public class ManageCustomDashboardPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_CUSTOM_DASHBOARDS);
  }
}
