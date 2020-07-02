package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.event.usagemetrics.UsageMetricsService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;

@Slf4j
public class UsageMetricsHandler implements Handler<Account> {
  @Inject private UsageMetricsService usageMetricsService;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<Account> persistenceProvider;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("UsageMetricsHandler").poolSize(2).interval(ofSeconds(30)).build(),
        UsageMetricsHandler.class,
        MongoPersistenceIterator.<Account, MorphiaFilterExpander<Account>>builder()
            .clazz(Account.class)
            .fieldName(AccountKeys.usageMetricsTaskIteration)
            .targetInterval(ofHours(24))
            .acceptableNoAlertDelay(ofMinutes(30))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Account account) {
    if ((account.getLicenseInfo() == null
            || ((account.getLicenseInfo() != null && account.getLicenseInfo().getAccountStatus() != null)
                   && (account.getLicenseInfo().getAccountStatus().equals(AccountStatus.ACTIVE)
                          && (account.getLicenseInfo().getAccountType().equals(AccountType.TRIAL)
                                 || account.getLicenseInfo().getAccountType().equals(AccountType.PAID)))))
        && (!account.getUuid().equals(GLOBAL_ACCOUNT_ID))) {
      usageMetricsService.createVerificationUsageEvents(account);
      usageMetricsService.createSetupEventsForTimescaleDB(account);
    }
  }
}
