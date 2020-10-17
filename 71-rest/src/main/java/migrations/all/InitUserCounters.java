package migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.util.List;

@OwnedBy(PL)
@Slf4j
public class InitUserCounters implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private UserService userService;

  @Override
  public void migrate() {
    logger.info("Initializing User Counters");

    try {
      List<Account> accounts = accountService.listAllAccounts();
      wingsPersistence.delete(
          wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_USER.toString()));

      logger.info("Total accounts fetched. Count: {}", accounts.size());
      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (accountId.equals(GLOBAL_ACCOUNT_ID)) {
          continue;
        }

        List<User> users = userService.getUsersOfAccount(accountId);
        Action action = new Action(accountId, ActionType.CREATE_USER);
        long userCount = users.size();

        logger.info("Initializing Counter. Account Id: {} , UserCount: {}", accountId, userCount);
        Counter counter = new Counter(action.key(), userCount);
        wingsPersistence.save(counter);
      }
    } catch (Exception e) {
      logger.error("Error initializing User counters", e);
    }
  }
}
