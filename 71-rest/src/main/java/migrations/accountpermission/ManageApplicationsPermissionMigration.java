package migrations.accountpermission;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;

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
public class ManageApplicationsPermissionMigration implements Migration {
  private final String DEBUG_MESSAGE = "MANAGE_APPLICATIONS: ";

  @Inject private WingsPersistence wingsPersistence;

  private void runMigration() {
    try (HIterator<UserGroup> userGroupHIterator = new HIterator<>(
             wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.accountPermissions).exists().fetch())) {
      // Adding MANAGE_APPLICATIONS permission for all user groups with APPLICATION_CREATE_DELETE permission
      while (userGroupHIterator.hasNext()) {
        UserGroup userGroup = userGroupHIterator.next();
        addPermissionToCurrentUserGroup(userGroup);
      }
    } catch (Exception e) {
      logger.error(DEBUG_MESSAGE + "Error creating query", e);
    }
  }

  private void addPermissionToCurrentUserGroup(UserGroup userGroup) {
    try {
      if (checkIfUserGroupContainsApplicationCreatePermission(userGroup)) {
        Set<PermissionAttribute.PermissionType> accountPermissions = userGroup.getAccountPermissions().getPermissions();

        accountPermissions.add(MANAGE_APPLICATIONS);

        UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
        setUnset(operations, UserGroupKeys.accountPermissions,
            AccountPermissions.builder().permissions(accountPermissions).build());
        wingsPersistence.update(userGroup, operations);
      }
    } catch (Exception e) {
      logger.error(DEBUG_MESSAGE + "Error occurred for userGroup:[{}]", userGroup.getUuid(), e);
    }
  }

  private boolean checkIfUserGroupContainsApplicationCreatePermission(UserGroup userGroup) {
    return userGroup.getAccountPermissions() != null && isNotEmpty(userGroup.getAccountPermissions().getPermissions())
        && userGroup.getAccountPermissions().getPermissions().contains(APPLICATION_CREATE_DELETE);
  }

  @Override
  public void migrate() {
    logger.info(DEBUG_MESSAGE + "Starting migration");
    try {
      runMigration();
    } catch (Exception e) {
      logger.error(DEBUG_MESSAGE + "Error occurred while migrating MANAGE_APPLICATIONS", e);
    }
    logger.info(DEBUG_MESSAGE + "Completed migration");
  }
}
