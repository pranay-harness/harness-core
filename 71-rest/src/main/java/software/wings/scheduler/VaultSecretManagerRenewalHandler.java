package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;
import software.wings.beans.VaultConfig;
import software.wings.service.intfc.security.VaultService;

@Slf4j
public class VaultSecretManagerRenewalHandler implements Handler<SecretManagerConfig> {
  @Inject private VaultService vaultService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("VaultSecretManagerRenewalHandler")
            .poolSize(2)
            .interval(ofMinutes(1))
            .build(),
        SecretManagerConfig.class,
        MongoPersistenceIterator.<SecretManagerConfig>builder()
            .clazz(SecretManagerConfig.class)
            .fieldName(SecretManagerConfigKeys.nextTokenRenewIteration)
            .targetInterval(ofMinutes(10))
            .acceptableNoAlertDelay(ofMinutes(20))
            .handler(this)
            .filterExpander(query -> query.field(SecretManagerConfigKeys.encryptionType).equal(EncryptionType.VAULT))
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(SecretManagerConfig secretManagerConfig) {
    logger.info("renewing client tokens for {}", secretManagerConfig.getUuid());
    VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
    try {
      vaultService.renewTokens(vaultConfig);
      vaultService.renewAppRoleClientToken(vaultConfig);
    } catch (Exception e) {
      logger.info("Failed to renew vault token for account id {}", secretManagerConfig.getUuid(), e);
    }
  }
}
