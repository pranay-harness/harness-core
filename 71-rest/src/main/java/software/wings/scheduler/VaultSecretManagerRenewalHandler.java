package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.VaultConfig.AccessType.APP_ROLE;
import static software.wings.beans.alert.AlertType.InvalidKMS;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.VaultService;

import java.util.concurrent.TimeUnit;

@Slf4j
public class VaultSecretManagerRenewalHandler implements Handler<SecretManagerConfig> {
  @Inject private VaultService vaultService;
  @Inject private AlertService alertService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<SecretManagerConfig> persistenceProvider;
  private static final long DEFAULT_RENEWAL_INTERVAL = 15;

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
            .filterExpander(query -> query.field(SecretManagerConfigKeys.encryptionType).equal(EncryptionType.VAULT))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(SecretManagerConfig secretManagerConfig) {
    logger.info("renewing client tokens for {}", secretManagerConfig.getUuid());
    VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
    KmsSetupAlert kmsSetupAlert =
        KmsSetupAlert.builder()
            .kmsId(vaultConfig.getUuid())
            .message(vaultConfig.getName()
                + "(Hashicorp Vault) is not able to renew the token. Please check your setup and ensure that token is renewable")
            .build();

    try {
      long renewalInterval = vaultConfig.calculateRenewalInterval(DEFAULT_RENEWAL_INTERVAL);
      if (renewalInterval <= 0 || SecretManagerConfig.isTemplatized(secretManagerConfig)) {
        logger.info("Vault {} not configured for renewal.", vaultConfig.getUuid());
        return;
      }
      if (!checkIfEligibleForRenewal(vaultConfig.getRenewedAt(), renewalInterval)) {
        logger.info(
            "Vault config {} renewed at {} not renewing now", vaultConfig.getUuid(), vaultConfig.getRenewedAt());
        return;
      }
      if (vaultConfig.getAccessType() == APP_ROLE) {
        vaultService.renewAppRoleClientToken(vaultConfig);
      } else {
        vaultService.renewToken(vaultConfig);
      }
      alertService.closeAlert(vaultConfig.getAccountId(), GLOBAL_APP_ID, InvalidKMS, kmsSetupAlert);
    } catch (Exception e) {
      logger.info("Failed to renew vault token for account id {}", secretManagerConfig.getUuid(), e);
      alertService.openAlert(vaultConfig.getAccountId(), GLOBAL_APP_ID, InvalidKMS, kmsSetupAlert);
    }
  }

  private static boolean checkIfEligibleForRenewal(long renewedAt, long renewalInterval) {
    long currentTime = System.currentTimeMillis();
    return TimeUnit.MILLISECONDS.toMinutes(currentTime - renewedAt) >= renewalInterval;
  }
}
