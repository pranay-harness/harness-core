package io.harness.cron;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.FeatureName;
import io.harness.beans.LocalEncryptionMigrationInfo;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SortOrder;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.ff.FeatureFlagService;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.repositories.LocalEncryptionMigrationInfoRepository;
import io.harness.secrets.SecretsDao;
import io.harness.security.encryption.EncryptionType;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public abstract class LocalEncryptionMigrationHandler implements Runnable {
  @Inject protected FeatureFlagService featureFlagService;
  @Inject protected SecretsDao secretsDao;
  @Inject protected LocalEncryptionMigrationInfoRepository localEncryptionMigrationInfoRepository;
  @Inject protected LocalEncryptor localEncryptor;
  @Inject protected PersistentLocker persistentLocker;

  private static final String LOCAL_ENCRYPTION_MIGRATION_CRON_PREFIX = "LOCAL_ENCRYPTION_MIGRATION_CRON:";
  private static final long LOCAL_ENCRYPTION_CRON_LOCK_EXPIRY_IN_SECONDS = 60;

  private static final String PAGE_SIZE = "1000";

  abstract protected void performMigration(String accountId);

  protected void startMigration(FeatureName featureFlag) {
    Set<String> accountIds = featureFlagService.getAccountIds(featureFlag);

    accountIds.forEach(accountId -> {
      try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR);
           AcquiredLock lock = persistentLocker.tryToAcquireLock(LocalEncryptionMigrationInfo.class,
               LOCAL_ENCRYPTION_MIGRATION_CRON_PREFIX + featureFlag + ":" + accountId,
               Duration.ofSeconds(LOCAL_ENCRYPTION_CRON_LOCK_EXPIRY_IN_SECONDS))) {
        if (lock == null) {
          log.error("Unable to fetch lock for running local encryption migration for feature: {} for account : {}",
              featureFlag, accountId);
          return;
        }

        performMigration(accountId);
      }
    });
  }

  protected PageRequest<EncryptedData> initPageRequest(String accountId) {
    PageRequest<EncryptedData> pageRequest = new PageRequest<>();
    pageRequest.setLimit(PAGE_SIZE);
    pageRequest.setOffset("0");
    pageRequest.addFilter(EncryptedDataKeys.accountId, SearchFilter.Operator.EQ, accountId);
    pageRequest.addFilter(EncryptedDataKeys.encryptionType, SearchFilter.Operator.EQ, EncryptionType.LOCAL);
    pageRequest.addOrder(EncryptedDataKeys.createdAt, SortOrder.OrderType.ASC);
    return pageRequest;
  }

  protected PageResponse<EncryptedData> getEncryptedRecords(String accountId, PageRequest<EncryptedData> pageRequest,
      Optional<LocalEncryptionMigrationInfo> lastMigrationState) {
    lastMigrationState.ifPresent(localEncryptionMigrationInfo
        -> pageRequest.addFilter(EncryptedDataKeys.createdAt, SearchFilter.Operator.GT,
            localEncryptionMigrationInfo.getLastMigratedRecordCreatedAtTimestamp()));
    return secretsDao.listSecrets(pageRequest);
  }

  protected Optional<LocalEncryptionMigrationInfo> getLastMigrationState(String accountId, FeatureName featureName) {
    return localEncryptionMigrationInfoRepository.findByAccountIdAndMode(accountId, featureName.name());
  }

  protected void saveMigrationState(String presentMigrationStateUuid, EncryptedData lastMigratedRecord) {
    localEncryptionMigrationInfoRepository.save(
        LocalEncryptionMigrationInfo.builder()
            .uuid(presentMigrationStateUuid)
            .lastMigratedRecordCreatedAtTimestamp(lastMigratedRecord.getCreatedAt())
            .lastMigratedRecordId(lastMigratedRecord.getUuid())
            .build());
  }
}
