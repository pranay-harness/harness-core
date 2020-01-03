package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.alerts.AlertStatus.Pending;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.expression.SecretString;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.alerts.AlertStatus;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.User;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.security.GcpKmsService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GcpSecretManagerServiceTest extends WingsBaseTest {
  @Mock GcpKmsService gcpKmsService;
  @Mock HarnessUserGroupService harnessUserGroupService;
  @Mock AccountService accountService;
  @Inject @InjectMocks AlertService alertService;
  @Inject @InjectMocks GcpSecretsManagerService gcpSecretsManagerService;
  private Account account;

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
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig() {
    setup();
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    assertThat(result).isNotNull();

    verify(accountService, times(1)).get(account.getUuid());
    verify(gcpKmsService, times(1)).encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig_ShouldFail1() {
    setup();
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig =
        new GcpKmsConfig("name", "projectId@!#)(*@!", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = null;
    try {
      result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig_ShouldFail2() {
    setup();
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig =
        new GcpKmsConfig("name)(*&@!#^)", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = null;
    try {
      result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig_ShouldFail3() {
    setup();
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig =
        new GcpKmsConfig("name", "projectId", "region!@#*(&$!@", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = null;
    try {
      result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig_ShouldFail4() {
    setup();
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig =
        new GcpKmsConfig("name", "projectId", "region", "keyRingOP@#!I#!@UO", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = null;
    try {
      result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig_ShouldFail5() {
    setup();
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig =
        new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName)_@!#(*", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = null;
    try {
      result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig_ShouldFail6() {
    setup();
    char[] credentials = "{\"credentials\":".toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = null;
    try {
      result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
      logger.error(e.getMessage(), e);
    }
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig_ShouldFail7() {
    setup();
    char[] credentials = "".toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = null;
    try {
      result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
      logger.error(e.getMessage(), e);
    }
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testSaveGcpKmsConfig_ShouldFail8() {
    setup();
    char[] credentials = "".toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String result = null;
    try {
      result = gcpSecretsManagerService.saveGcpKmsConfig(UUIDGenerator.generateUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
      logger.error(e.getMessage(), e);
    }
    assertThat(result).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testUpdateGcpKmsConfig() {
    setup();
    String credentialsString = "{\"credentials\":\"abc\"}";
    char[] credentials = credentialsString.toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name1", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String configId = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    assertThat(configId).isNotNull();

    GcpKmsConfig savedGcpKmsConfig = gcpSecretsManagerService.getGcpKmsConfig(account.getUuid(), configId);
    assertThat(savedGcpKmsConfig.getName()).isEqualTo(gcpKmsConfig.getName());
    assertThat(String.valueOf(savedGcpKmsConfig.getCredentials())).isEqualTo(String.valueOf(credentials));
    assertThat(savedGcpKmsConfig.getProjectId()).isEqualTo(gcpKmsConfig.getProjectId());

    GcpKmsConfig updateGcpKmsConfig = new GcpKmsConfig(
        "name2", "projectId1", "region1", "keyRing1", "keyName1", SecretString.SECRET_MASK.toCharArray());
    updateGcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    updateGcpKmsConfig.setAccountId(account.getUuid());
    updateGcpKmsConfig.setDefault(false);
    updateGcpKmsConfig.setUuid(savedGcpKmsConfig.getUuid());

    String updatedConfigId = gcpSecretsManagerService.updateGcpKmsConfig(account.getUuid(), updateGcpKmsConfig);
    assertThat(updatedConfigId).isEqualTo(configId);

    GcpKmsConfig updatedGcpKmsConfig = gcpSecretsManagerService.getGcpKmsConfig(account.getUuid(), updatedConfigId);
    assertThat(updatedGcpKmsConfig.getName()).isEqualTo(updateGcpKmsConfig.getName());
    assertThat(updatedGcpKmsConfig.isDefault()).isEqualTo(updateGcpKmsConfig.isDefault());
    assertThat(updatedGcpKmsConfig.getProjectId()).isEqualTo(gcpKmsConfig.getProjectId());
    assertThat(String.valueOf(updatedGcpKmsConfig.getCredentials())).isEqualTo(credentialsString);

    String newCredentialsString = "{\"credentials\":\"abcd\"}";
    updateGcpKmsConfig.setCredentials(newCredentialsString.toCharArray());
    updateGcpKmsConfig.setDefault(true);

    updatedConfigId = gcpSecretsManagerService.updateGcpKmsConfig(account.getUuid(), updateGcpKmsConfig);
    assertThat(updatedConfigId).isEqualTo(configId);

    updatedGcpKmsConfig = gcpSecretsManagerService.getGcpKmsConfig(account.getUuid(), updatedConfigId);
    assertThat(updatedGcpKmsConfig.getName()).isEqualTo(updateGcpKmsConfig.getName());
    assertThat(updatedGcpKmsConfig.isDefault()).isEqualTo(updateGcpKmsConfig.isDefault());
    assertThat(updatedGcpKmsConfig.getProjectId()).isEqualTo(gcpKmsConfig.getProjectId());
    assertThat(String.valueOf(updatedGcpKmsConfig.getCredentials())).isEqualTo(newCredentialsString);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testUpdateGcpKmsConfig_ShouldFail1() {
    setup();
    String credentialsString = "{\"credentials\":\"abc\"}";
    char[] credentials = credentialsString.toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name1", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String configId = null;
    try {
      configId = gcpSecretsManagerService.updateGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }
    assertThat(configId).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testUpdateGcpKmsConfig_ShouldFail2() {
    setup();
    String credentialsString = "{\"credentials\":\"abc\"}";
    char[] credentials = credentialsString.toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name1", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);
    gcpKmsConfig.setUuid(UUIDGenerator.generateUuid());

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String configId = null;
    try {
      configId = gcpSecretsManagerService.updateGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }
    assertThat(configId).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testDeleteGcpKmsConfig() {
    setup();
    String credentialsString = "{\"credentials\":\"abc\"}";
    char[] credentials = credentialsString.toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name1", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);

    String configId = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    assertThat(configId).isNotNull();

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(configId)
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .build();
    String encryptedRecordId = wingsPersistence.save(encryptedData);

    boolean result = false;
    String alertId = UUIDGenerator.generateUuid();
    try {
      KmsSetupAlert alertData = KmsSetupAlert.builder().kmsId(gcpKmsConfig.getUuid()).build();
      Alert alert = Alert.builder()
                        .uuid(alertId)
                        .appId(GLOBAL_APP_ID)
                        .accountId(account.getUuid())
                        .type(AlertType.InvalidKMS)
                        .status(Pending)
                        .alertData(alertData)
                        .build();
      wingsPersistence.save(alert);
      result = gcpSecretsManagerService.deleteGcpKmsConfig(account.getUuid(), configId);
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    } finally {
      assertThat(result).isFalse();
    }

    wingsPersistence.delete(EncryptedData.class, encryptedRecordId);
    result = gcpSecretsManagerService.deleteGcpKmsConfig(account.getUuid(), configId);
    assertThat(result).isTrue();
    Alert alert = wingsPersistence.get(Alert.class, alertId);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.Closed);
  }
}
