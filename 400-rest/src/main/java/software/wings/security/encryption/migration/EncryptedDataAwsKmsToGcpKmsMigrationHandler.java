package software.wings.security.encryption.migration;

import static io.harness.beans.FeatureName.ACTIVE_MIGRATION_FROM_AWS_KMS_TO_GCP_KMS;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;

import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.FeatureFlag;
import io.harness.beans.MigrateSecretTask;
import io.harness.ff.FeatureFlagService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;
import io.harness.secrets.SecretService;

import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EncryptedDataAwsKmsToGcpKmsMigrationHandler implements MongoPersistenceIterator.Handler<EncryptedData> {
  public static final int MAX_RETRY_COUNT = 3;
  private final WingsPersistence wingsPersistence;
  private final FeatureFlagService featureFlagService;
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final GcpSecretsManagerService gcpSecretsManagerService;
  private final KmsService kmsService;
  private final MorphiaPersistenceProvider<EncryptedData> persistenceProvider;
  private final SecretService secretService;
  private KmsConfig kmsConfig;
  private GcpKmsConfig gcpKmsConfig;

  @Inject
  public EncryptedDataAwsKmsToGcpKmsMigrationHandler(WingsPersistence wingsPersistence,
      FeatureFlagService featureFlagService, PersistenceIteratorFactory persistenceIteratorFactory,
      GcpSecretsManagerService gcpSecretsManagerService, KmsService kmsService,
      MorphiaPersistenceProvider<EncryptedData> persistenceProvider, SecretService secretService) {
    this.wingsPersistence = wingsPersistence;
    this.featureFlagService = featureFlagService;
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.gcpSecretsManagerService = gcpSecretsManagerService;
    this.kmsService = kmsService;
    this.persistenceProvider = persistenceProvider;
    this.secretService = secretService;
  }

  public void registerIterators() {
    this.gcpKmsConfig = gcpSecretsManagerService.getGlobalKmsConfig();
    if (gcpKmsConfig == null) {
      log.error(
          "Global GCP KMS config found to be null hence not registering EncryptedDataAwsKmsToGcpKmsMigrationHandler iterators");
      return;
    }

    this.kmsConfig = kmsService.getGlobalKmsConfig();
    if (kmsConfig == null) {
      log.error(
          "Global AWS KMS config found to be null hence not registering EncryptedDataAwsKmsToGcpKmsMigrationHandler iterators");
      return;
    }

    Optional<FeatureFlag> featureFlagOptional =
        featureFlagService.getFeatureFlag(ACTIVE_MIGRATION_FROM_AWS_KMS_TO_GCP_KMS);
    featureFlagOptional.ifPresent(featureFlag -> {
      MorphiaFilterExpander<EncryptedData> filterExpander = null;

      if (featureFlag.isEnabled()) {
        log.info(
            "Feature flag {} is enabled globally hence registering EncryptedDataAwsKmsToGcpKmsMigrationHandler iterators",
            ACTIVE_MIGRATION_FROM_AWS_KMS_TO_GCP_KMS);
        filterExpander = getFilterQuery();
      } else if (!hasNone(featureFlag.getAccountIds())) {
        log.info(
            "Feature flag {} is enabled for accounts {} hence registering EncryptedDataAwsKmsToGcpKmsMigrationHandler iterators",
            ACTIVE_MIGRATION_FROM_AWS_KMS_TO_GCP_KMS, featureFlag.getAccountIds().toString());
        filterExpander = getFilterQueryWithAccountIdsFilter(featureFlag.getAccountIds());
      }

      if (filterExpander == null) {
        log.info(
            "Feature flag {} is not enabled hence not registering EncryptedDataAwsKmsToGcpKmsMigrationHandler iterators",
            ACTIVE_MIGRATION_FROM_AWS_KMS_TO_GCP_KMS);
      } else {
        registerIteratorWithFactory(filterExpander);
      }
    });
  }

  private void registerIteratorWithFactory(@NotNull MorphiaFilterExpander<EncryptedData> filterExpander) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("EncryptedDataAwsKmsToGcpKmsMigrationHandler")
            .poolSize(5)
            .interval(ofSeconds(30))
            .build(),
        EncryptedData.class,
        MongoPersistenceIterator.<EncryptedData, MorphiaFilterExpander<EncryptedData>>builder()
            .clazz(EncryptedData.class)
            .fieldName(EncryptedDataKeys.nextAwsKmsToGcpKmsMigrationIteration)
            .targetInterval(ofHours(20))
            .acceptableNoAlertDelay(ofHours(40))
            .handler(this)
            .filterExpander(filterExpander)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  private MorphiaFilterExpander<EncryptedData> getFilterQuery() {
    return query
        -> query.field(EncryptedDataKeys.accountId)
               .exists()
               .field(EncryptedDataKeys.encryptionType)
               .equal(KMS)
               .field(EncryptedDataKeys.kmsId)
               .equal(kmsConfig.getUuid())
               .field(EncryptedDataKeys.ngMetadata)
               .equal(null);
  }

  private MorphiaFilterExpander<EncryptedData> getFilterQueryWithAccountIdsFilter(Set<String> accountIds) {
    return query
        -> query.field(EncryptedDataKeys.accountId)
               .hasAnyOf(accountIds)
               .field(EncryptedDataKeys.encryptionType)
               .equal(KMS)
               .field(EncryptedDataKeys.kmsId)
               .equal(kmsConfig.getUuid())
               .field(EncryptedDataKeys.ngMetadata)
               .equal(null);
  }

  @Override
  public void handle(@NotNull EncryptedData encryptedData) {
    if (!featureFlagService.isEnabled(ACTIVE_MIGRATION_FROM_AWS_KMS_TO_GCP_KMS, encryptedData.getAccountId())) {
      log.info(
          "Feature flag {} is not enabled hence not processing encryptedData {} for accountId {} for AWS KMS to GCP KMS migration ",
          ACTIVE_MIGRATION_FROM_AWS_KMS_TO_GCP_KMS, encryptedData.getUuid(), encryptedData.getAccountId());
      return;
    }
    int retryCount = 0;
    boolean isMigrationSuccessful = false;
    while (!isMigrationSuccessful && retryCount < MAX_RETRY_COUNT) {
      if (encryptedData.getEncryptedValue() == null) {
        log.info("EncryptedValue value was null for encrypted record {} hence just updating encryption type info only",
            encryptedData.getUuid());
        isMigrationSuccessful = updateEncryptionInfo(encryptedData);
      } else {
        log.info("Executing AWS KMS to GCP KMS migration for encrypted record {}", encryptedData.getUuid());
        isMigrationSuccessful = migrateToGcpKMS(encryptedData);
      }
      retryCount++;
    }
    if (!isMigrationSuccessful) {
      log.error(
          "Could not migrate encrypted record {} from AWS KMS to GCP KMS for after 3 retries", encryptedData.getUuid());
    }
  }

  private boolean updateEncryptionInfo(@NotNull EncryptedData encryptedData) {
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .field(EncryptedDataKeys.ID_KEY)
                                     .equal(encryptedData.getUuid())
                                     .field(EncryptedDataKeys.lastUpdatedAt)
                                     .equal(encryptedData.getLastUpdatedAt());

    UpdateOperations<EncryptedData> updateOperations =
        wingsPersistence.createUpdateOperations(EncryptedData.class)
            .set(EncryptedDataKeys.encryptionType, GCP_KMS)
            .set(EncryptedDataKeys.kmsId, gcpKmsConfig.getUuid())
            .set(EncryptedDataKeys.backupEncryptionType, KMS)
            .set(EncryptedDataKeys.backupKmsId, kmsConfig.getUuid())
            .set(EncryptedDataKeys.backupEncryptionKey, encryptedData.getEncryptionKey());

    EncryptedData savedEncryptedData =
        wingsPersistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);

    if (savedEncryptedData == null) {
      log.error("Failed to save encrypted record {} during AWS KMS to GCP KMS migration", encryptedData.getUuid());
      return false;
    }
    return true;
  }

  protected boolean migrateToGcpKMS(@NotNull EncryptedData encryptedData) {
    try {
      MigrateSecretTask migrateSecretTask = MigrateSecretTask.builder()
                                                .accountId(encryptedData.getAccountId())
                                                .secretId(encryptedData.getUuid())
                                                .fromConfig(kmsConfig)
                                                .toConfig(gcpKmsConfig)
                                                .build();
      secretService.migrateSecret(migrateSecretTask);
      return true;
    } catch (Exception ex) {
      log.error(
          "Exception occurred for encrypted record {} while AWS KMS to GCP KMS migration", encryptedData.getUuid(), ex);
      return false;
    }
  }
}
