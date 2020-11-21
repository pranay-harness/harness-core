package migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import software.wings.beans.Account;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertNotificationRuleService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;

@Slf4j
public class CreateDefaultAlertNotificationRule implements Migration {
  @Inject private AccountService accountService;
  @Inject private AlertNotificationRuleService alertNotificationRuleService;

  @Override
  public void migrate() {
    log.info("Creating default alert notification rules for all accounts.");

    try {
      List<Account> accounts = accountService.listAllAccounts();

      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (GLOBAL_ACCOUNT_ID.equals(accountId)) {
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
