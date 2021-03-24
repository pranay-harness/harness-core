package io.harness.migrations.accountpermission;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;

import java.util.EnumSet;
import java.util.Set;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class ManageDelegatePermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return EnumSet.of(PermissionAttribute.PermissionType.MANAGE_DELEGATES);
  }
}
