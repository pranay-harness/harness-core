package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.service.intfc.security.VaultService;

@Slf4j
public class AdministrativeJob implements Handler<Account> {
  public static final String ADMINISTRATIVE_CRON_NAME = "ADMINISTRATIVE_CRON_NAME";
  public static final String ADMINISTRATIVE_CRON_GROUP = "ADMINISTRATIVE_CRON_GROUP";

  @Inject private VaultService vaultService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("AdministrativeJob")
            .poolSize(1)
            .interval(ofMinutes(10))
            .build(),
        Account.class,
        MongoPersistenceIterator.<Account>builder()
            .clazz(Account.class)
            .fieldName(AccountKeys.secretManagerValidationIterator)
            .targetInterval(ofMinutes(10))
            .acceptableNoAlertDelay(ofMinutes(1))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(Account account) {
    logger.info("renewing tokens for {}", account.getUuid());
    try {
      vaultService.renewTokens(account.getUuid());
      vaultService.appRoleLogin(account.getUuid());
    } catch (Exception e) {
      logger.info("Failed to renew vault token for account id {}", account.getUuid(), e);
    }
  }
}
