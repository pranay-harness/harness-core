package migrations.accountpermission;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_IP_WHITELISTING;

import com.google.common.collect.Sets;

import software.wings.security.PermissionAttribute;

import java.util.Set;

public class ManageIPWhitelistPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_IP_WHITELISTING);
  }
}
