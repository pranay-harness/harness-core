package io.harness.cron;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.SMCoreTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.FeatureName;
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
    when(featureFlagHelperService.isEnabled(ACCOUNT_ID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(true);
    createEncryptedRecords(10);

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

  private void createEncryptedRecords(int numOfRecords) {
    for (int i = 0; i < numOfRecords; i++) {
      String value = RandomStringUtils.randomAlphabetic(10);
      String id = secretsDao.saveSecret(
          mapEncryptedRecordToEncryptedData(localEncryptor.encryptSecret(ACCOUNT_ID, value, null)));
      Optional<EncryptedData> fetchedEncryptedDataOptional = secretsDao.getSecretById(ACCOUNT_ID, id);
      if (fetchedEncryptedDataOptional.isPresent()) {
        System.out.println(fetchedEncryptedDataOptional.get());
      }
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
    pageRequest.setLimit("1000");
    pageRequest.setOffset("0");
    pageRequest.addFilter(EncryptedDataKeys.accountId, SearchFilter.Operator.EQ, ACCOUNT_ID);
    return pageRequest;
  }
}
