package migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.util.List;

public class InitUserCounters implements Migration {
  private static final Logger log = LoggerFactory.getLogger(InitUserCounters.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private UserService userService;

  @Override
  public void migrate() {
    log.info("Initializing User Counters");

    try {
      List<Account> accounts = accountService.listAllAccounts();
      wingsPersistence.delete(
          wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_USER.toString()));

      log.info("Total accounts fetched. Count: {}", accounts.size());
      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (accountId.equals(GLOBAL_ACCOUNT_ID)) {
          continue;
        }

        List<User> users = userService.getUsersOfAccount(accountId);
        Action action = new Action(accountId, ActionType.CREATE_USER);
        long userCount = users.size();

        log.info("Initializing Counter. Account Id: {} , UserCount: {}", accountId, userCount);
        Counter counter = new Counter(action.key(), userCount);
        wingsPersistence.save(counter);
      }
    } catch (Exception e) {
      log.error("Error initializing User counters", e);
    }
  }
}
