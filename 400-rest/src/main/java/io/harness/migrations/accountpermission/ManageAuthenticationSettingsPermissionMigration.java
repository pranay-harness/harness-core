package io.harness.migrations.accountpermission;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_AUTHENTICATION_SETTINGS;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.PermissionType;

import com.google.common.collect.Sets;
import java.util.Set;

@TargetModule(Module._390_DB_MIGRATION)
public class ManageAuthenticationSettingsPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_AUTHENTICATION_SETTINGS);
  }
}
