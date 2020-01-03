package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.GcpKmsService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SecretManagerImplTest extends WingsBaseTest {
  private Account account;
  private GcpKmsConfig gcpKmsConfig;
  @Mock private GcpKmsService gcpKmsService;
  @Mock private AccountService accountService;
  @Mock private FileService fileService;
  @Inject @InjectMocks private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject @InjectMocks private SecretManager secretManager;

  @Before
  public void setup() {
    account = getAccount(AccountType.PAID);
    account.setLocalEncryptionEnabled(false);
    wingsPersistence.save(account);
    List<Account> accounts = new ArrayList<>();
    accounts.add(account);
    User user = User.Builder.anUser()
                    .withName("Hello")
                    .withUuid(UUIDGenerator.generateUuid())
                    .withEmail("hello@harness.io")
                    .withAccounts(accounts)
                    .build();
    UserThreadLocal.set(user);

    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);
    String result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    assertThat(result).isNotNull();
    gcpKmsConfig.setUuid(result);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncrypt_GCPKMS() {
    String secretName = "secretName";
    String secretValue = "secretValue";

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .build();

    when(gcpKmsService.encrypt(
             eq(secretValue), eq(account.getUuid()), any(GcpKmsConfig.class), any(EncryptedData.class)))
        .thenReturn(encryptedData);
    EncryptedData savedEncryptedData = secretManager.encrypt(
        account.getUuid(), SettingVariableTypes.GCP_KMS, secretValue.toCharArray(), null, null, secretName, null);
    assertThat(savedEncryptedData.getKmsId()).isEqualTo(gcpKmsConfig.getUuid());
    assertThat(savedEncryptedData.getEncryptionType()).isEqualTo(gcpKmsConfig.getEncryptionType());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testChangeSecretManager_fromGCPKMS_toGCPKMS() throws IOException {
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig1 = new GcpKmsConfig("name1", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig1.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig1.setAccountId(account.getUuid());
    gcpKmsConfig1.setDefault(true);

    String result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig1);
    assertThat(result).isNotNull();
    gcpKmsConfig1.setUuid(result);

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .build();

    String encryptedDataId = wingsPersistence.save(encryptedData);
    encryptedData.setUuid(encryptedDataId);

    when(gcpKmsService.decrypt(any(EncryptedData.class), eq(account.getUuid()), any(GcpKmsConfig.class)))
        .thenReturn(encryptedData.getEncryptedValue());
    when(gcpKmsService.encrypt(eq(String.valueOf(encryptedData.getEncryptedValue())), eq(account.getUuid()),
             any(GcpKmsConfig.class), any(EncryptedData.class)))
        .thenReturn(encryptedData);
    secretManager.changeSecretManager(account.getUuid(), encryptedDataId, gcpKmsConfig.getEncryptionType(),
        gcpKmsConfig.getUuid(), gcpKmsConfig1.getEncryptionType(), gcpKmsConfig1.getUuid());

    verify(gcpKmsService, times(1))
        .encrypt(eq(String.valueOf(encryptedData.getEncryptedValue())), eq(account.getUuid()), any(GcpKmsConfig.class),
            any(EncryptedData.class));
    verify(gcpKmsService, times(1)).decrypt(any(EncryptedData.class), eq(account.getUuid()), any(GcpKmsConfig.class));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testChangeFileSecretManager_fromGCPKMS_toGCPKMS() throws IOException {
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig1 = new GcpKmsConfig("name1", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig1.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig1.setAccountId(account.getUuid());
    gcpKmsConfig1.setDefault(true);

    String result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig1);
    assertThat(result).isNotNull();
    gcpKmsConfig1.setUuid(result);

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.CONFIG_FILE)
                                      .build();

    String encryptedDataId = wingsPersistence.save(encryptedData);
    encryptedData.setUuid(encryptedDataId);

    when(gcpKmsService.encryptFile(eq(account.getUuid()), any(GcpKmsConfig.class), eq(encryptedData.getName()), any(),
             any(EncryptedData.class)))
        .thenReturn(encryptedData);
    secretManager.changeSecretManager(account.getUuid(), encryptedDataId, gcpKmsConfig.getEncryptionType(),
        gcpKmsConfig.getUuid(), gcpKmsConfig1.getEncryptionType(), gcpKmsConfig1.getUuid());

    verify(gcpKmsService, times(1))
        .encryptFile(eq(account.getUuid()), any(GcpKmsConfig.class), eq(encryptedData.getName()), any(),
            any(EncryptedData.class));
  }
}
