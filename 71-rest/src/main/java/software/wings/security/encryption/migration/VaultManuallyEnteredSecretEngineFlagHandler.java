package software.wings.security.encryption.migration;

import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;

import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.security.encryption.EncryptionType;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;

import software.wings.beans.VaultConfig;
import software.wings.beans.VaultConfig.VaultConfigKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.VaultService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VaultManuallyEnteredSecretEngineFlagHandler implements Handler<SecretManagerConfig> {
  VaultService vaultService;
  WingsPersistence wingsPersistence;
  PersistenceIteratorFactory persistenceIteratorFactory;
  MorphiaPersistenceProvider<SecretManagerConfig> persistenceProvider;
  AccountService accountService;

  @Inject
  public VaultManuallyEnteredSecretEngineFlagHandler(VaultService vaultService, WingsPersistence wingsPersistence,
      PersistenceIteratorFactory persistenceIteratorFactory, AccountService accountService,
      MorphiaPersistenceProvider<SecretManagerConfig> morphiaPersistenceProvider) {
    this.vaultService = vaultService;
    this.wingsPersistence = wingsPersistence;
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.accountService = accountService;
    this.persistenceProvider = morphiaPersistenceProvider;
  }

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("VaultManuallyEnteredSecretEngineFlagHandler")
            .poolSize(2)
            .interval(ofSeconds(120))
            .build(),
        SecretManagerConfig.class,
        MongoPersistenceIterator.<SecretManagerConfig, MorphiaFilterExpander<SecretManagerConfig>>builder()
            .clazz(SecretManagerConfig.class)
            .fieldName(SecretManagerConfigKeys.manuallyEnteredSecretEngineMigrationIteration)
            .targetInterval(ofHours(1))
            .acceptableNoAlertDelay(ofHours(2))
            .handler(this)
            .entityProcessController(new AccountStatusBasedEntityProcessController<>(accountService))
            .filterExpander(query -> query.field(SecretManagerConfigKeys.encryptionType).equal(EncryptionType.VAULT))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(SecretManagerConfig secretManagerConfig) {
    log.info("Setting vault manually entered flag for config id {}", secretManagerConfig.getUuid());
    VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
    vaultConfig.setSecretId(SECRET_MASK);
    vaultConfig.setAuthToken(SECRET_MASK);
    boolean engineManuallyEntered;
    try {
      List<SecretEngineSummary> secretEngines = vaultService.listSecretEngines(vaultConfig);
      engineManuallyEntered = isSecretEngineManuallyEntered(
          secretEngines, vaultConfig.getSecretEngineName(), vaultConfig.getSecretEngineVersion());
    } catch (Exception e) {
      log.error("Cannot list secret engine, permission is not present.", e);
      engineManuallyEntered = true;
    }
    if (engineManuallyEntered != vaultConfig.isEngineManuallyEntered()) {
      wingsPersistence.updateField(SecretManagerConfig.class, vaultConfig.getUuid(),
          VaultConfigKeys.engineManuallyEntered, engineManuallyEntered);
    }
  }

  public static boolean isSecretEngineManuallyEntered(
      List<SecretEngineSummary> secretEngineSummaries, String secretEngineName, int secretEngineVersion) {
    if (secretEngineSummaries == null || secretEngineName == null) {
      return true;
    }
    return secretEngineSummaries.stream().noneMatch(secretEngineSummary
        -> secretEngineName.equals(secretEngineSummary.getName())
            && secretEngineVersion == secretEngineSummary.getVersion());
  }
}
