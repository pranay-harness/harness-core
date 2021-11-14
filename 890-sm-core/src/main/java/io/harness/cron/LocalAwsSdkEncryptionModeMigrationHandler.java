package io.harness.cron;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.FeatureName;
import io.harness.beans.LocalEncryptionMigrationInfo;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.security.encryption.EncryptedMech;

import java.util.Optional;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(HarnessTeam.PL)
public class LocalAwsSdkEncryptionModeMigrationHandler extends LocalEncryptionMigrationHandler {
  private static final FeatureName featureFlag = FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE;

  @Override
  public void run() {
    startMigration(featureFlag);
  }

  @Override
  protected void performMigration(String accountId) {
    PageRequest<EncryptedData> pageRequest = getPageRequest(accountId);
    String migrationStateUuid = null;
    EncryptedData lastMigratedRecord = null;

    Optional<LocalEncryptionMigrationInfo> lastMigrationState = getLastMigrationState(accountId, featureFlag);
    if (lastMigrationState.isPresent()) {
      migrationStateUuid = lastMigrationState.get().getUuid();
    }

    PageResponse<EncryptedData> encryptedDataPageResponse =
        getEncryptedRecords(accountId, pageRequest, lastMigrationState);
    while (encryptedDataPageResponse.iterator().hasNext()) {
      EncryptedData encryptedData = encryptedDataPageResponse.iterator().next();
      UpdateOperations<EncryptedData> updateOperations = secretsDao.getUpdateOperations();
      if (encryptedData.getEncryptedMech() == null) {
        EncryptedData migratedRecord = (EncryptedData) localEncryptor.encryptSecret(
            accountId, new String(localEncryptor.fetchSecretValue(accountId, encryptedData, null)), null);

        updateOperations.set(EncryptedDataKeys.encryptedMech, migratedRecord.getEncryptedMech())
            .set(EncryptedDataKeys.encryptionKey, migratedRecord.getEncryptionKey())
            .set(EncryptedDataKeys.encryptedValueBytes, migratedRecord.getEncryptedValueBytes())
            .set(EncryptedDataKeys.encryptedValue, null);
      } else {
        updateOperations.set(EncryptedDataKeys.encryptedMech, EncryptedMech.AWS_ENCRYPTION_SDK_CRYPTO)
            .set(EncryptedDataKeys.encryptionKey, encryptedData.getAdditionalMetadata().getSecretKeyUuid())
            .set(EncryptedDataKeys.encryptedValueBytes, encryptedData.getAdditionalMetadata().getAwsEncryptedSecret())
            .set(EncryptedDataKeys.encryptedValue, null);
      }

      secretsDao.updateSecret(encryptedData, updateOperations);
      lastMigratedRecord = encryptedData;
    }

    saveMigrationState(migrationStateUuid, lastMigratedRecord);
  }

  private PageRequest<EncryptedData> getPageRequest(String accountId) {
    PageRequest<EncryptedData> pageRequest = initPageRequest(accountId);
    pageRequest.addFilter(EncryptedData.EncryptedDataKeys.encryptedMech, null);
    return pageRequest;
  }
}
