package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.security.SimpleEncryption.CHARSET;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
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
import software.wings.beans.AzureVaultConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AzureVaultServiceImplTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Inject @Spy @InjectMocks private AzureVaultService azureVaultService;
  @Inject @Spy @InjectMocks private AzureSecretsManagerService azureSecretsManagerService;
  @Mock private AccountService accountService;
  @Mock private DelegateProxyFactory delegateProxyFactory;

  private String accountId;

  @Rule public TemporaryFolder tempDirectory = new TemporaryFolder();

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    initMocks(this);

    Account account = getAccount(AccountType.PAID);
    accountId = account.getUuid();
    when(accountService.get(accountId)).thenReturn(account);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testEncryptSecret() {
    String secretName = "TestSecret";
    String secretValue = "TestValue";
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    AzureVaultConfig config = mock(AzureVaultConfig.class);
    EncryptedData encryptedData = mock(EncryptedData.class);

    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);

    azureVaultService.encrypt(secretName, secretValue, accountId, SettingVariableTypes.AZURE, config, encryptedData);

    verify(secretManagementDelegateService, times(1))
        .encrypt(secretName, secretValue, accountId, SettingVariableTypes.AZURE, config, encryptedData);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testDecryptSecret() {
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    AzureVaultConfig config = mock(AzureVaultConfig.class);
    EncryptedData encryptedData = mock(EncryptedData.class);
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);

    azureVaultService.decrypt(encryptedData, accountId, config);

    verify(secretManagementDelegateService, times(1)).decrypt(encryptedData, config);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testDeleteSecret() {
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    AzureVaultConfig config = mock(AzureVaultConfig.class);
    String secretName = "TestSecretName";
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);

    azureVaultService.delete(accountId, secretName, config);

    verify(secretManagementDelegateService, times(1)).delete(config, secretName);
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
    AzureVaultConfig config = mock(AzureVaultConfig.class);
    EncryptedData inputEncryptedData = mock(EncryptedData.class);
    EncryptedData outputEncryptedData = mock(EncryptedData.class);

    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);
    when(secretManagementDelegateService.encrypt(
             secretName, base64String, accountId, SettingVariableTypes.CONFIG_FILE, config, inputEncryptedData))
        .thenReturn(outputEncryptedData);

    EncryptedData encryptedData =
        azureVaultService.encryptFile(accountId, config, secretName, fileContentBytes, inputEncryptedData);

    assertSame(encryptedData, outputEncryptedData);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testDecryptFile() throws IOException {
    String str = "TestString!";
    String base64EncodedStr = encodeBase64(str);

    AzureVaultConfig config = getAzureVaultConfig();
    String configId = azureSecretsManagerService.saveAzureSecretsManagerConfig(accountId, config);
    AzureVaultConfig savedConfig = azureSecretsManagerService.getEncryptionConfig(accountId, configId);

    EncryptedData encryptedData = mock(EncryptedData.class);
    when(encryptedData.getKmsId()).thenReturn(configId);
    when(encryptedData.isBase64Encoded()).thenReturn(true);

    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);

    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);
    when(secretManagementDelegateService.decrypt(encryptedData, savedConfig))
        .thenReturn(base64EncodedStr.toCharArray());

    File file = tempDirectory.newFile();

    azureVaultService.decryptFile(file, accountId, encryptedData);

    assertTrue(file.exists());
    assertEquals(FileUtils.readFileToString(file, StandardCharsets.UTF_8), str);
  }
}
