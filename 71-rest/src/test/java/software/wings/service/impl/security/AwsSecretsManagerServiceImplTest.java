package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.eraro.ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.security.SimpleEncryption.CHARSET;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.beans.EncryptedData;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoSerializer;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.features.api.PremiumFeature;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingVariableTypes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AwsSecretsManagerServiceImplTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Inject @Spy @InjectMocks private AwsSecretsManagerService awsSecretsManagerService;
  @Mock private AccountService accountService;
  @Mock private PremiumFeature secretsManagementFeature;
  @Mock private DelegateProxyFactory delegateProxyFactory;

  @Inject KryoSerializer kryoSerializer;

  private String accountId;

  @Rule public TemporaryFolder tempDirectory = new TemporaryFolder();

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    initMocks(this);

    Account account = getAccount(AccountType.PAID);
    accountId = account.getUuid();
    when(accountService.get(accountId)).thenReturn(account);

    doNothing().when(awsSecretsManagerService).validateSecretsManagerConfig(any());
    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(true);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void saveAwsSecretManagerConfig_shouldPass() {
    AwsSecretsManagerConfig awsSecretManagerConfig = getAwsSecretManagerConfig();
    awsSecretManagerConfig.setAccountId(accountId);

    String savedConfigId = awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, awsSecretManagerConfig);
    assertEquals(awsSecretManagerConfig.getName(),
        awsSecretsManagerService.getAwsSecretsManagerConfig(accountId, savedConfigId).getName());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void saveAwsSecretManagerConfigIfFeatureNotAvailable_shouldThrowException() {
    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(false);

    AwsSecretsManagerConfig awsSecretManagerConfig = getAwsSecretManagerConfig();
    awsSecretManagerConfig.setAccountId(accountId);

    try {
      awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, awsSecretManagerConfig);
      fail("Aws Secret Manager Config Saved when Secrets Management Feature is Unavailable !!");
    } catch (Exception ex) {
      assertTrue(ex instanceof InvalidRequestException);
    }
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void updateAwsSecretManagerConfigNonSecretKey_shouldPass() {
    AwsSecretsManagerConfig awsSecretManagerConfig = getAwsSecretManagerConfig();
    awsSecretManagerConfig.setAccountId(accountId);

    String savedConfigId =
        awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, kryoSerializer.clone(awsSecretManagerConfig));

    AwsSecretsManagerConfig updatedAwsSecretManagerConfig =
        awsSecretsManagerService.getAwsSecretsManagerConfig(accountId, savedConfigId);

    updatedAwsSecretManagerConfig.setUuid(savedConfigId);
    updatedAwsSecretManagerConfig.setName("UpdatedConfig");
    updatedAwsSecretManagerConfig.maskSecrets();

    awsSecretsManagerService.saveAwsSecretsManagerConfig(
        accountId, kryoSerializer.clone(updatedAwsSecretManagerConfig));

    assertEquals(
        "UpdatedConfig", awsSecretsManagerService.getAwsSecretsManagerConfig(accountId, savedConfigId).getName());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void updateAwsSecretManagerConfigSecretKey_shouldPass() {
    AwsSecretsManagerConfig awsSecretManagerConfig = getAwsSecretManagerConfig();
    awsSecretManagerConfig.setAccountId(accountId);

    String savedConfigId =
        awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, kryoSerializer.clone(awsSecretManagerConfig));

    AwsSecretsManagerConfig savedAwsSecretManagerConfig =
        awsSecretsManagerService.getAwsSecretsManagerConfig(accountId, savedConfigId);

    savedAwsSecretManagerConfig.setUuid(savedConfigId);
    savedAwsSecretManagerConfig.setSecretKey("UpdatedSecretKey");

    awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, kryoSerializer.clone(savedAwsSecretManagerConfig));

    assertEquals("UpdatedSecretKey",
        awsSecretsManagerService.getAwsSecretsManagerConfig(accountId, savedConfigId).getSecretKey());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void deleteAwsSecretManagerConfigWithNoEncryptedSecrets_shouldPass() {
    AwsSecretsManagerConfig awsSecretManagerConfig = getAwsSecretManagerConfig();
    awsSecretManagerConfig.setAccountId(accountId);

    String savedConfigId =
        awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, kryoSerializer.clone(awsSecretManagerConfig));

    assertNotNull(secretManager.getSecretManager(accountId, savedConfigId));

    awsSecretsManagerService.deleteAwsSecretsManagerConfig(accountId, savedConfigId);

    assertNull(secretManager.getSecretManager(accountId, savedConfigId));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void deleteAwsSecretManagerConfigWithEncryptedSecrets_shouldFail() {
    AwsSecretsManagerConfig awsSecretManagerConfig = getAwsSecretManagerConfig();
    awsSecretManagerConfig.setAccountId(accountId);

    String savedConfigId =
        awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, kryoSerializer.clone(awsSecretManagerConfig));

    wingsPersistence.save(EncryptedData.builder()
                              .accountId(accountId)
                              .encryptionType(EncryptionType.AWS_SECRETS_MANAGER)
                              .kmsId(savedConfigId)
                              .build());

    try {
      awsSecretsManagerService.deleteAwsSecretsManagerConfig(accountId, savedConfigId);
      fail("Aws Secret Manager Config Containing Secrets Deleted");
    } catch (WingsException ex) {
      assertEquals(AWS_SECRETS_MANAGER_OPERATION_ERROR, ex.getCode());
    }
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testEncryptSecret() {
    String secretName = "TestSecret";
    String secretValue = "TestValue";
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    AwsSecretsManagerConfig config = mock(AwsSecretsManagerConfig.class);
    EncryptedData encryptedData = mock(EncryptedData.class);

    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);

    awsSecretsManagerService.encrypt(
        secretName, secretValue, accountId, SettingVariableTypes.AWS_SECRETS_MANAGER, config, encryptedData);

    verify(secretManagementDelegateService, times(1))
        .encrypt(secretName, secretValue, accountId, SettingVariableTypes.AWS_SECRETS_MANAGER, config, encryptedData);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testDecryptSecret() {
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    AwsSecretsManagerConfig config = mock(AwsSecretsManagerConfig.class);
    EncryptedData encryptedData = mock(EncryptedData.class);
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);

    awsSecretsManagerService.decrypt(encryptedData, accountId, config);

    verify(secretManagementDelegateService, times(1)).decrypt(encryptedData, config);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testDeleteSecret() {
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    AwsSecretsManagerConfig config = mock(AwsSecretsManagerConfig.class);
    String path = "TestPath";
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);

    awsSecretsManagerService.deleteSecret(accountId, path, config);

    verify(secretManagementDelegateService, times(1)).deleteSecret(path, config);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testEncryptFile() {
    String secretName = "TestSecretName";
    String fileContent = "SampleFileContent";
    byte[] fileContentBytes = fileContent.getBytes(StandardCharsets.UTF_8);
    String base64String = new String(encodeBase64ToByteArray(fileContentBytes), CHARSET);

    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    AwsSecretsManagerConfig config = mock(AwsSecretsManagerConfig.class);
    EncryptedData inputEncryptedData = mock(EncryptedData.class);
    EncryptedData outputEncryptedData = mock(EncryptedData.class);

    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);
    when(secretManagementDelegateService.encrypt(
             secretName, base64String, accountId, SettingVariableTypes.CONFIG_FILE, config, inputEncryptedData))
        .thenReturn(outputEncryptedData);

    EncryptedData encryptedData =
        awsSecretsManagerService.encryptFile(accountId, config, secretName, fileContentBytes, inputEncryptedData);

    assertSame(encryptedData, outputEncryptedData);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testDecryptFile() throws IOException {
    String str = "TestString!";
    String base64EncodedStr = encodeBase64(str);

    AwsSecretsManagerConfig awsSecretManagerConfig = getAwsSecretManagerConfig();
    String configId = awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, awsSecretManagerConfig);
    AwsSecretsManagerConfig savedConfig = awsSecretsManagerService.getAwsSecretsManagerConfig(accountId, configId);

    EncryptedData encryptedData = mock(EncryptedData.class);
    when(encryptedData.getKmsId()).thenReturn(configId);
    when(encryptedData.isBase64Encoded()).thenReturn(true);

    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);

    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);
    when(secretManagementDelegateService.decrypt(encryptedData, savedConfig))
        .thenReturn(base64EncodedStr.toCharArray());

    File file = tempDirectory.newFile();

    awsSecretsManagerService.decryptFile(file, accountId, encryptedData);

    assertTrue(file.exists());
    assertEquals(FileUtils.readFileToString(file, StandardCharsets.UTF_8), str);
  }
}
