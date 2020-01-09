package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.dl.WingsPersistence;

public class SecretManagerConfigServiceImplTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  private String kmsId = "kmsId";
  private String accountId = "accountId";
  @Inject @InjectMocks SecretManagerConfigServiceImpl secretManagetConfigServiceImpl;

  @Test(expected = SecretManagementException.class)
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_getEncryptionBySecretManagerId_whenNOValuePresent() {
    when(wingsPersistence.get(any(), any())).thenReturn(null);
    EncryptionType encryptionType = secretManagetConfigServiceImpl.getEncryptionBySecretManagerId(kmsId, accountId);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_getEncryptionBySecretManagerId() {
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();
    SecretManagerConfig secretManagerConfig =
        new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    secretManagerConfig.setEncryptionType(EncryptionType.GCP_KMS);
    when(wingsPersistence.get(any(), any())).thenReturn(secretManagerConfig);
    EncryptionType encryptionType = secretManagetConfigServiceImpl.getEncryptionBySecretManagerId(kmsId, accountId);
    assertThat(encryptionType).isEqualTo(EncryptionType.GCP_KMS);
  }
}