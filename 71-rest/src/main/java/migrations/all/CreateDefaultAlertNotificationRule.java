package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertNotificationRuleService;

import java.util.List;

public class CreateDefaultAlertNotificationRule implements Migration {
  private static final Logger log = LoggerFactory.getLogger(CreateDefaultAlertNotificationRule.class);

  @Inject private AccountService accountService;
  @Inject private AlertNotificationRuleService alertNotificationRuleService;

  @Override
  public void migrate() {
    log.info("Creating default alert notification rules for all accounts.");

    try {
      List<Account> accounts = accountService.listAllAccounts();

      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (Base.GLOBAL_ACCOUNT_ID.equals(accountId)) {
          continue;
        }

        AlertNotificationRule rule = alertNotificationRuleService.createDefaultRule(accountId);
        if (null == rule) {
          log.error("No default notification rule create. accountId={}", accountId);
        }
      }
    } catch (Exception e) {
      log.error("Error creating default notification rules", e);
    }
  }
}
