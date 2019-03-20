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
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InitWorkflowCounters implements Migration {
  private static final Logger log = LoggerFactory.getLogger(InitWorkflowCounters.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private AppService appService;

  @Override
  public void migrate() {
    log.info("Initializing Workflow Counters");

    try {
      List<Account> accounts = accountService.listAllAccounts();
      wingsPersistence.delete(
          wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_WORKFLOW.toString()));

      log.info("Total accounts fetched. Count: {}", accounts.size());
      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (GLOBAL_ACCOUNT_ID.equals(accountId)) {
          continue;
        }

        Set<String> appIds =
            appService.getAppsByAccountId(accountId).stream().map(Application::getUuid).collect(Collectors.toSet());

        long workflowCount = wingsPersistence.createQuery(Workflow.class).field("appId").in(appIds).count();

        Action action = new Action(accountId, ActionType.CREATE_WORKFLOW);

        log.info("Initializing Counter. Account Id: {} , WorkflowCount: {}", accountId, workflowCount);
        Counter counter = new Counter(action.key(), workflowCount);
        wingsPersistence.save(counter);
      }
    } catch (Exception e) {
      log.error("Error initializing Workflow counters", e);
    }
  }
}
