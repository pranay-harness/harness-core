package migrations.accountpermission;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;

@Slf4j
public abstract class AbstractAccountManagementPermissionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  private void runMigration() {
    try (HIterator<UserGroup> userGroupHIterator = new HIterator<>(
             wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.accountPermissions).exists().fetch())) {
      while (userGroupHIterator.hasNext()) {
        UserGroup userGroup = userGroupHIterator.next();
        if (checkIfUserGroupContainsAccountManagementPermission(userGroup)) {
          Set<PermissionType> accountPermissions = userGroup.getAccountPermissions().getPermissions();
          accountPermissions.addAll(getToBeAddedPermissions());

          UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
          setUnset(operations, UserGroupKeys.accountPermissions,
              AccountPermissions.builder().permissions(accountPermissions).build());
          wingsPersistence.update(userGroup, operations);
        }
      }
    }
  }

  private boolean checkIfUserGroupContainsAccountManagementPermission(UserGroup userGroup) {
    return userGroup.getAccountPermissions() != null && isNotEmpty(userGroup.getAccountPermissions().getPermissions())
        && userGroup.getAccountPermissions().getPermissions().contains(ACCOUNT_MANAGEMENT);
  }

  public abstract Set<PermissionType> getToBeAddedPermissions();

  @Override
  public void migrate() {
    try {
      runMigration();
    } catch (Exception e) {
      logger.error("Migration: Error on running migration", e);
    }
  }
}
