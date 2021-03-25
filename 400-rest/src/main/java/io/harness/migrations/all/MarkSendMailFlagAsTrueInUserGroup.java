package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.beans.Account;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class MarkSendMailFlagAsTrueInUserGroup implements Migration {
  @Inject private AccountService accountService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      log.info("Running Migration: {}", MarkSendMailFlagAsTrueInUserGroup.class.getSimpleName());

      List<Account> accounts = accountService.listAllAccounts();

      for (Account account : accounts) {
        List<UserGroup> userGroups = getAllUserGroups(account.getUuid());
        for (UserGroup userGroup : userGroups) {
          NotificationSettings existing = userGroup.getNotificationSettings();
          NotificationSettings updatedSetting;

          if (null == existing) {
            log.info(
                "Existing notification settings are null. Creating default notifications settings for userGroup {}",
                userGroup.getUuid());
            updatedSetting = new NotificationSettings(false, true, Collections.emptyList(), null, "", "");
          } else {
            log.info("Overriding original notification settings for userGroup {}", userGroup.getUuid());
            updatedSetting = new NotificationSettings(existing.isUseIndividualEmails(), true,
                existing.getEmailAddresses(), existing.getSlackConfig(), "", existing.getMicrosoftTeamsWebhookUrl());
          }
          wingsPersistence.updateField(
              UserGroup.class, userGroup.getUuid(), UserGroup.NOTIFICATION_SETTINGS_KEY, updatedSetting);
        }
      }

    } catch (Exception e) {
      log.error("Error running MarkSendMailFlagAsTrueInUserGroup migration", e);
    }
  }

  private List<UserGroup> getAllUserGroups(String accountId) {
    return wingsPersistence.createQuery(UserGroup.class).filter(UserGroup.ACCOUNT_ID_KEY, accountId).asList();
  }
}
