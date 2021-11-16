package io.harness.cron;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.SMCoreTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.repositories.LocalEncryptionMigrationInfoRepository;
import io.harness.rule.Owner;
import io.harness.secrets.SecretsDaoImpl;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.utils.featureflaghelper.FeatureFlagHelperService;

import com.google.inject.Inject;
import java.util.Optional;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PL)
public class LocalMultiCryptoModeEncryptionMigrationTest extends SMCoreTestBase {
  @InjectMocks @Inject LocalMultiCryptoModeEncryptionMigrationHandler localMultiCryptoModeEncryptionMigrationHandler;
  @Mock FeatureFlagHelperService featureFlagHelperService;
  @Inject LocalEncryptor localEncryptor;
  @Inject LocalEncryptionMigrationInfoRepository localEncryptionMigrationInfoRepository;
  @Inject SecretsDaoImpl secretsDao;

  private static final String ACCOUNT_ID = "ACCOUNT_ID";

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testWhenLastMigrationStateIsNotPresent() {
    createEncryptedRecords(10);
    when(featureFlagHelperService.isEnabled(ACCOUNT_ID, FeatureName.LOCAL_MULTI_CRYPTO_MODE)).thenReturn(true);

    localMultiCryptoModeEncryptionMigrationHandler.performMigration(ACCOUNT_ID);

    assertThat(localEncryptionMigrationInfoRepository.findByAccountIdAndMode(
                   ACCOUNT_ID, FeatureName.LOCAL_MULTI_CRYPTO_MODE.name()))
        .isNotEqualTo(Optional.empty());
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
        .encryptionType(encryptedRecord.getEncryptionType())
        .build();
  }
}
