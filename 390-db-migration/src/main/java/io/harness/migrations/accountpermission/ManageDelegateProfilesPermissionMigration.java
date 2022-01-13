/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.accountpermission;

import software.wings.security.PermissionAttribute;

import java.util.EnumSet;
import java.util.Set;

public class ManageDelegateProfilesPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return EnumSet.of(PermissionAttribute.PermissionType.MANAGE_DELEGATE_PROFILES);
  }
}
