/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRETS;

import io.harness.migrations.accountpermission.AbstractAccountManagementPermissionMigration;

import software.wings.security.PermissionAttribute.PermissionType;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManageSecretsMigration extends AbstractAccountManagementPermissionMigration {
  public Set<PermissionType> getToBeAddedPermissions() {
    Set<PermissionType> toBeAddedPermissions = new HashSet<>();
    toBeAddedPermissions.add(MANAGE_SECRETS);
    return toBeAddedPermissions;
  }
}
