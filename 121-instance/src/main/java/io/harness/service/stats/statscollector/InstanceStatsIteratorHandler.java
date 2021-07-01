package io.harness.service.stats.statscollector;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.workers.background.AccountLevelEntityProcessController;

import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountStatus;
import software.wings.beans.LicenseInfo;
import software.wings.service.intfc.AccountService;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.DX)
public class InstanceStatsIteratorHandler implements Handler<Account> {
  public static final long TWO_MONTH_IN_MILLIS = 5184000000L;
  public static final String LOCK_KEY = "INSTANCE_STATS_ITERATOR:";

  private PersistenceIteratorFactory persistenceIteratorFactory;
  private AccountService accountService;
  private MorphiaPersistenceProvider<Account> persistenceProvider;
  private PersistentLocker persistentLocker;
  private InstanceStatsCollectorImpl instanceStatsCollector;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("InstanceStatsMetricsPublisher")
            .poolSize(1)
            .interval(ofMinutes(10))
            .build(),
        InstanceStatsIteratorHandler.class,
        MongoPersistenceIterator.<Account, MorphiaFilterExpander<Account>>builder()
            .clazz(Account.class)
            .fieldName(AccountKeys.instanceStatsMetricsPublisherInteration)
            .targetInterval(ofMinutes(10))
            // TODO check acceptableNoAlertDelay
            .acceptableNoAlertDelay(ofMinutes(120))
            .acceptableExecutionTime(ofMinutes(5))
            .handler(this)
            .entityProcessController(new AccountLevelEntityProcessController(accountService))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Account account) {
    String accountId = account.getUuid();
    try (AutoLogContext ignore = new AccountLogContext(accountId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      if (account.getLicenseInfo() == null || account.getLicenseInfo().getAccountStatus() == null
          || shouldSkipStatsCollection(account.getLicenseInfo())) {
        log.info("Skipping instance stats since the account is not active");
      } else {
        log.info("Running instance stats metrics iterator");
      }
      createStats(accountId);
    } catch (Exception exception) {
      log.error("Failed to publish instance stats metrics {}", exception);
    }
  }

  // ------------------- PRIVATE METHODS -----------------

  private void createStats(final String accountId) {
    try (AcquiredLock lock = persistentLocker.tryToAcquireLock(LOCK_KEY + accountId, Duration.ofSeconds(120))) {
      if (lock == null) {
        log.error("Unable to acquire lock for creating instance stats");
        return;
      }

      Stopwatch sw = Stopwatch.createStarted();
      boolean success = instanceStatsCollector.createStats(accountId);
      if (success) {
        log.info(
            "Successfully published instance stats to timescale, time taken : {}", sw.elapsed(TimeUnit.MILLISECONDS));
      } else {
        log.error("Unable to publish instance stats to timescale, time taken : {}", sw.elapsed(TimeUnit.MILLISECONDS));
      }
    }
  }

  private boolean shouldSkipStatsCollection(LicenseInfo licenseInfo) {
    if (AccountStatus.ACTIVE.equals(licenseInfo.getAccountStatus())) {
      return false;
    } else if (AccountStatus.DELETED.equals(licenseInfo.getAccountStatus())
        || AccountStatus.INACTIVE.equals(licenseInfo.getAccountStatus())
        || AccountStatus.MARKED_FOR_DELETION.equals(licenseInfo.getAccountStatus())) {
      return true;
    } else if (AccountStatus.EXPIRED.equals(licenseInfo.getAccountStatus())
        && System.currentTimeMillis() > (licenseInfo.getExpiryTime() + TWO_MONTH_IN_MILLIS)) {
      return true;
    }
    return false;
  }
}
