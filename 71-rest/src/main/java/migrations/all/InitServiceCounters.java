package migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class InitServiceCounters implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private AppService appService;

  @Override
  public void migrate() {
    logger.info("Initializing Service Counters");

    try {
      List<Account> accounts = accountService.listAllAccounts();
      wingsPersistence.delete(
          wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_SERVICE.toString()));

      logger.info("Total accounts fetched. Count: {}", accounts.size());
      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (GLOBAL_ACCOUNT_ID.equals(accountId)) {
          continue;
        }

        Set<String> appIds =
            appService.getAppsByAccountId(accountId).stream().map(Application::getUuid).collect(Collectors.toSet());

        long serviceCount = wingsPersistence.createQuery(Service.class).field("appId").in(appIds).count();

        Action action = new Action(accountId, ActionType.CREATE_SERVICE);

        logger.info("Initializing Counter. Account Id: {} , ServiceCount: {}", accountId, serviceCount);
        Counter counter = new Counter(action.key(), serviceCount);
        wingsPersistence.save(counter);
      }
    } catch (Exception e) {
      logger.error("Error initializing Service counters", e);
    }
  }
}
