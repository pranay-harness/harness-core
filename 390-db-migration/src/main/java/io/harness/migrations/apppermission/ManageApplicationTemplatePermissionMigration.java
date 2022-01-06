/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.apppermission;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class ManageApplicationTemplatePermissionMigration implements Migration {
  private final String DEBUG_MESSAGE = "MANAGE_APPLICATION_TEMPLATE_PERMISSION_MIGRATION: ";
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserGroupService userGroupService;

  private void runMigration() {
    try (HIterator<UserGroup> userGroupHIterator =
             new HIterator<>(wingsPersistence.createQuery(UserGroup.class).fetch())) {
      while (userGroupHIterator.hasNext()) {
        UserGroup userGroup = userGroupHIterator.next();
        try {
          userGroupService.maintainTemplatePermissions(userGroup);
          userGroupService.updatePermissions(userGroup);
        } catch (Exception e) {
          log.error("{} Migration failed for user group with id {} in account {}", DEBUG_MESSAGE, userGroup.getUuid(),
              userGroup.getAccountId(), e);
        }
      }
    } catch (Exception e) {
      log.error(DEBUG_MESSAGE + "Error creating query", e);
    }
  }

  public void migrate() {
    log.info(DEBUG_MESSAGE + "Starting migration");
    runMigration();
    log.info(DEBUG_MESSAGE + "Completed migration");
  }
}
