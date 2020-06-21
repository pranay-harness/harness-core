package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.security.PermissionAttribute.PermissionType.AUDIT_VIEWER;

import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.Morphia;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class AuditViewerPermissionMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;
  @Inject private Morphia morphia;

  @Override
  @SuppressWarnings("deprecation")
  public void migrate() {
    logger.info("Running the AuditViewerPermissionMigration script.");

    // 1. create AccountPermission to be added when its missing from existing userGroup
    Set<PermissionType> newAccPermissions = new HashSet<>();
    newAccPermissions.add(AUDIT_VIEWER);
    AccountPermissions newAccountPermissions = AccountPermissions.builder().permissions(newAccPermissions).build();

    // 2. Init bulkWriteOperation
    final DBCollection collection = wingsPersistence.getCollection(UserGroup.class);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int count = 1;
    try (HIterator<UserGroup> userGroupIterator =
             new HIterator<>(wingsPersistence.createQuery(UserGroup.class, excludeAuthority).fetch())) {
      while (userGroupIterator.hasNext()) {
        UserGroup userGroup = userGroupIterator.next();
        // if AccPermissions are not present, assign accLevelPermission created above with only AUDIT_VIEWER permission
        AccountPermissions accountLevelPermissions =
            userGroup.getAccountPermissions() == null ? newAccountPermissions : userGroup.getAccountPermissions();

        // userGroup had accountLevelPermission but permission list was empty. Assign permissionList having only
        // AUDIT_VIEWER permission
        if (isEmpty(accountLevelPermissions.getPermissions())) {
          accountLevelPermissions.setPermissions(newAccPermissions);
        }

        Set<PermissionType> permissions = accountLevelPermissions.getPermissions();
        permissions.add(PermissionType.AUDIT_VIEWER);
        if (count % 1000 == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          logger.info("UserGroups: {} updated", count);
        }
        ++count;

        DBObject dbObject = morphia.toDBObject(accountLevelPermissions);
        dbObject.removeField("className");
        bulkWriteOperation
            .find(wingsPersistence.createQuery(UserGroup.class).filter("uuid", userGroup.getUuid()).getQueryObject())
            .updateOne(new BasicDBObject("$set", new BasicDBObject(UserGroupKeys.accountPermissions, dbObject)));
      }
    } catch (Exception ex) {
      logger.error("AuditViewerPermissionMigration failed: ", ex);
    }

    if (count % 1000 != 1) {
      try {
        bulkWriteOperation.execute();
      } catch (Exception ex) {
        logger.error("AuditViewerPermissionMigration failed: ", ex);
      }
    }
  }
}
