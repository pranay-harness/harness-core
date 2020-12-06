package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.security.encryption.AccessType.APP_ROLE;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.alert.AlertType.InvalidKMS;

import static java.time.Duration.ofSeconds;

import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.security.encryption.EncryptionType;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;

import software.wings.beans.VaultConfig;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.VaultService;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VaultSecretManagerRenewalHandler implements Handler<SecretManagerConfig> {
  @Inject private AccountService accountService;
  @Inject private VaultService vaultService;
  @Inject private AlertService alertService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<SecretManagerConfig> persistenceProvider;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("VaultSecretManagerRenewalHandler")
            .poolSize(5)
            .interval(ofSeconds(5))
            .build(),
        SecretManagerConfig.class,
        MongoPersistenceIterator.<SecretManagerConfig, MorphiaFilterExpander<SecretManagerConfig>>builder()
            .clazz(SecretManagerConfig.class)
            .fieldName(SecretManagerConfigKeys.nextTokenRenewIteration)
            .targetInterval(ofSeconds(31))
            .acceptableNoAlertDelay(ofSeconds(62))
            .handler(this)
            .entityProcessController(new AccountStatusBasedEntityProcessController<>(accountService))
            .filterExpander(query -> query.field(SecretManagerConfigKeys.encryptionType).equal(EncryptionType.VAULT))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(SecretManagerConfig secretManagerConfig) {
    log.info("renewing client tokens for {}", secretManagerConfig.getUuid());
    VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
    KmsSetupAlert kmsSetupAlert = vaultService.getRenewalAlert(vaultConfig);
    try {
      long renewalInterval = vaultConfig.getRenewalInterval();
      if (renewalInterval <= 0 || secretManagerConfig.isTemplatized()) {
        log.info("Vault {} not configured for renewal.", vaultConfig.getUuid());
        return;
      }
      if (!checkIfEligibleForRenewal(vaultConfig.getRenewedAt(), renewalInterval)) {
        log.info("Vault config {} renewed at {} not renewing now", vaultConfig.getUuid(), vaultConfig.getRenewedAt());
        return;
      }
      if (vaultConfig.getAccessType() == APP_ROLE) {
        vaultService.renewAppRoleClientToken(vaultConfig);
      } else {
        vaultService.renewToken(vaultConfig);
      }
      alertService.closeAlert(vaultConfig.getAccountId(), GLOBAL_APP_ID, InvalidKMS, kmsSetupAlert);
    } catch (Exception e) {
      log.info("Failed to renew vault token for vault id {}", secretManagerConfig.getUuid(), e);
      alertService.openAlert(vaultConfig.getAccountId(), GLOBAL_APP_ID, InvalidKMS, kmsSetupAlert);
    }
  }

  private boolean checkIfEligibleForRenewal(long renewedAt, long renewalInterval) {
    long currentTime = System.currentTimeMillis();
    return TimeUnit.MILLISECONDS.toMinutes(currentTime - renewedAt) >= renewalInterval;
  }
}
