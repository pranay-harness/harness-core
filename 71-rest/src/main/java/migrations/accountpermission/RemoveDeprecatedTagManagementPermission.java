package migrations.accountpermission;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;
import static software.wings.security.PermissionAttribute.PermissionType.TAG_MANAGEMENT;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute;

import java.util.Set;

@Slf4j
public class RemoveDeprecatedTagManagementPermission implements Migration {
  private static final String debugMessage = "REMOVE_TAG_MANAGEMENT: ";

  @Inject private WingsPersistence wingsPersistence;

  private void runMigration() {
    try (HIterator<UserGroup> userGroupHIterator = new HIterator<>(
             wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.accountPermissions).exists().fetch())) {
      // Removing deprecated TAG_MANAGEMENT permission from user groups
      while (userGroupHIterator.hasNext()) {
        UserGroup userGroup = userGroupHIterator.next();
        migrateCurrentUserGroup(userGroup);
      }
    } catch (Exception e) {
      logger.error(debugMessage + "Error creating query", e);
    }
  }

  private void migrateCurrentUserGroup(UserGroup userGroup) {
    try {
      if (checkIfUserGroupContainsTagManagementPermission(userGroup)) {
        Set<PermissionAttribute.PermissionType> accountPermissions = userGroup.getAccountPermissions().getPermissions();

        accountPermissions.remove(TAG_MANAGEMENT);

        UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
        setUnset(operations, UserGroupKeys.accountPermissions,
            AccountPermissions.builder().permissions(accountPermissions).build());
        wingsPersistence.update(userGroup, operations);
      }
    } catch (Exception e) {
      logger.error(debugMessage + "Error occurred for userGroup:[{}]", userGroup.getUuid(), e);
    }
  }

  private boolean checkIfUserGroupContainsTagManagementPermission(UserGroup userGroup) {
    return userGroup.getAccountPermissions() != null && isNotEmpty(userGroup.getAccountPermissions().getPermissions())
        && userGroup.getAccountPermissions().getPermissions().contains(TAG_MANAGEMENT);
  }

  @Override
  public void migrate() {
    logger.info(debugMessage + "Starting Migration");
    try {
      runMigration();
    } catch (Exception e) {
      logger.error(debugMessage + "Error occurred while migrating", e);
    }
    logger.info(debugMessage + "Completed migration");
  }
}
