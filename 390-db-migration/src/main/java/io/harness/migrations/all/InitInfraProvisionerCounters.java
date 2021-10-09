package io.harness.migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.migrations.Migration;

import software.wings.beans.Account;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InitInfraProvisionerCounters implements Migration {
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

        long infraProvisionerCount = wingsPersistence.createQuery(InfrastructureProvisioner.class)
                                         .field(InfrastructureProvisioner.ACCOUNT_ID_KEY2)
                                         .equal(accountId)
                                         .count();

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
