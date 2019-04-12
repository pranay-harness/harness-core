package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SetEmailToIndividualMemberFlag implements Migration {
  private static final Logger log = LoggerFactory.getLogger(SetEmailToIndividualMemberFlag.class);

  @Inject private UserGroupService userGroupService;
  @Inject private AccountService accountService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      List<Account> accounts = accountService.listAllAccounts();

      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (Account.GLOBAL_ACCOUNT_ID.equals(accountId)) {
          continue;
        }

        UserGroup userGroup = userGroupService.getDefaultUserGroup(accountId);
        if (null == userGroup) {
          continue;
        }

        log.info(
            "Setting useIndividualEmails flag to true. accountId={} userGroupId={}", accountId, userGroup.getUuid());
        NotificationSettings existing = userGroup.getNotificationSettings();
        if (null == existing) {
          userGroup.setNotificationSettings(new NotificationSettings(true, Collections.emptyList(), null));
        } else {
          userGroup.setNotificationSettings(
              new NotificationSettings(true, existing.getEmailAddresses(), existing.getSlackConfig()));
        }

        wingsPersistence.save(userGroup);
      }
    } catch (Exception e) {
      log.error("Error running SetEmailToIndividualMemberFlag migration", e);
    }
  }
}
