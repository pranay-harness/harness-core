package io.harness.ccm.license;

import com.google.inject.Inject;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.service.intfc.AccountService;

import java.time.Instant;
import java.util.Optional;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofMinutes;

public class CeLicenseExpiryHandler implements Handler<Account> {
  private static final int CE_LICENSE_EXPIRY_INTERVAL_DAY = 1;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<Account> persistenceProvider;
  @Inject private AccountService accountService;
  PersistenceIterator<Account> iterator;

  public void registerIterators() {
    iterator = persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("CeLicenceExpiryProcessor")
            .poolSize(3)
            .interval(ofDays(CE_LICENSE_EXPIRY_INTERVAL_DAY))
            .build(),
        CeLicenseExpiryHandler.class,
        MongoPersistenceIterator.<Account, MorphiaFilterExpander<Account>>builder()
            .clazz(Account.class)
            .fieldName(AccountKeys.ceLicenseExpiryIteration)
            .targetInterval(ofMinutes(CE_LICENSE_EXPIRY_INTERVAL_DAY))
            .acceptableNoAlertDelay(ofMinutes(60))
            .acceptableExecutionTime(ofMinutes(5))
            .handler(this)
            .filterExpander(query -> query.field(AccountKeys.ceLicenseInfo).exists())
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Account account) {
    CeLicenseInfo ceLicenseInfo =
        Optional.ofNullable(account.getCeLicenseInfo()).orElse(CeLicenseInfo.builder().build());
    long expiryTime = ceLicenseInfo.getExpiryTimeWithGracePeriod();
    if (expiryTime != 0L && Instant.now().toEpochMilli() > expiryTime) {
      account.setCloudCostEnabled(false);
      accountService.update(account);
    }
  }
}
