package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;

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
import software.wings.service.intfc.UserGroupService;

import java.util.Set;

@Slf4j
public class AddCEPermissionToAllUserGroups implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserGroupService userGroupService;

  @Override
  public void migrate() {
    logger.info("Starting updating admin user groups with CE_ADMIN permission type");
    UserGroup userGroup = null;
    try (HIterator<UserGroup> userGroups =
             new HIterator<>(wingsPersistence.createQuery(UserGroup.class, excludeAuthority)
                                 .field(UserGroupKeys.accountPermissions)
                                 .exists()
                                 .fetch())) {
      while (userGroups.hasNext()) {
        try {
          userGroup = userGroups.next();
          AccountPermissions accountPermissions = userGroup.getAccountPermissions();
          Set<PermissionType> accountPermissionsSet = accountPermissions.getPermissions();
          if (isNotEmpty(accountPermissionsSet)) {
            accountPermissionsSet.add(PermissionType.CE_VIEWER);
            if (accountPermissionsSet.contains(PermissionType.ACCOUNT_MANAGEMENT)) {
              accountPermissionsSet.add(PermissionType.CE_ADMIN);
            }
            UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
            setUnset(operations, UserGroupKeys.accountPermissions,
                AccountPermissions.builder().permissions(accountPermissionsSet).build());
            wingsPersistence.update(userGroup, operations);
          }
        } catch (Exception ex) {
          logger.error("Error while updating user group {}", userGroup != null ? userGroup.getUuid() : "NA", ex);
        }
      }
    }
    logger.info("Completed updating admin user groups with CE_ADMIN permission type");
  }
}
