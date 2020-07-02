package software.wings.scheduler.account;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.licensing.LicenseService;

/**
 * Handler class that checks for license expiry
 * @author rktummala
 */

@Slf4j
public class LicenseCheckHandler implements Handler<Account> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private LicenseService licenseService;
  @Inject private MorphiaPersistenceProvider<Account> persistenceProvider;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("LicenseExpiryCheck").poolSize(2).interval(ofSeconds(30)).build(),
        LicenseCheckHandler.class,
        MongoPersistenceIterator.<Account, MorphiaFilterExpander<Account>>builder()
            .clazz(Account.class)
            .fieldName(AccountKeys.licenseExpiryCheckIteration)
            .targetInterval(ofMinutes(30))
            .acceptableNoAlertDelay(ofMinutes(60))
            .acceptableExecutionTime(ofSeconds(15))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Account account) {
    try {
      logger.info("Running license check job");
      licenseService.checkForLicenseExpiry(account);
      logger.info("License check job complete");
    } catch (Exception ex) {
      logger.error("Error while checking license", ex);
    }
  }
}
