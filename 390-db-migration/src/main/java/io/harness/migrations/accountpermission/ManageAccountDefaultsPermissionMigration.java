/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.accountpermission;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ACCOUNT_DEFAULTS;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute.PermissionType;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(PL)
public class ManageAccountDefaultsPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_ACCOUNT_DEFAULTS);
  }
}
