package io.harness.migrations.accountpermission;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_API_KEYS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;

import com.google.common.collect.Sets;
import java.util.Set;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class ManageApiKeyPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_API_KEYS);
  }
}
