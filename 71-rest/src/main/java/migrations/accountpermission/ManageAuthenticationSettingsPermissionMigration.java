package migrations.accountpermission;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_AUTHENTICATION_SETTINGS;

import com.google.common.collect.Sets;

import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;

public class ManageAuthenticationSettingsPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_AUTHENTICATION_SETTINGS);
  }
}
