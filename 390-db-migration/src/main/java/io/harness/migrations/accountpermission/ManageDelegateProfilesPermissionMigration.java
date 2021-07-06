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
