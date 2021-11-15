package io.harness.cron;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.SMCoreTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.repositories.LocalEncryptionMigrationInfoRepository;
import io.harness.rule.Owner;
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
      localEncryptor.encryptSecret(ACCOUNT_ID, value, null);
    }
  }
}
