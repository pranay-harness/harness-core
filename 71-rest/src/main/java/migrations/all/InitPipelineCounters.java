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
import software.wings.beans.Pipeline;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InitPipelineCounters implements Migration {
  private static final Logger log = LoggerFactory.getLogger(InitPipelineCounters.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private AppService appService;

  @Override
  public void migrate() {
    log.info("Initializing Pipeline Counters");

    try {
      List<Account> accounts = accountService.listAllAccounts();
      wingsPersistence.delete(
          wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_PIPELINE.toString()));

      log.info("Total accounts fetched. Count: {}", accounts.size());
      for (Account account : accounts) {
        String accountId = account.getUuid();
        if (GLOBAL_ACCOUNT_ID.equals(accountId)) {
          continue;
        }

        Set<String> appIds =
            appService.getAppsByAccountId(accountId).stream().map(Application::getUuid).collect(Collectors.toSet());

        long pipelineCount = wingsPersistence.createQuery(Pipeline.class).field("appId").in(appIds).count();

        Action action = new Action(accountId, ActionType.CREATE_PIPELINE);

        log.info("Initializing Counter. Account Id: {} , PipelineCount: {}", accountId, pipelineCount);
        Counter counter = new Counter(action.key(), pipelineCount);
        wingsPersistence.save(counter);
      }
    } catch (Exception e) {
      log.error("Error initializing Pipeline counters", e);
    }
  }
}
