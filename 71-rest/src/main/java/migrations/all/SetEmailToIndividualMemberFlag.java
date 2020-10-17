package migrations.all;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserGroupService;

import java.util.Collections;
import java.util.List;

/**
 * Refer to https://harness.atlassian.net/browse/PL-1296 for context.
 */
@Slf4j
public class SetEmailToIndividualMemberFlag implements Migration {
  @Inject private UserGroupService userGroupService;
  @Inject private AccountService accountService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      logger.info("Running Migration: {}", SetEmailToIndividualMemberFlag.class.getSimpleName());

      List<Account> accounts = accountService.listAllAccounts();

      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (Account.GLOBAL_ACCOUNT_ID.equals(accountId)) {
          continue;
        }

        UserGroup userGroup = userGroupService.getDefaultUserGroup(accountId);
        if (null == userGroup) {
          logger.info("No default user group present. accountId={}", accountId);
          continue;
        }

        logger.info(
            "Setting useIndividualEmails flag to true. accountId={} userGroupId={}", accountId, userGroup.getUuid());
        NotificationSettings existing = userGroup.getNotificationSettings();
        NotificationSettings updatedSetting;

        if (null == existing) {
          updatedSetting = new NotificationSettings(true, true, Collections.emptyList(), null, "", "");
        } else {
          updatedSetting = new NotificationSettings(true, true, existing.getEmailAddresses(), existing.getSlackConfig(),
              "", existing.getMicrosoftTeamsWebhookUrl());
        }

        wingsPersistence.updateField(
            UserGroup.class, userGroup.getUuid(), UserGroup.NOTIFICATION_SETTINGS_KEY, updatedSetting);
      }

    } catch (Exception e) {
      logger.error("Error running SetEmailToIndividualMemberFlag migration", e);
    }
  }
}
