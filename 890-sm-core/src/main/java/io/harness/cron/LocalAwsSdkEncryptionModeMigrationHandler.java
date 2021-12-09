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
import io.harness.security.encryption.EncryptedMech;
import io.harness.security.encryption.EncryptedRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class LocalAwsSdkEncryptionModeMigrationHandler extends LocalEncryptionMigrationHandler {
  private static final FeatureName featureFlag = FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE;

  @Override
  public void run() {
    startMigration();
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

    List<EncryptedData> encryptedDataList = encryptedDataPageResponse.getResponse();
    for (EncryptedData encryptedData : encryptedDataList) {
      log.info(getFeatureName().name() + ": Processing encrypted record : {}", encryptedData);
      UpdateOperations<EncryptedData> updateOperations = secretsDao.getUpdateOperations();
      if (encryptedData.getEncryptedMech() == null) {
        EncryptedRecord migratedRecord = encryptRecord(accountId, decryptRecord(accountId, encryptedData));

        updateOperations.set(EncryptedDataKeys.encryptedMech, migratedRecord.getEncryptedMech())
            .set(EncryptedDataKeys.encryptionKey, migratedRecord.getEncryptionKey())
            .set(EncryptedDataKeys.encryptedValueBytes, migratedRecord.getEncryptedValueBytes())
            .unset(EncryptedDataKeys.encryptedValue);
      } else {
        updateOperations.set(EncryptedDataKeys.encryptedMech, EncryptedMech.AWS_ENCRYPTION_SDK_CRYPTO)
            .set(EncryptedDataKeys.encryptionKey, encryptedData.getAdditionalMetadata().getSecretKeyUuid())
            .set(EncryptedDataKeys.encryptedValueBytes, encryptedData.getAdditionalMetadata().getAwsEncryptedSecret())
            .unset(EncryptedDataKeys.encryptedValue);

        removeAwsSdkKeysFromMetadata(encryptedData);
        if (encryptedData.getAdditionalMetadata().getValues() != null) {
          updateOperations.set(EncryptedDataKeys.additionalMetadata, encryptedData.getAdditionalMetadata());
        } else {
          updateOperations.unset(EncryptedDataKeys.additionalMetadata);
        }
      }

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
    pageRequest.addFilter(EncryptedData.EncryptedDataKeys.encryptedMech, null);
    return pageRequest;
  }

  private void removeAwsSdkKeysFromMetadata(EncryptedRecord encryptedRecord) {
    Map<String, Object> values = encryptedRecord.getAdditionalMetadata().getValues();
    if (values != null) {
      values.remove(AdditionalMetadata.AWS_ENCRYPTED_SECRET);
      values.remove(AdditionalMetadata.SECRET_KEY_UUID_KEY);
    }
  }
}
