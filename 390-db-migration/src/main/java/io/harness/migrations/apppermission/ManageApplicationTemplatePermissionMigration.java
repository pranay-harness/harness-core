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
              userGroup.getAccountId());
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
