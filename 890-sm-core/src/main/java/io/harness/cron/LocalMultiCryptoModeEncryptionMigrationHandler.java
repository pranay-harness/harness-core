package io.harness.cron;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.FeatureName;
import io.harness.beans.LocalEncryptionMigrationInfo;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedRecord;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class LocalMultiCryptoModeEncryptionMigrationHandler extends LocalEncryptionMigrationHandler {
  private static final FeatureName featureFlag = FeatureName.LOCAL_MULTI_CRYPTO_MODE;

  @Override
  public void run() {
    startMigration();
  }

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
    List<EncryptedData> encryptedDataList = encryptedDataPageResponse.getResponse();
    for (EncryptedData encryptedData : encryptedDataList) {
      log.info(getFeatureName().name() + ": Processing encrypted record : {}", encryptedData);
      EncryptedRecord migratedRecord = localEncryptor.encryptSecret(
          accountId, new String(localEncryptor.fetchSecretValue(accountId, encryptedData, null)), null);

      UpdateOperations<EncryptedData> updateOperations = secretsDao.getUpdateOperations();
      updateOperations.set(EncryptedDataKeys.encryptedMech, migratedRecord.getEncryptedMech())
          .set(EncryptedDataKeys.additionalMetadata,
              AdditionalMetadata.builder()
                  .values(migratedRecord.getAdditionalMetadata().addValues(
                      encryptedData.getAdditionalMetadata().getValues()))
                  .build());
      secretsDao.updateSecret(encryptedData, updateOperations);
      log.info(getFeatureName().name() + ": Processed encrypted record with updates : {}", updateOperations);
      lastMigratedRecord = encryptedData;
    }

    saveMigrationState(accountId, migrationStateUuid, lastMigratedRecord);
  }

  @Override
  protected FeatureName getFeatureName() {
    return featureFlag;
  }

  private PageRequest<EncryptedData> getPageRequest(String accountId) {
    PageRequest<EncryptedData> pageRequest = initPageRequest(accountId);
    pageRequest.addFilter(EncryptedDataKeys.encryptedMech, null);
    return pageRequest;
  }
}
