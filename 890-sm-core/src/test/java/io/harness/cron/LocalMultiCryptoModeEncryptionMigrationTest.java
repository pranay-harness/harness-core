package io.harness.cron;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.SMCoreTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.FeatureName;
import io.harness.beans.LocalEncryptionMigrationInfo;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.category.element.UnitTests;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.repositories.LocalEncryptionMigrationInfoRepository;
import io.harness.rule.Owner;
import io.harness.secrets.SecretsDaoImpl;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedMech;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import io.harness.utils.featureflaghelper.FeatureFlagHelperService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PL)
public class LocalMultiCryptoModeEncryptionMigrationTest extends SMCoreTestBase {
  @Mock FeatureFlagHelperService featureFlagHelperService;
  @InjectMocks @Inject LocalEncryptor localEncryptor;
  @InjectMocks @Inject LocalMultiCryptoModeEncryptionMigrationHandler localMultiCryptoModeEncryptionMigrationHandler;
  @Inject LocalEncryptionMigrationInfoRepository localEncryptionMigrationInfoRepository;
  @Inject SecretsDaoImpl secretsDao;

  private static final String ACCOUNT_ID = "ACCOUNT_ID";

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testWhenLastMigrationStateIsNotPresent() {
    createEncryptedRecords(10);
    testIfPresentRecordsAreNotMigrated();

    when(featureFlagHelperService.isEnabled(ACCOUNT_ID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(true);

    localMultiCryptoModeEncryptionMigrationHandler.performMigration(ACCOUNT_ID);

    assertThat(localEncryptionMigrationInfoRepository.findByAccountIdAndMode(
                   ACCOUNT_ID, FeatureName.LOCAL_MULTI_CRYPTO_MODE.name()))
        .isNotEqualTo(Optional.empty());
    PageResponse<EncryptedData> encryptedDataPageResponse = secretsDao.listSecrets(getPageRequest());
    List<EncryptedData> encryptedDataList = encryptedDataPageResponse.getResponse();
    encryptedDataList.forEach(encryptedData -> {
      assertThat(EncryptedMech.MULTI_CRYPTO.equals(encryptedData.getEncryptedMech())).isTrue();
      assertThat(encryptedData.getEncryptedMech()).isEqualTo(EncryptedMech.MULTI_CRYPTO);
      assertThat(encryptedData.getEncryptedValue()).isNotNull();
      assertThat(encryptedData.getEncryptionKey()).isNotNull();
      assertThat(encryptedData.getAdditionalMetadata().getValues().get(AdditionalMetadata.SECRET_KEY_UUID_KEY))
          .isNotNull();
      assertThat(encryptedData.getAdditionalMetadata().getValues().get(AdditionalMetadata.AWS_ENCRYPTED_SECRET))
          .isNotNull();
    });
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testWhenLastMigrationStateIsPresent() {
    int recordsToBeMigrated = 20;
    int recordsNotToBeMigrated = 15;

    createEncryptedRecords(recordsToBeMigrated);
    long lastMigratedRecordCreatedAtTimestamp = 100;
    createEncryptedRecordsWithCreatedAtBeforeGivenTimestamp(
        recordsNotToBeMigrated, lastMigratedRecordCreatedAtTimestamp);
    testIfPresentRecordsAreNotMigrated();

    when(featureFlagHelperService.isEnabled(ACCOUNT_ID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(true);

    localEncryptionMigrationInfoRepository.save(
        LocalEncryptionMigrationInfo.builder()
            .accountId(ACCOUNT_ID)
            .uuid("uuid")
            .lastMigratedRecordCreatedAtTimestamp(lastMigratedRecordCreatedAtTimestamp)
            .lastMigratedRecordId("just-for-reference")
            .mode(FeatureName.LOCAL_MULTI_CRYPTO_MODE.name())
            .build());

    localMultiCryptoModeEncryptionMigrationHandler.performMigration(ACCOUNT_ID);

    Optional<LocalEncryptionMigrationInfo> localEncryptionMigrationInfo =
        localEncryptionMigrationInfoRepository.findByAccountIdAndMode(
            ACCOUNT_ID, FeatureName.LOCAL_MULTI_CRYPTO_MODE.name());

    assertThat(localEncryptionMigrationInfo.isPresent()).isTrue();
    assertThat(localEncryptionMigrationInfo.get().getLastMigratedRecordCreatedAtTimestamp())
        .isNotEqualTo(lastMigratedRecordCreatedAtTimestamp);
    assertThat(getMigratedRecordsCount()).isEqualTo(recordsToBeMigrated);
    assertThat(getNonMigratedRecordsCount()).isEqualTo(recordsNotToBeMigrated);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testIfMigratedRecordsCountIsConsistent() {
    int page_size = Integer.parseInt(LocalEncryptionMigrationHandler.PAGE_SIZE);
    createEncryptedRecords(5 * page_size);
    when(featureFlagHelperService.isEnabled(ACCOUNT_ID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(true);

    localMultiCryptoModeEncryptionMigrationHandler.performMigration(ACCOUNT_ID);
    assertThat(getMigratedRecordsCount()).isEqualTo(1 * page_size);
    assertThat(getNonMigratedRecordsCount()).isEqualTo(4 * page_size);

    localMultiCryptoModeEncryptionMigrationHandler.performMigration(ACCOUNT_ID);
    assertThat(getMigratedRecordsCount()).isEqualTo(2 * page_size);
    assertThat(getNonMigratedRecordsCount()).isEqualTo(3 * page_size);

    localMultiCryptoModeEncryptionMigrationHandler.performMigration(ACCOUNT_ID);
    assertThat(getMigratedRecordsCount()).isEqualTo(3 * page_size);
    assertThat(getNonMigratedRecordsCount()).isEqualTo(2 * page_size);

    localMultiCryptoModeEncryptionMigrationHandler.performMigration(ACCOUNT_ID);
    assertThat(getMigratedRecordsCount()).isEqualTo(4 * page_size);
    assertThat(getNonMigratedRecordsCount()).isEqualTo(1 * page_size);

    localMultiCryptoModeEncryptionMigrationHandler.performMigration(ACCOUNT_ID);
    assertThat(getMigratedRecordsCount()).isEqualTo(5 * page_size);
    assertThat(getNonMigratedRecordsCount()).isEqualTo(0 * page_size);
  }

  private void createEncryptedRecords(int numOfRecords) {
    for (int i = 0; i < numOfRecords; i++) {
      String value = RandomStringUtils.randomAlphabetic(10);
      secretsDao.saveSecret(mapEncryptedRecordToEncryptedData(localEncryptor.encryptSecret(ACCOUNT_ID, value, null)));
    }
    getMigratedRecordsCount();
  }

  private void createEncryptedRecordsWithCreatedAtBeforeGivenTimestamp(int numOfRecords, long thresholdCreatedAt) {
    for (int i = 0; i < numOfRecords; i++) {
      String value = RandomStringUtils.randomAlphabetic(10);
      EncryptedData encryptedData =
          mapEncryptedRecordToEncryptedData(localEncryptor.encryptSecret(ACCOUNT_ID, value, null));
      encryptedData.setCreatedAt(thresholdCreatedAt - i - 1);
      secretsDao.saveSecret(encryptedData);
    }
  }

  private EncryptedData mapEncryptedRecordToEncryptedData(EncryptedRecord encryptedRecord) {
    return EncryptedData.builder()
        .accountId(ACCOUNT_ID)
        .encryptedMech(encryptedRecord.getEncryptedMech())
        .encryptedValue(encryptedRecord.getEncryptedValue())
        .encryptedValueBytes(encryptedRecord.getEncryptedValueBytes())
        .encryptionKey(encryptedRecord.getEncryptionKey())
        .additionalMetadata(encryptedRecord.getAdditionalMetadata())
        .backupEncryptedValue(encryptedRecord.getBackupEncryptedValue())
        .backupEncryptionKey(encryptedRecord.getBackupEncryptionKey())
        .backupEncryptionType(encryptedRecord.getBackupEncryptionType())
        .backupKmsId(encryptedRecord.getBackupKmsId())
        .encryptionType(EncryptionType.LOCAL)
        .build();
  }

  private PageRequest<EncryptedData> getPageRequest() {
    PageRequest<EncryptedData> pageRequest = new PageRequest<>();
    pageRequest.setLimit("100000");
    pageRequest.setOffset("0");
    pageRequest.addFilter(EncryptedDataKeys.accountId, SearchFilter.Operator.EQ, ACCOUNT_ID);
    return pageRequest;
  }

  private void testIfPresentRecordsAreNotMigrated() {
    PageResponse<EncryptedData> encryptedDataPageResponse = secretsDao.listSecrets(getPageRequest());
    List<EncryptedData> encryptedDataList = encryptedDataPageResponse.getResponse();
    encryptedDataList.forEach(encryptedData -> {
      assertThat(encryptedData.getEncryptedMech()).isNull();
      assertThat(encryptedData.getEncryptedValue()).isNotNull();
      assertThat(encryptedData.getEncryptionKey()).isNotNull();
      assertThat(encryptedData.getAdditionalMetadata().getAwsEncryptedSecret()).isNull();
      assertThat(encryptedData.getAdditionalMetadata().getSecretKeyUuid()).isNull();
    });
  }

  private int getMigratedRecordsCount() {
    PageResponse<EncryptedData> encryptedDataPageResponse = secretsDao.listSecrets(getPageRequest());
    List<EncryptedData> encryptedDataList = encryptedDataPageResponse.getResponse();
    return (int) encryptedDataList.stream()
        .filter(encryptedData -> encryptedData.getEncryptedMech() == EncryptedMech.MULTI_CRYPTO)
        .count();
  }

  private int getNonMigratedRecordsCount() {
    PageResponse<EncryptedData> encryptedDataPageResponse = secretsDao.listSecrets(getPageRequest());
    List<EncryptedData> encryptedDataList = encryptedDataPageResponse.getResponse();
    return (int) encryptedDataList.stream()
        .filter(encryptedData -> encryptedData.getEncryptedMech() != EncryptedMech.MULTI_CRYPTO)
        .count();
  }
}
