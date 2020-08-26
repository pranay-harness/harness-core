package migrations.accountpermission;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_API_KEYS;

import com.google.common.collect.Sets;

import software.wings.security.PermissionAttribute;

import java.util.Set;

public class ManageApiKeyPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_API_KEYS);
  }
}
