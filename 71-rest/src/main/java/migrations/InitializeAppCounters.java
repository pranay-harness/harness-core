package migrations;

import com.google.inject.Inject;

import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import java.util.List;

/**
 * Populate `limitCounters` collection with current value of applications an account has.
 */
public class InitializeAppCounters implements Migration {
  private static final Logger log = LoggerFactory.getLogger(InitializeAppCounters.class);

  @Inject AccountService accountService;
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Initializing Counters");
    Datastore ds = wingsPersistence.getDatastore(HPersistence.DEFAULT_STORE, ReadPref.NORMAL);

    try {
      List<Account> accounts = accountService.listAllAccounts();
      ds.delete(ds.createQuery(Counter.class));

      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (accountId.equals(Base.GLOBAL_ACCOUNT_ID)) {
          continue;
        }

        Action action = new Action(accountId, ActionType.CREATE_APPLICATION);
        long appCount =
            ds.getCount(wingsPersistence.createQuery(Application.class).field("accountId").equal(accountId));

        log.info("Initializing Counter. Account Id: {} , AppCount: {}", accountId, appCount);
        Counter counter = new Counter(action.key(), appCount);
        wingsPersistence.save(counter);
      }
    } catch (Exception e) {
      log.error("Error initializing app counters", e);
    }
  }
}
