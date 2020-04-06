package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagerConfigService;

import java.util.List;

public class SecretManagerConfigServiceTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Mock FeatureFlagService featureFlagService;
  @Mock KmsService kmsService;
  @Mock GcpSecretsManagerService gcpSecretsManagerService;
  @Inject @InjectMocks SecretManagerConfigService secretManagerConfigService;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetGlobalSecretManager_shouldReturnAwsKms_FeatureFlagEnabled() {
    String accountId = "accountId";
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String configId = wingsPersistence.save(kmsConfig);
    kmsConfig.setUuid(configId);

    when(featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)).thenReturn(true);
    when(kmsService.getKmsConfig(accountId, configId)).thenReturn(kmsConfig);

    SecretManagerConfig secretManagerConfig = secretManagerConfigService.getGlobalSecretManager(accountId);
    assertThat(secretManagerConfig).isNotNull();
    assertThat(secretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.KMS);
    assertThat(secretManagerConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);

    verify(featureFlagService, times(1)).isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId);
    verify(kmsService, times(1)).decryptKmsConfigSecrets(accountId, kmsConfig, false);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetGlobalSecretManager_shouldReturnGcpKms_FeatureFlagEnabled() {
    String accountId = "accountId";
    GcpKmsConfig gcpKmsConfig = getGcpKmsConfig();
    gcpKmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String configId = wingsPersistence.save(gcpKmsConfig);
    gcpKmsConfig.setUuid(configId);

    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String kmsConfigId = wingsPersistence.save(kmsConfig);
    kmsConfig.setUuid(kmsConfigId);

    when(featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)).thenReturn(true);
    when(gcpSecretsManagerService.getGcpKmsConfig(accountId, configId)).thenReturn(gcpKmsConfig);

    SecretManagerConfig secretManagerConfig = secretManagerConfigService.getGlobalSecretManager(accountId);
    assertThat(secretManagerConfig).isNotNull();
    assertThat(secretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.GCP_KMS);
    assertThat(secretManagerConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);

    verify(featureFlagService, times(1)).isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId);
    verify(gcpSecretsManagerService, times(1)).decryptGcpConfigSecrets(gcpKmsConfig, false);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetGlobalSecretManager_shouldReturnAwsKms_FeatureFlagDisabled() {
    String accountId = "accountId";

    GcpKmsConfig gcpKmsConfig = getGcpKmsConfig();
    gcpKmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String gcpKmsConfigId = wingsPersistence.save(gcpKmsConfig);
    gcpKmsConfig.setUuid(gcpKmsConfigId);

    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String kmsConfigId = wingsPersistence.save(kmsConfig);
    kmsConfig.setUuid(kmsConfigId);

    when(featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)).thenReturn(false);
    when(kmsService.getKmsConfig(accountId, kmsConfigId)).thenReturn(kmsConfig);

    SecretManagerConfig secretManagerConfig = secretManagerConfigService.getGlobalSecretManager(accountId);
    assertThat(secretManagerConfig).isNotNull();
    assertThat(secretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.KMS);
    assertThat(secretManagerConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);

    verify(featureFlagService, times(1)).isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId);
    verify(kmsService, times(1)).decryptKmsConfigSecrets(accountId, kmsConfig, false);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetDefaultGlobalSecretManager_FeatureFlagEnabled() {
    String accountId = "accountId";

    GcpKmsConfig gcpKmsConfig = getGcpKmsConfig();
    gcpKmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String gcpKmsConfigId = wingsPersistence.save(gcpKmsConfig);
    gcpKmsConfig.setUuid(gcpKmsConfigId);

    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String kmsConfigId = wingsPersistence.save(kmsConfig);
    kmsConfig.setUuid(kmsConfigId);

    when(featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)).thenReturn(true);
    List<SecretManagerConfig> secretManagerConfigList = secretManagerConfigService.listSecretManagers(accountId, true);
    assertThat(secretManagerConfigList).isNotNull();
    assertThat(secretManagerConfigList.size()).isEqualTo(1);
    SecretManagerConfig returnedSecretManagerConfig = secretManagerConfigList.get(0);
    assertThat(returnedSecretManagerConfig).isNotNull();
    assertThat(returnedSecretManagerConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);
    assertThat(returnedSecretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.GCP_KMS);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetDefaultGlobalSecretManager_FeatureFlagDisabled() {
    String accountId = "accountId";

    GcpKmsConfig gcpKmsConfig = getGcpKmsConfig();
    gcpKmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String gcpKmsConfigId = wingsPersistence.save(gcpKmsConfig);
    gcpKmsConfig.setUuid(gcpKmsConfigId);

    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String kmsConfigId = wingsPersistence.save(kmsConfig);
    kmsConfig.setUuid(kmsConfigId);

    when(featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)).thenReturn(false);
    List<SecretManagerConfig> secretManagerConfigList = secretManagerConfigService.listSecretManagers(accountId, true);
    assertThat(secretManagerConfigList).isNotNull();
    assertThat(secretManagerConfigList.size()).isEqualTo(1);
    SecretManagerConfig returnedSecretManagerConfig = secretManagerConfigList.get(0);
    assertThat(returnedSecretManagerConfig).isNotNull();
    assertThat(returnedSecretManagerConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);
    assertThat(returnedSecretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.KMS);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testListSecresManager_AccountLocalEncryptionEnabled() {
    Account account = getAccount(AccountType.PAID);
    account.setLocalEncryptionEnabled(true);
    wingsPersistence.save(account);

    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    String kmsConfigId = wingsPersistence.save(kmsConfig);
    kmsConfig.setUuid(kmsConfigId);

    List<SecretManagerConfig> secretManagerConfigList =
        secretManagerConfigService.listSecretManagers(account.getUuid(), true);
    assertThat(secretManagerConfigList).hasSize(1);
    assertThat(secretManagerConfigList.get(0).getEncryptionType()).isEqualTo(EncryptionType.LOCAL);

    account.setLocalEncryptionEnabled(false);
    wingsPersistence.save(account);

    secretManagerConfigList = secretManagerConfigService.listSecretManagers(account.getUuid(), true);
    assertThat(secretManagerConfigList).hasSize(1);
    assertThat(secretManagerConfigList.get(0).getEncryptionType()).isEqualTo(EncryptionType.KMS);
  }
}
