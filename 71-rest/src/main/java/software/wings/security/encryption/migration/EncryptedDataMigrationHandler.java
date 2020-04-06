package software.wings.security.encryption.migration;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.migration.secretparents.AzureToAzureVaultMigrator;
import software.wings.security.encryption.migration.secretparents.EncryptedDataMigrator;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Optional;
import javax.validation.constraints.NotNull;

@Slf4j
public class EncryptedDataMigrationHandler implements Handler<EncryptedData> {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptedDataMigrator encryptedDataMigrator;
  @Inject private AzureToAzureVaultMigrator azureToAzureVaultMigrator;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private FeatureFlagService featureFlagService;

  public void registerIterators() {
    if (featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, null)) {
      return;
    }

    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("EncryptedDataParentMigrationHandler")
            .poolSize(5)
            .interval(ofSeconds(10))
            .build(),
        EncryptedData.class,
        MongoPersistenceIterator.<EncryptedData>builder()
            .clazz(EncryptedData.class)
            .fieldName(EncryptedDataKeys.nextMigrationIteration)
            .targetInterval(ofHours(15))
            .acceptableNoAlertDelay(ofDays(1))
            .handler(this)
            .filterExpander(
                query -> query.field(EncryptedDataKeys.accountId).exists().field(EncryptedDataKeys.parentIds).exists())
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  public void handle(@NotNull EncryptedData encryptedData) {
    int retryCount = 0;
    boolean isMigrationSucceeded = false;
    while (!isMigrationSucceeded && retryCount < 3) {
      isMigrationSucceeded = handleInternal(encryptedData.getUuid());
      retryCount++;
    }
    if (!isMigrationSucceeded) {
      logger.error("Could not migrate encrypted record {} after 3 retries", encryptedData.getUuid());
    }
  }

  private boolean handleInternal(@NotNull String encryptedId) {
    try {
      EncryptedData encryptedData =
          Optional.ofNullable(wingsPersistence.get(EncryptedData.class, encryptedId))
              .orElseThrow(() -> new InvalidRequestException("Could not find the encrypted data record."));

      encryptedData =
          fixAzureVaultSecretIfRequired(encryptedData)
              .orElseThrow(() -> new InvalidRequestException("The encrypted data is probably deleted or modified."));

      if (!migrateEncryptedData(encryptedData).isPresent()) {
        throw new InvalidRequestException("The encrypted data is probably deleted or modified during migration.");
      }

      return true;
    } catch (RuntimeException e) {
      logger.warn("Migration of encrypted record {} failed while retrying because of error", encryptedId, e);
      return false;
    }
  }

  private Optional<EncryptedData> migrateEncryptedData(@NotNull EncryptedData encryptedData) {
    if (encryptedDataMigrator.shouldMigrate(encryptedData) && encryptedDataMigrator.canMigrate(encryptedData)) {
      return encryptedDataMigrator.migrateEncryptedDataParents(encryptedData);
    }
    return Optional.of(encryptedData);
  }

  private Optional<EncryptedData> fixAzureVaultSecretIfRequired(@NotNull EncryptedData encryptedData) {
    if (azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData)) {
      return azureToAzureVaultMigrator.convertToAzureVaultSettingType(encryptedData);
    }
    return Optional.of(encryptedData);
  }
}
