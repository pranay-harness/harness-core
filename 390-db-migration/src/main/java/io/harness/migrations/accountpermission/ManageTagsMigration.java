/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.accountpermission;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_TAGS;
import static software.wings.security.PermissionAttribute.PermissionType.TAG_MANAGEMENT;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute;

import com.google.inject.Inject;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class ManageTagsMigration implements Migration {
  private final String DEBUG_MESSAGE = "MANAGE_TAGS_MIGRATION: ";

  @Inject private WingsPersistence wingsPersistence;

  private void runMigration() {
    try (HIterator<UserGroup> userGroupHIterator = new HIterator<>(
             wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.accountPermissions).exists().fetch())) {
      // Adding MANAGE_TAGS permission for all user groups with TAG_MANAGEMENT permission
      while (userGroupHIterator.hasNext()) {
        UserGroup userGroup = userGroupHIterator.next();
        addPermissionToCurrentUserGroup(userGroup);
      }
    } catch (Exception e) {
      log.error(DEBUG_MESSAGE + "Error creating query", e);
    }
  }

  private void addPermissionToCurrentUserGroup(UserGroup userGroup) {
    try {
      if (checkIfUserGroupContainsTagManagementPermission(userGroup)) {
        Set<PermissionAttribute.PermissionType> accountPermissions = userGroup.getAccountPermissions().getPermissions();

        accountPermissions.add(MANAGE_TAGS);

        UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
        setUnset(operations, UserGroupKeys.accountPermissions,
            AccountPermissions.builder().permissions(accountPermissions).build());
        wingsPersistence.update(userGroup, operations);
      }
    } catch (Exception e) {
      log.error(DEBUG_MESSAGE + "Error occurred for userGroup:[{}]", userGroup.getUuid(), e);
    }
  }

  private boolean checkIfUserGroupContainsTagManagementPermission(UserGroup userGroup) {
    return userGroup.getAccountPermissions() != null && isNotEmpty(userGroup.getAccountPermissions().getPermissions())
        && userGroup.getAccountPermissions().getPermissions().contains(TAG_MANAGEMENT);
  }

  @Override
  public void migrate() {
    log.info(DEBUG_MESSAGE + "Starting migration");
    try {
      runMigration();
    } catch (Exception e) {
      log.error(DEBUG_MESSAGE + "Error occurred while migrating MANAGE_TAGS", e);
    }
    log.info(DEBUG_MESSAGE + "Completed migration");
  }
}
