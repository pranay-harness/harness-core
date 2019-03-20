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
import software.wings.beans.Application;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InitInfraProvisionerCounters implements Migration {
  private static final Logger log = LoggerFactory.getLogger(InitInfraProvisionerCounters.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private AppService appService;

  @Override
  public void migrate() {
    log.info("Initializing Infrastructure Provisioner Counters");

    try {
      wingsPersistence.delete(wingsPersistence.createQuery(Counter.class)
                                  .field("key")
                                  .endsWith(ActionType.CREATE_INFRA_PROVISIONER.toString()));

      List<Account> accounts = accountService.listAllAccounts();

      log.info("Total accounts fetched. Count: {}", accounts.size());
      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (GLOBAL_ACCOUNT_ID.equals(accountId)) {
          continue;
        }

        Set<String> appIds =
            appService.getAppsByAccountId(accountId).stream().map(Application::getUuid).collect(Collectors.toSet());

        long infraProvisionerCount =
            wingsPersistence.createQuery(InfrastructureProvisioner.class).field("appId").in(appIds).count();

        Action action = new Action(accountId, ActionType.CREATE_INFRA_PROVISIONER);

        log.info("Initializing Counter. Account Id: {} , Infrastructure Provisioner Count: {}", accountId,
            infraProvisionerCount);
        Counter counter = new Counter(action.key(), infraProvisionerCount);
        wingsPersistence.save(counter);
      }
    } catch (Exception e) {
      log.error("Error initializing Infrastructure Provisioner counters", e);
    }
  }
}
