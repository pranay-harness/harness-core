package software.wings.security.encryption.migration;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.persistence.UpdatedAtAware.LAST_UPDATED_AT_KEY;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.FeatureName.ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.service.intfc.security.SecretManager.ID_KEY;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.danielbechler.util.Collections;
import io.harness.filesystem.FileIo;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.persistence.HPersistence;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.FeatureFlag;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.impl.security.GlobalEncryptDecryptClient;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.settings.SettingValue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.NotNull;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EncryptedDataAwsToGcpKmsMigrationHandler implements Handler<EncryptedData> {
  static final int BATCH_SIZE = 100;
  public static final int MAX_RETRY_COUNT = 3;
  WingsPersistence wingsPersistence;
  FeatureFlagService featureFlagService;
  PersistenceIteratorFactory persistenceIteratorFactory;
  GlobalEncryptDecryptClient globalEncryptDecryptClient;
  KmsService kmsService;
  GcpSecretsManagerService gcpSecretsManagerService;
  KmsConfig awsKmsConfig;
  GcpKmsConfig gcpKmsConfig;
  FileService fileService;

  @Inject
  public EncryptedDataAwsToGcpKmsMigrationHandler(WingsPersistence wingsPersistence,
      FeatureFlagService featureFlagService, PersistenceIteratorFactory persistenceIteratorFactory,
      KmsService kmsService, GlobalEncryptDecryptClient globalEncryptDecryptClient,
      GcpSecretsManagerService gcpSecretsManagerService, FileService fileService) {
    this.wingsPersistence = wingsPersistence;
    this.featureFlagService = featureFlagService;
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.kmsService = kmsService;
    this.globalEncryptDecryptClient = globalEncryptDecryptClient;
    this.gcpSecretsManagerService = gcpSecretsManagerService;
    this.fileService = fileService;
  }

  public void registerIterators() {
    this.awsKmsConfig = kmsService.getGlobalKmsConfig();
    if (awsKmsConfig == null) {
      logger.error(
          "Global AWS KMS config found to be null hence not registering EncryptedDataAwsToGcpKmsMigrationHandler iterators");
      return;
    }

    this.gcpKmsConfig = gcpSecretsManagerService.getGlobalKmsConfig();
    if (gcpKmsConfig == null) {
      logger.error(
          "Global GCP KMS config found to be null hence not registering EncryptedDataAwsToGcpKmsMigrationHandler iterators");
      return;
    }

    Optional<FeatureFlag> featureFlagOptional = featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);

    featureFlagOptional.ifPresent(featureFlag -> {
      MongoPersistenceIterator.FilterExpander<EncryptedData> filterExpander = null;

      if (featureFlag.isEnabled()) {
        logger.info(
            "Feature flag {} is enabled globally hence registering EncryptedDataAwsToGcpKmsMigrationHandler iterators",
            ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
        filterExpander = getFilterQuery();
      } else if (!Collections.isEmpty(featureFlag.getAccountIds())) {
        logger.info(
            "Feature flag {} is enabled for accounts {} hence registering EncryptedDataAwsToGcpKmsMigrationHandler iterators",
            ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, featureFlag.getAccountIds().toString());
        filterExpander = getFilterQueryWithAccountIdsFilter(featureFlag.getAccountIds());
      }

      if (filterExpander == null) {
        logger.info(
            "Feature flag {} is not enabled hence not registering EncryptedDataAwsToGcpKmsMigrationHandler iterators",
            ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS);
      } else {
        registerIteratorWithFactory(filterExpander);
      }
    });
  }

  private void registerIteratorWithFactory(
      @NotNull MongoPersistenceIterator.FilterExpander<EncryptedData> filterExpander) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("EncryptedDataAwsToGcpKmsMigrationHandler")
            .poolSize(5)
            .interval(ofSeconds(30))
            .build(),
        EncryptedData.class,
        MongoPersistenceIterator.<EncryptedData>builder()
            .clazz(EncryptedData.class)
            .fieldName(EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration)
            .targetInterval(ofHours(20))
            .acceptableNoAlertDelay(ofHours(40))
            .handler(this)
            .filterExpander(filterExpander)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  private MongoPersistenceIterator.FilterExpander<EncryptedData> getFilterQuery() {
    return query
        -> query.field(EncryptedDataKeys.accountId)
               .exists()
               .field(EncryptedDataKeys.kmsId)
               .equal(this.awsKmsConfig.getUuid())
               .field(EncryptedDataKeys.encryptionType)
               .equal(KMS);
  }

  private MongoPersistenceIterator.FilterExpander<EncryptedData> getFilterQueryWithAccountIdsFilter(
      Set<String> accountIds) {
    return query
        -> query.field(EncryptedDataKeys.accountId)
               .hasAnyOf(accountIds)
               .field(EncryptedDataKeys.kmsId)
               .equal(this.awsKmsConfig.getUuid())
               .field(EncryptedDataKeys.encryptionType)
               .equal(KMS);
  }

  @Override
  public void handle(@NotNull EncryptedData encryptedData) {
    if (!featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, encryptedData.getAccountId())) {
      logger.info(
          "Feature flag {} is not enabled hence not processing encryptedData {} for accountId= {} for AWS to GCP migration ",
          ACTIVE_MIGRATION_FROM_AWS_TO_GCP_KMS, encryptedData.getUuid(), encryptedData.getAccountId());
      return;
    }

    if (encryptedData.getEncryptedValue() != null
        && SettingValue.SettingVariableTypes.CONFIG_FILE == encryptedData.getType()) {
      boolean isUpdateSuccessful = updateEncryptedValueFile(encryptedData);
      if (!isUpdateSuccessful) {
        logger.info("Not proceeding with migration of CONFIG_FILE encrypted record {} as encryptedValue update failed ",
            encryptedData.getUuid());
        return;
      }
    }

    int retryCount = 0;
    boolean isMigrationSuccessful = false;
    while (!isMigrationSuccessful && retryCount < MAX_RETRY_COUNT) {
      if (encryptedData.getEncryptedValue() == null) {
        logger.info(
            "EncryptedValue value was null for encrypted record {} hence just updating encryption type info only",
            encryptedData.getUuid());
        isMigrationSuccessful = updateEncryptionInfo(encryptedData);
      } else {
        logger.info("Executing AWS to GCP migration for migration for encrypted record {} via decryption",
            encryptedData.getUuid());
        isMigrationSuccessful = decrypteAndMigrateToGcpKMS(encryptedData);
      }
      retryCount++;
    }
    if (!isMigrationSuccessful) {
      logger.error(
          "Could not migrate encrypted record {} from AWS to GCP KMS for after 3 retries", encryptedData.getUuid());
    }
  }

  private boolean updateEncryptedValueFile(@NotNull EncryptedData encryptedData) {
    File file = new File(Files.createTempDir(), new File(UUID.randomUUID().toString()).getName());
    file = fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
    byte[] bytes;
    try {
      bytes = Files.toByteArray(file);
    } catch (IOException ioe) {
      logger.error("IOException occurred for CONFIG_FILE encrypted record {} when migrating from AWS to GCP KMS ",
          encryptedData.getUuid(), ioe);
      return false;
    } finally {
      try {
        FileIo.deleteFileIfExists(file.getAbsolutePath());
      } catch (IOException ioe) {
        logger.error(
            "IOException occurred when deleting temp file for encrypted record {}", encryptedData.getUuid(), ioe);
      }
    }

    encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(bytes)).array());
    return true;
  }

  private boolean decrypteAndMigrateToGcpKMS(@NotNull EncryptedData encryptedData) {
    try {
      return globalEncryptDecryptClient.decrypt(encryptedData, encryptedData.getAccountId(), this.awsKmsConfig) != null;
    } catch (RuntimeException ex) {
      logger.error(
          "Exception occurred for encrypted record {} while AWS to GCP KMS migration", encryptedData.getUuid(), ex);
      return false;
    }
  }

  private boolean updateEncryptionInfo(@NotNull EncryptedData encryptedData) {
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .field(ID_KEY)
                                     .equal(encryptedData.getUuid())
                                     .field(LAST_UPDATED_AT_KEY)
                                     .equal(encryptedData.getLastUpdatedAt());

    UpdateOperations<EncryptedData> updateOperations =
        wingsPersistence.createUpdateOperations(EncryptedData.class)
            .set(EncryptedDataKeys.encryptionType, GCP_KMS)
            .set(EncryptedDataKeys.kmsId, gcpKmsConfig.getUuid())
            .set(EncryptedDataKeys.backupEncryptionType, KMS)
            .set(EncryptedDataKeys.backupKmsId, encryptedData.getKmsId())
            .set(EncryptedDataKeys.backupEncryptionKey, encryptedData.getEncryptionKey());

    EncryptedData savedEncryptedData =
        wingsPersistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);

    if (savedEncryptedData == null) {
      logger.error("Failed to save encrypted record {} during AWS to GCP KMS migration", encryptedData.getUuid());
      return false;
    }

    return true;
  }
}