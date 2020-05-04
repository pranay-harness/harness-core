package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.io.Files;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.BaseFile;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.User;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.GcpKmsService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Files.class)
@PowerMockIgnore({"javax.security.*", "javax.crypto.*", "javax.net.*"})
public class GcpKmsServiceTest extends WingsBaseTest {
  @Mock private FileService fileService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private AccountService accountService;
  @Mock private GcpKmsService mockGcpKmsService;
  @Inject @InjectMocks private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject @InjectMocks private GcpKmsService gcpKmsService;
  private Account account;
  private GcpKmsConfig gcpKmsConfig;

  @Rule public TemporaryFolder tempDirectory = new TemporaryFolder();

  @Before
  public void setup() {
    initMocks(this);
    account = getAccount(AccountType.PAID);
    account.setLocalEncryptionEnabled(false);
    wingsPersistence.save(account);
    List<Account> accounts = new ArrayList<>();
    accounts.add(account);
    User user = User.Builder.anUser()
                    .name("Hello")
                    .uuid(UUIDGenerator.generateUuid())
                    .email("hello@harness.io")
                    .accounts(accounts)
                    .build();
    UserThreadLocal.set(user);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(mockGcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();
    gcpKmsConfig = new GcpKmsConfig("name", "projectId", "us", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setDefault(true);
    gcpKmsConfig.setAccountId(account.getUuid());

    String configId = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    assertThat(configId).isNotNull();
    gcpKmsConfig = gcpSecretsManagerService.getGcpKmsConfig(account.getUuid(), configId);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncrypt() {
    String secretValue = "TestValue";
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);

    gcpKmsService.encrypt(secretValue, account.getUuid(), gcpKmsConfig, null);
    verify(secretManagementDelegateService, times(1)).encrypt(secretValue, account.getUuid(), gcpKmsConfig, null);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecrypt() {
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .backupEncryptedValue("Dummy value".toCharArray())
                                      .backupEncryptionKey("Dummy key")
                                      .backupKmsId("kmsId")
                                      .encryptionType(EncryptionType.KMS)
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .build();
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);
    gcpKmsService.decrypt(encryptedData, account.getUuid(), gcpKmsConfig);
    verify(secretManagementDelegateService, times(1)).decrypt(encryptedData, gcpKmsConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptFile() {
    String secretName = "TestSecretName";
    String fileContent = "SampleFileContent";
    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);
    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .backupEncryptedValue("Dummy value".toCharArray())
                                      .backupEncryptionKey("Dummy key")
                                      .backupKmsId("kmsId")
                                      .encryptionType(EncryptionType.KMS)
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .build();
    when(secretManagementDelegateService.encrypt(anyString(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null)))
        .thenReturn(encryptedData);
    when(fileService.saveFile(any(BaseFile.class), any(ByteArrayInputStream.class), eq(CONFIGS)))
        .thenReturn(UUIDGenerator.generateUuid());

    EncryptedData savedEncryptedData = gcpKmsService.encryptFile(
        account.getUuid(), gcpKmsConfig, secretName, fileContent.getBytes(StandardCharsets.ISO_8859_1), null);
    assertThat(savedEncryptedData).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecryptFile() throws IOException {
    String str = "TestString!";
    String base64EncodedStr = encodeBase64(str);
    PowerMockito.mockStatic(Files.class);

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .backupEncryptedValue("Dummy value".toCharArray())
                                      .backupEncryptionKey("Dummy key")
                                      .backupKmsId("kmsId")
                                      .encryptionType(EncryptionType.KMS)
                                      .base64Encoded(true)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .build();

    File file = tempDirectory.newFile();
    when(Files.createTempDir()).thenReturn(file);
    when(Files.toByteArray(any())).thenReturn(String.valueOf(encryptedData.getEncryptedValue()).getBytes());

    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);
    when(secretManagementDelegateService.decrypt(encryptedData, gcpKmsConfig))
        .thenReturn(base64EncodedStr.toCharArray());

    file = gcpKmsService.decryptFile(file, account.getUuid(), encryptedData);
    assertThat(file.exists()).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDecryptStream() throws IOException {
    String str = "TestString!";
    String base64EncodedStr = encodeBase64(str);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PowerMockito.mockStatic(Files.class);

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .backupEncryptedValue("Dummy value".toCharArray())
                                      .backupEncryptionKey("Dummy key")
                                      .backupKmsId("kmsId")
                                      .encryptionType(EncryptionType.KMS)
                                      .base64Encoded(true)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .build();

    File file = tempDirectory.newFile();
    when(Files.createTempDir()).thenReturn(file);
    when(Files.toByteArray(any())).thenReturn(String.valueOf(encryptedData.getEncryptedValue()).getBytes());

    SecretManagementDelegateService secretManagementDelegateService = mock(SecretManagementDelegateService.class);
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any()))
        .thenReturn(secretManagementDelegateService);
    when(secretManagementDelegateService.decrypt(encryptedData, gcpKmsConfig))
        .thenReturn(base64EncodedStr.toCharArray());

    gcpKmsService.decryptToStream(file, account.getUuid(), encryptedData, output);
    assertThat(output.toString()).isNotEmpty();
    String outputStr = output.toString();

    assertThat(str).isEqualTo(outputStr);
  }
}
