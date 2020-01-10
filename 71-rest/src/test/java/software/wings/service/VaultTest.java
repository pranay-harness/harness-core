package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static io.harness.rule.OwnerRule.UTKARSH;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.settings.SettingValue.SettingVariableTypes.CONFIG_FILE;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.queue.QueueConsumer;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import io.harness.rule.Repeat;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoUtils;
import io.harness.stream.BoundedInputStream;
import io.harness.threading.Morpheus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.Activity;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigOverrideType;
import software.wings.beans.EntityType;
import software.wings.beans.Event;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.User;
import software.wings.beans.VaultConfig;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.WorkflowExecution;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.features.api.PremiumFeature;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.security.GlobalEncryptDecryptClient;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 11/3/17.
 */
@RunWith(Parameterized.class)
@Slf4j
public class VaultTest extends WingsBaseTest {
  private static String VAULT_TOKEN = UUID.randomUUID().toString();

  private final int numOfEncryptedValsForKms = 3;
  private final int numOfEncryptedValsForVault = 1;
  private int numOfEncRecords;
  @Parameter public boolean isKmsEnabled;
  @Mock private AccountService accountService;
  @Inject @InjectMocks private VaultService vaultService;
  @Inject @InjectMocks private KmsService kmsService;
  @Inject @InjectMocks private SecretManagerConfigService secretManagerConfigService;
  @Inject private QueueConsumer<KmsTransitionEvent> kmsTransitionConsumer;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SecretManagementDelegateService secretManagementDelegateService;
  @Mock private PremiumFeature secretsManagementFeature;
  @Mock protected AuditServiceHelper auditServiceHelper;
  @Mock private GlobalEncryptDecryptClient globalEncryptDecryptClient;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "UTKARSH";
  private final User user = User.Builder.anUser().email(userEmail).name(userName).build();
  private String accountId;
  private String appId;
  private String workflowExecutionId;
  private String workflowName;
  private KmsTransitionEventListener transitionEventListener;
  private String kmsId;
  private String envId;

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{true}, {false}});
  }

  @Before
  public void setup() throws IllegalAccessException {
    //    assumeTrue(getClass().getClassLoader().getResource("vault_token.txt") != null);
    initMocks(this);
    appId = UUID.randomUUID().toString();
    workflowName = UUID.randomUUID().toString();
    envId = UUID.randomUUID().toString();
    workflowExecutionId = wingsPersistence.save(WorkflowExecution.builder().name(workflowName).envId(envId).build());

    when(globalEncryptDecryptClient.encrypt(anyString(), any(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return encrypt((String) args[0], (char[]) args[1], (KmsConfig) args[2]);
    });

    when(globalEncryptDecryptClient.decrypt(anyObject(), anyString(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (KmsConfig) args[2]);
    });

    when(secretManagementDelegateService.encrypt(anyString(), anyObject(), anyString(), any(SettingVariableTypes.class),
             any(VaultConfig.class), any(EncryptedData.class)))
        .then(invocation -> {
          Object[] args = invocation.getArguments();
          return encrypt((String) args[0], (String) args[1], (String) args[2], (SettingVariableTypes) args[3],
              (VaultConfig) args[4], (EncryptedData) args[5]);
        });

    when(secretManagementDelegateService.decrypt(anyObject(), any(VaultConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedRecord) args[0], (VaultConfig) args[1]);
    });

    when(secretManagementDelegateService.encrypt(anyString(), anyObject(), anyObject())).then(invocation -> {
      Object[] args = invocation.getArguments();
      return encrypt((String) args[0], (char[]) args[1], (KmsConfig) args[2]);
    });

    when(secretManagementDelegateService.decrypt(anyObject(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedRecord) args[0], (KmsConfig) args[1]);
    });
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(secretManagementDelegateService);
    FieldUtils.writeField(vaultService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(kmsService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(vaultService, "kmsService", kmsService, true);
    FieldUtils.writeField(secretManager, "kmsService", kmsService, true);
    FieldUtils.writeField(secretManager, "vaultService", vaultService, true);
    FieldUtils.writeField(wingsPersistence, "secretManager", secretManager, true);
    FieldUtils.writeField(configService, "secretManager", secretManager, true);
    FieldUtils.writeField(encryptionService, "secretManagementDelegateService", secretManagementDelegateService, true);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);

    Account account = getAccount(AccountType.PAID);
    accountId = account.getUuid();
    when(accountService.get(accountId)).thenReturn(account);
    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(true);
    numOfEncRecords = numOfEncryptedValsForVault;
    if (isKmsEnabled) {
      final KmsConfig kmsConfig = getKmsConfig();
      kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);
      numOfEncRecords = numOfEncryptedValsForKms + numOfEncryptedValsForVault;
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void invalidConfig() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setAuthToken("invalidKey");
    vaultConfig.setAccountId(accountId);

    try {
      vaultService.saveVaultConfig(accountId, vaultConfig);
      fail("Saved invalid vault config");
    } catch (WingsException e) {
      assertThat(e.getCode()).isEqualTo(ErrorCode.VAULT_OPERATION_ERROR);
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void saveConfig() {
    if (isKmsEnabled) {
      kmsService.deleteKmsConfig(accountId, kmsId);
    }

    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setDefault(false);
    String vaultConfigId = vaultService.saveVaultConfig(accountId, vaultConfig);

    List<SecretManagerConfig> encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertThat(encryptionConfigs).hasSize(1);
    VaultConfig next = (VaultConfig) encryptionConfigs.get(0);
    assertThat(next.isDefault()).isFalse();
    assertThat(next.getAccountId()).isEqualTo(accountId);
    assertThat(String.valueOf(next.getAuthToken())).isEqualTo(SECRET_MASK);
    assertThat(next.getName()).isEqualTo(vaultConfig.getName());
    assertThat(next.getVaultUrl()).isEqualTo(vaultConfig.getVaultUrl());
    assertThat(next.isDefault()).isFalse();

    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);

    encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertThat(encryptionConfigs).hasSize(2);
    int numOfVaultDefaults = 0;
    int numOfKmsDefaults = 0;

    for (EncryptionConfig encryptionConfig : encryptionConfigs) {
      if (encryptionConfig.getEncryptionType() == EncryptionType.KMS) {
        assertThat(encryptionConfig.isDefault()).isTrue();
        assertThat(encryptionConfig.getUuid()).isEqualTo(kmsId);
        numOfKmsDefaults++;
      }

      if (encryptionConfig.getEncryptionType() == EncryptionType.VAULT) {
        assertThat(encryptionConfig.isDefault()).isFalse();
        assertThat(encryptionConfig.getUuid()).isEqualTo(vaultConfigId);
        numOfVaultDefaults++;
      }
    }

    assertThat(numOfKmsDefaults).isEqualTo(1);
    assertThat(numOfVaultDefaults).isEqualTo(1);

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setDefault(true);
    vaultConfigId = vaultService.saveVaultConfig(accountId, vaultConfig);

    encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertThat(encryptionConfigs).hasSize(3);

    numOfVaultDefaults = 0;
    numOfKmsDefaults = 0;

    for (EncryptionConfig encryptionConfig : encryptionConfigs) {
      if (encryptionConfig.getEncryptionType() == EncryptionType.KMS) {
        assertThat(encryptionConfig.isDefault()).isFalse();
        assertThat(encryptionConfig.getUuid()).isEqualTo(kmsId);
        numOfKmsDefaults++;
      }

      if (encryptionConfig.getEncryptionType() == EncryptionType.VAULT) {
        if (encryptionConfig.getUuid().equals(vaultConfigId)) {
          assertThat(encryptionConfig.isDefault()).isTrue();
          numOfVaultDefaults++;
        } else {
          assertThat(encryptionConfig.isDefault()).isFalse();
        }
      }
    }

    assertThat(numOfKmsDefaults).isEqualTo(1);
    assertThat(numOfVaultDefaults).isEqualTo(1);

    kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);

    encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertThat(encryptionConfigs).hasSize(4);

    numOfVaultDefaults = 0;
    numOfKmsDefaults = 0;

    for (EncryptionConfig encryptionConfig : encryptionConfigs) {
      if (encryptionConfig.getEncryptionType() == EncryptionType.KMS) {
        if (encryptionConfig.getUuid().equals(kmsId)) {
          assertThat(encryptionConfig.isDefault()).isTrue();
          numOfKmsDefaults++;
        } else {
          assertThat(encryptionConfig.isDefault()).isFalse();
        }
      }

      if (encryptionConfig.getEncryptionType() == EncryptionType.VAULT) {
        assertThat(encryptionConfig.isDefault()).isFalse();
        numOfVaultDefaults++;
      }
    }

    assertThat(numOfKmsDefaults).isEqualTo(1);
    assertThat(numOfVaultDefaults).isEqualTo(2);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void saveAndEditConfig() {
    Account renameAccount = getAccount(AccountType.PAID);
    String renameAccountId = renameAccount.getUuid();
    when(accountService.get(renameAccountId)).thenReturn(renameAccount);
    when(secretsManagementFeature.isAvailableForAccount(renameAccountId)).thenReturn(true);

    String name = UUID.randomUUID().toString();
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName(name);
    vaultConfig.setAccountId(renameAccountId);

    vaultService.saveVaultConfig(renameAccountId, vaultConfig);
    vaultConfig.setAuthToken(VAULT_TOKEN);

    VaultConfig savedConfig = (VaultConfig) secretManagerConfigService.getDefaultSecretManager(renameAccountId);
    assertThat(savedConfig).isEqualTo(vaultConfig);

    // Testing getVaultConfigByName API is working properly
    savedConfig = vaultService.getVaultConfigByName(renameAccountId, vaultConfig.getName());
    assertThat(savedConfig).isEqualTo(vaultConfig);

    List<EncryptedData> encryptedDataList = wingsPersistence.createQuery(EncryptedData.class)
                                                .filter(EncryptedDataKeys.type, SettingVariableTypes.VAULT)
                                                .filter(EncryptedDataKeys.accountId, renameAccountId)
                                                .asList();
    assertThat(encryptedDataList).hasSize(1);
    assertThat(encryptedDataList.get(0).getParentIds()).hasSize(1);
    assertThat(encryptedDataList.get(0).getParentIds().iterator().next()).isEqualTo(savedConfig.getUuid());
    assertThat(encryptedDataList.get(0).getName()).isEqualTo(name + "_token");

    name = UUID.randomUUID().toString();
    vaultConfig = getVaultConfig();
    savedConfig.setAuthToken(vaultConfig.getAuthToken());
    savedConfig.setName(name);
    vaultService.saveVaultConfig(renameAccountId, savedConfig);
    encryptedDataList = wingsPersistence.createQuery(EncryptedData.class)
                            .filter(EncryptedDataKeys.accountId, renameAccountId)
                            .filter(EncryptedDataKeys.type, SettingVariableTypes.VAULT)
                            .asList();
    assertThat(encryptedDataList).hasSize(1);
    assertThat(encryptedDataList.get(0).getParentIds()).hasSize(1);
    assertThat(encryptedDataList.get(0).getParentIds().iterator().next()).isEqualTo(savedConfig.getUuid());
    assertThat(encryptedDataList.get(0).getName()).isEqualTo(name + "_token");
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveAndEditConfig_withMaskedSecrets_changeNameDefaultOnly() {
    String name = UUID.randomUUID().toString();
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName(name);
    vaultConfig.setAccountId(accountId);

    vaultService.saveVaultConfig(accountId, KryoUtils.clone(vaultConfig));

    VaultConfig savedConfig = (VaultConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
    assertThat(savedConfig.getAuthToken()).isEqualTo(vaultConfig.getAuthToken());
    assertThat(savedConfig.getSecretId()).isEqualTo(vaultConfig.getSecretId());
    assertThat(savedConfig.getName()).isEqualTo(vaultConfig.getName());
    assertThat(savedConfig.isDefault()).isEqualTo(true);

    String newName = UUID.randomUUID().toString();
    vaultConfig.setUuid(savedConfig.getUuid());
    vaultConfig.setName(newName);
    vaultConfig.setDefault(false);
    vaultConfig.maskSecrets();

    // Masked Secrets, only name and default flag should be updated.
    vaultService.saveVaultConfig(accountId, KryoUtils.clone(vaultConfig));

    VaultConfig modifiedSavedConfig = vaultService.getVaultConfig(accountId, savedConfig.getUuid());
    assertThat(modifiedSavedConfig.getAuthToken()).isEqualTo(savedConfig.getAuthToken());
    assertThat(modifiedSavedConfig.getSecretId()).isEqualTo(savedConfig.getSecretId());
    assertThat(modifiedSavedConfig.getName()).isEqualTo(vaultConfig.getName());
    assertThat(modifiedSavedConfig.isDefault()).isEqualTo(false);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void saveConfigDefaultWithDefaultKms() {
    if (isKmsEnabled) {
      wingsPersistence.delete(KmsConfig.class, kmsId);
    }
    // set kms default config
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    kmsService.saveGlobalKmsConfig(accountId, kmsConfig);

    List<SecretManagerConfig> encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertThat(encryptionConfigs).hasSize(1);
    KmsConfig savedKmsConfig = (KmsConfig) encryptionConfigs.get(0);
    assertThat(savedKmsConfig.isDefault()).isTrue();
    assertThat(savedKmsConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);

    SecretManagerConfig defaultConfig = secretManagerConfigService.getDefaultSecretManager(accountId);
    assertThat(defaultConfig instanceof KmsConfig).isTrue();
    assertThat(defaultConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);

    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    defaultConfig = secretManagerConfigService.getDefaultSecretManager(accountId);
    assertThat(defaultConfig instanceof VaultConfig).isTrue();
    assertThat(defaultConfig.getAccountId()).isEqualTo(accountId);

    encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertThat(encryptionConfigs).hasSize(2);

    VaultConfig savedVaultConfig = (VaultConfig) encryptionConfigs.get(0);
    assertThat(savedVaultConfig.isDefault()).isTrue();
    assertThat(savedVaultConfig.getAccountId()).isEqualTo(accountId);

    savedKmsConfig = (KmsConfig) encryptionConfigs.get(1);
    // PL-3472: There should be only one default secret manager from the list secret manager call.
    assertThat(savedKmsConfig.isDefault()).isFalse();
    assertThat(savedKmsConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void saveConfigDefault() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    Collection<SecretManagerConfig> vaultConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertThat(vaultConfigs).hasSize(1);
    VaultConfig next = (VaultConfig) vaultConfigs.iterator().next();
    assertThat(next.isDefault()).isTrue();

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName("config1");
    vaultConfig.setDefault(true);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    vaultConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertThat(vaultConfigs).hasSize(2);

    int numOfDefault = 0;
    int numOfNonDefault = 0;

    for (SecretManagerConfig config : vaultConfigs) {
      if (config.getName().equals(getVaultConfig(VAULT_TOKEN).getName())) {
        assertThat(config.isDefault()).isFalse();
        numOfNonDefault++;
      }

      if (config.getName().equals("config1")) {
        assertThat(config.isDefault()).isTrue();
        numOfDefault++;
      }
    }

    assertThat(numOfDefault).isEqualTo(1);
    assertThat(numOfNonDefault).isEqualTo(1);

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName("config2");
    vaultConfig.setDefault(true);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    vaultConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertThat(vaultConfigs).hasSize(3);

    for (SecretManagerConfig config : vaultConfigs) {
      if (config.getName().equals(getVaultConfig(VAULT_TOKEN).getName()) || config.getName().equals("config1")) {
        assertThat(config.isDefault()).isFalse();
        numOfNonDefault++;
      }

      if (config.getName().equals("config2")) {
        assertThat(config.isDefault()).isTrue();
        numOfDefault++;
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void getConfigDefault() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    Collection<SecretManagerConfig> vaultConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertThat(vaultConfigs).hasSize(1);
    VaultConfig next = (VaultConfig) vaultConfigs.iterator().next();
    assertThat(next.isDefault()).isTrue();

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName("config1");
    vaultConfig.setDefault(true);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName("config2");
    vaultConfig.setDefault(false);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    vaultConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertThat(vaultConfigs).hasSize(3);

    VaultConfig defaultConfig = (VaultConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
    assertThat(defaultConfig).isNotNull();

    assertThat(defaultConfig.getAccountId()).isEqualTo(accountId);
    assertThat(String.valueOf(defaultConfig.getAuthToken())).isEqualTo(VAULT_TOKEN);
    assertThat(defaultConfig.getName()).isEqualTo("config1");
    assertThat(defaultConfig.getVaultUrl()).isEqualTo(vaultConfig.getVaultUrl());
    assertThat(defaultConfig.isDefault()).isTrue();
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testNewVaultConfigIfUnavailable() {
    Account account = getAccount(AccountType.PAID);
    String accountId = account.getUuid();

    when(accountService.get(accountId)).thenReturn(account);
    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(false);

    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setAccountId(accountId);

    try {
      vaultService.saveVaultConfig(accountId, vaultConfig);
      fail("");
    } catch (WingsException e) {
      assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void vaultNullEncryption() throws Exception {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);
    final String keyToEncrypt = null;
    final String name = "password";
    final String password = UUID.randomUUID().toString();
    EncryptedData encryptedData =
        vaultService.encrypt(name, keyToEncrypt, accountId, SettingVariableTypes.APP_DYNAMICS, vaultConfig, null);
    assertThat(encryptedData.getEncryptedValue()).isNull();
    assertThat(encryptedData.getEncryptionKey()).isNotNull();
    assertThat(isBlank(encryptedData.getEncryptionKey())).isFalse();

    char[] decryptedValue = vaultService.decrypt(encryptedData, accountId, vaultConfig);
    assertThat(decryptedValue).isNull();

    String randomPassword = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, randomPassword);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    EncryptedData savedEncryptedData = wingsPersistence.get(
        EncryptedData.class, ((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword());

    vaultConfig = (VaultConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
    encryptedData = vaultService.encrypt(
        name, password, accountId, SettingVariableTypes.APP_DYNAMICS, vaultConfig, savedEncryptedData);
    assertThat(encryptedData.getEncryptedValue()).isNotNull();
    assertThat(encryptedData.getEncryptionKey()).isNotNull();
    assertThat(isBlank(encryptedData.getEncryptionKey())).isFalse();

    decryptedValue = vaultService.decrypt(encryptedData, accountId, vaultConfig);
    assertThat(String.valueOf(decryptedValue)).isEqualTo(password);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void vaultEncryptionWhileSaving() throws IllegalAccessException {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute.getValue()).isEqualTo(appDynamicsConfig);
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()).isNull();
    assertThat(isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword())).isFalse();

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(settingAttribute.getUuid());
    assertThat(query.count()).isEqualTo(1);
    EncryptedData encryptedData = query.get();
    assertThat(encryptedData.getKmsId()).isEqualTo(vaultConfig.getUuid());
    assertThat(encryptedData.getCreatedBy().getUuid()).isEqualTo(user.getUuid());
    assertThat(encryptedData.getCreatedBy().getEmail()).isEqualTo(userEmail);
    assertThat(encryptedData.getCreatedBy().getName()).isEqualTo(userName);

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertThat(changeLogs).hasSize(1);
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user.getName());

    query = wingsPersistence.createQuery(EncryptedData.class);
    assertThat(query.count()).isEqualTo(numOfEncRecords + 1);

    encryptionService.decrypt((EncryptableSetting) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), workflowExecutionId, appId));

    AppDynamicsConfig value = (AppDynamicsConfig) savedAttribute.getValue();
    assertThat(String.valueOf(value.getPassword())).isEqualTo(password);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void vaultEncryptionSaveMultiple() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    wingsPersistence.save(settingAttributes);

    Collection<SecretManagerConfig> vaultConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertThat(vaultConfigs).hasSize(1);
    assertThat(vaultConfigs.iterator().next().getNumOfEncryptedValue()).isEqualTo(numOfSettingAttributes);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void vaultEncryptionUpdateObject() throws IllegalAccessException {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(settingAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncRecords + 1);

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    User user1 = User.Builder.anUser().email(UUID.randomUUID().toString()).name("user1").build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(updatedAttribute).isEqualTo(savedAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncRecords + 1);

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);
    EncryptedData encryptedData = query.get();
    assertThat(encryptedData.getKmsId()).isEqualTo(vaultConfig.getUuid());

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertThat(changeLogs).hasSize(2);
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user1.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user1.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user1.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Changed password");

    secretChangeLog = changeLogs.get(1);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Created");

    User user2 = User.Builder.anUser().email(UUID.randomUUID().toString()).name("user2").build();
    wingsPersistence.save(user2);
    UserThreadLocal.set(user2);

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    wingsPersistence.save(savedAttribute);

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);

    changeLogs = secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertThat(changeLogs).hasSize(3);
    secretChangeLog = changeLogs.get(0);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user2.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user2.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user2.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Changed password");

    secretChangeLog = changeLogs.get(1);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user1.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user1.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user1.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Changed password");

    secretChangeLog = changeLogs.get(2);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Created");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNoOnDemandMigrationOnSecretUpdate() {
    if (isKmsEnabled) {
      return;
    }

    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    String vaultId = vaultService.saveVaultConfig(accountId, vaultConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(settingAttribute);

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                     .field(EncryptedDataKeys.parentIds)
                                     .hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);

    EncryptedData encryptedData = query.get();
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.VAULT);
    assertThat(encryptedData.getKmsId()).isEqualTo(vaultId);

    // Change the default to KMS
    KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

    assertThat(secretManager.getEncryptionType(accountId)).isEqualTo(EncryptionType.KMS);

    String updatedAppId = UUID.randomUUID().toString();
    wingsPersistence.updateField(SettingAttribute.class, savedAttributeId, SettingAttributeKeys.appId, updatedAppId);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(updatedAttribute.getAppId()).isEqualTo(updatedAppId);

    query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                .field(EncryptedDataKeys.parentIds)
                .hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);

    encryptedData = query.get();
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.VAULT);
    assertThat(encryptedData.getKmsId()).isEqualTo(vaultId);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void vaultEncryptionUpdateFieldSettingAttribute() throws IllegalAccessException {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(settingAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).count())
        .isEqualTo(numOfEncRecords + 1);

    String updatedAppId = UUID.randomUUID().toString();
    wingsPersistence.updateField(SettingAttribute.class, savedAttributeId, "appId", updatedAppId);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(updatedAttribute.getAppId()).isEqualTo(updatedAppId);
    savedAttribute.setAppId(updatedAppId);
    assertThat(updatedAttribute).isEqualTo(savedAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).count())
        .isEqualTo(numOfEncRecords + 1);

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                     .field("parentIds")
                                     .hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertThat(changeLogs).hasSize(1);
    SecretChangeLog secretChangeLog = changeLogs.get(0);

    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Created");

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = getAppDynamicsConfig(accountId, newPassWord);

    updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("value", newAppDynamicsConfig);

    User user1 = User.Builder.anUser().email(UUID.randomUUID().toString()).name(UUID.randomUUID().toString()).build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);

    query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                .field("parentIds")
                .hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);
    EncryptedData encryptedData = query.get();

    changeLogs = secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertThat(changeLogs).hasSize(2);
    secretChangeLog = changeLogs.get(0);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user1.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user1.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user1.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Changed password");

    secretChangeLog = changeLogs.get(1);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Created");

    assertThat(encryptedData.getCreatedBy().getUuid()).isEqualTo(user.getUuid());
    assertThat(encryptedData.getCreatedBy().getEmail()).isEqualTo(userEmail);
    assertThat(encryptedData.getCreatedBy().getName()).isEqualTo(userName);

    updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(updatedAttribute.getAppId()).isEqualTo(updatedAppId);
    assertThat(updatedAttribute.getName()).isEqualTo(updatedName);

    newAppDynamicsConfig.setPassword(null);
    assertThat(updatedAttribute.getValue()).isEqualTo(newAppDynamicsConfig);
    newAppDynamicsConfig.setPassword(newPassWord.toCharArray());

    assertThat(wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncRecords + 1);

    User user2 = User.Builder.anUser().email(UUID.randomUUID().toString()).name(UUID.randomUUID().toString()).build();
    wingsPersistence.save(user2);
    UserThreadLocal.set(user2);
    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);

    query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                .field("parentIds")
                .hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);
    encryptedData = query.get();

    changeLogs = secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertThat(changeLogs).hasSize(3);
    secretChangeLog = changeLogs.get(0);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user2.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user2.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user2.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Changed password");

    secretChangeLog = changeLogs.get(1);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user1.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user1.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user1.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Changed password");

    secretChangeLog = changeLogs.get(2);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Created");

    // test decryption
    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertThat(savedConfig.getPassword()).isNull();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertThat(String.valueOf(savedConfig.getPassword())).isEqualTo(newPassWord);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @RealMongo
  public void vaultEncryptionSaveServiceVariable() throws IllegalAccessException {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

    final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(UUID.randomUUID().toString())
                                                .envId(UUID.randomUUID().toString())
                                                .entityType(EntityType.APPLICATION)
                                                .entityId(UUID.randomUUID().toString())
                                                .parentServiceVariableId(UUID.randomUUID().toString())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                .expression(UUID.randomUUID().toString())
                                                .accountId(accountId)
                                                .name(UUID.randomUUID().toString())
                                                .value(secretId.toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(serviceVariable);
    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncRecords + 1);

    secretName = UUID.randomUUID().toString();
    secretValue = UUID.randomUUID().toString();
    secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", "newName");
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncRecords + 2);

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);

    // decrypt and verify
    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, workflowExecutionId, appId));
    assertThat(String.valueOf(savedVariable.getValue())).isEqualTo(secretValue);

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertThat(changeLogs).hasSize(1);
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user.getEmail());
    assertThat(secretChangeLog.getUser().getName()).isEqualTo(user.getName());
    assertThat(secretChangeLog.getDescription()).isEqualTo("Created");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void vaultEncryptionSaveServiceVariableTemplate() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

    String serviceId = wingsPersistence.save(Service.builder().name(UUID.randomUUID().toString()).build());
    String serviceTemplateId =
        wingsPersistence.save(ServiceTemplate.Builder.aServiceTemplate().withServiceId(serviceId).build());

    final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(UUID.randomUUID().toString())
                                                .envId(UUID.randomUUID().toString())
                                                .entityType(EntityType.SERVICE_TEMPLATE)
                                                .entityId(serviceTemplateId)
                                                .parentServiceVariableId(UUID.randomUUID().toString())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                .expression(UUID.randomUUID().toString())
                                                .accountId(accountId)
                                                .name(UUID.randomUUID().toString())
                                                .value(secretId.toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(serviceVariable);
    assertThat(savedAttribute.getValue()).isNull();
    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncRecords + 1);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void vaultEncryptionDeleteSettingAttribute() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    wingsPersistence.save(settingAttributes);
    validateSettingAttributes(settingAttributes, numOfEncRecords + numOfSettingAttributes);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void vaultEncryptionDeleteSettingAttributeQueryUuid() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    for (SettingAttribute settingAttribute : settingAttributes) {
      wingsPersistence.save(settingAttribute);
    }
    validateSettingAttributes(settingAttributes, numOfEncRecords + numOfSettingAttributes);

    settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    wingsPersistence.save(settingAttributes);
    validateSettingAttributes(settingAttributes, numOfEncRecords + numOfSettingAttributes);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Repeat(times = 3, successes = 1)
  @Category(UnitTests.class)
  public void transitionVault() throws InterruptedException, IllegalAccessException {
    Thread listenerThread = startTransitionListener();
    try {
      VaultConfig fromConfig = getVaultConfig(VAULT_TOKEN);
      vaultService.saveVaultConfig(accountId, fromConfig);

      int numOfSettingAttributes = 5;
      Map<String, SettingAttribute> encryptedEntities = new HashMap<>();
      for (int i = 0; i < numOfSettingAttributes; i++) {
        String password = UUID.randomUUID().toString();
        final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
        SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

        wingsPersistence.save(settingAttribute);
        appDynamicsConfig.setPassword(null);
        encryptedEntities.put(settingAttribute.getUuid(), settingAttribute);
      }

      Query<EncryptedData> query =
          wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId);
      List<EncryptedData> encryptedData = new ArrayList<>();
      assertThat(query.count()).isEqualTo(numOfEncRecords + numOfSettingAttributes);
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null || data.getType() == SettingVariableTypes.VAULT) {
          continue;
        }
        encryptedData.add(data);
        assertThat(data.getKmsId()).isEqualTo(fromConfig.getUuid());
        assertThat(data.getAccountId()).isEqualTo(accountId);
      }

      assertThat(encryptedData).hasSize(numOfSettingAttributes);

      VaultConfig toConfig = getVaultConfig(VAULT_TOKEN);
      vaultService.saveVaultConfig(accountId, toConfig);

      secretManager.transitionSecrets(
          accountId, EncryptionType.VAULT, fromConfig.getUuid(), EncryptionType.VAULT, toConfig.getUuid());
      waitTillEventsProcessed(30);
      query = wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId);

      assertThat(query.count()).isEqualTo(numOfEncRecords + 1 + numOfSettingAttributes);
      encryptedData = new ArrayList<>();
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null || data.getType() == SettingVariableTypes.VAULT) {
          continue;
        }
        encryptedData.add(data);
        assertThat(data.getKmsId()).isEqualTo(toConfig.getUuid());
        assertThat(data.getAccountId()).isEqualTo(accountId);
      }
      assertThat(encryptedData).hasSize(numOfSettingAttributes);

      // read the values and compare
      PageResponse<SettingAttribute> attributeQuery = wingsPersistence.query(
          SettingAttribute.class, aPageRequest().addFilter("accountId", Operator.EQ, accountId).build());
      assertThat(attributeQuery).hasSize(numOfSettingAttributes);
      for (SettingAttribute settingAttribute : attributeQuery) {
        assertThat(settingAttribute).isEqualTo(encryptedEntities.get(settingAttribute.getUuid()));
      }
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void transitionAndDeleteVault() throws InterruptedException, IllegalAccessException {
    Thread listenerThread = startTransitionListener();
    try {
      VaultConfig fromConfig = getVaultConfig(VAULT_TOKEN);
      vaultService.saveVaultConfig(accountId, fromConfig);

      int numOfSettingAttributes = 5;
      List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
      wingsPersistence.save(settingAttributes);

      Query<EncryptedData> query =
          wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId);
      List<EncryptedData> encryptedDataList = new ArrayList<>();
      assertThat(query.count()).isEqualTo(numOfEncRecords + numOfSettingAttributes);
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null || data.getType() == SettingVariableTypes.VAULT) {
          continue;
        }
        encryptedDataList.add(data);
        assertThat(data.getKmsId()).isEqualTo(fromConfig.getUuid());
        assertThat(data.getAccountId()).isEqualTo(accountId);
      }

      assertThat(encryptedDataList).hasSize(numOfSettingAttributes);

      VaultConfig toConfig = getVaultConfig(VAULT_TOKEN);
      vaultService.saveVaultConfig(accountId, toConfig);

      assertThat(secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true).size())
          .isEqualTo(2);
      try {
        vaultService.deleteVaultConfig(accountId, fromConfig.getUuid());
        fail("Was able to delete vault which has reference in encrypted secrets");
      } catch (WingsException e) {
        // expected
      }

      secretManager.transitionSecrets(
          accountId, EncryptionType.VAULT, fromConfig.getUuid(), EncryptionType.VAULT, toConfig.getUuid());
      waitTillEventsProcessed(30);
      vaultService.deleteVaultConfig(accountId, fromConfig.getUuid());
      assertThat(secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true).size())
          .isEqualTo(1);

      query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority);
      assertThat(query.count()).isEqualTo(numOfEncRecords + numOfSettingAttributes);

      // Verified old secrets has been deleted from Vault
      for (EncryptedData encryptedData : encryptedDataList) {
        verify(secretManagementDelegateService)
            .deleteVaultSecret(eq(encryptedData.getEncryptionKey()), any(VaultConfig.class));
      }

      encryptedDataList.clear();
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null || data.getType() == SettingVariableTypes.VAULT) {
          continue;
        }
        encryptedDataList.add(data);
        assertThat(data.getKmsId()).isEqualTo(toConfig.getUuid());
        assertThat(data.getAccountId()).isEqualTo(accountId);
      }
      assertThat(encryptedDataList.size()).isEqualTo(numOfSettingAttributes);
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void transitionFromKmsToVault() throws InterruptedException, IllegalAccessException {
    if (isKmsEnabled) {
      return;
    }

    KmsConfig fromConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, fromConfig);

    Thread listenerThread = startTransitionListener();
    try {
      int numOfSettingAttributes = 5;
      Map<String, SettingAttribute> encryptedEntities = new HashMap<>();
      for (int i = 0; i < numOfSettingAttributes; i++) {
        String password = UUID.randomUUID().toString();
        final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
        SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

        wingsPersistence.save(settingAttribute);
        appDynamicsConfig.setPassword(null);
        encryptedEntities.put(settingAttribute.getUuid(), settingAttribute);
      }

      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                       .filter(EncryptedDataKeys.type, SettingVariableTypes.APP_DYNAMICS);
      assertThat(query.count()).isEqualTo(numOfSettingAttributes);
      for (EncryptedData data : query.asList()) {
        assertThat(data.getKmsId()).isEqualTo(fromConfig.getUuid());
        assertThat(data.getAccountId()).isEqualTo(accountId);
        assertThat(data.getEncryptionType()).isEqualTo(EncryptionType.KMS);
      }

      VaultConfig toConfig = getVaultConfig(VAULT_TOKEN);
      vaultService.saveVaultConfig(accountId, toConfig);

      secretManager.transitionSecrets(
          accountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.VAULT, toConfig.getUuid());
      waitTillEventsProcessed(30);
      query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                  .filter(EncryptedDataKeys.type, SettingVariableTypes.APP_DYNAMICS);

      assertThat(query.count()).isEqualTo(numOfSettingAttributes);
      for (EncryptedData data : query.asList()) {
        assertThat(data.getKmsId()).isEqualTo(toConfig.getUuid());
        assertThat(data.getAccountId()).isEqualTo(accountId);
        assertThat(data.getEncryptionType()).isEqualTo(EncryptionType.VAULT);
      }

      secretManager.transitionSecrets(
          accountId, EncryptionType.VAULT, toConfig.getUuid(), EncryptionType.KMS, fromConfig.getUuid());
      waitTillEventsProcessed(30);
      query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                  .filter(EncryptedDataKeys.type, SettingVariableTypes.APP_DYNAMICS);

      assertThat(query.count()).isEqualTo(numOfSettingAttributes);
      for (EncryptedData data : query.asList()) {
        assertThat(data.getKmsId()).isEqualTo(fromConfig.getUuid());
        assertThat(data.getAccountId()).isEqualTo(accountId);
        assertThat(data.getEncryptionType()).isEqualTo(EncryptionType.KMS);
      }

      // read the values and compare
      PageResponse<SettingAttribute> attributeQuery =
          wingsPersistence.query(SettingAttribute.class, aPageRequest().build());
      assertThat(attributeQuery).hasSize(numOfSettingAttributes);
      for (SettingAttribute settingAttribute : attributeQuery) {
        assertThat(settingAttribute).isEqualTo(encryptedEntities.get(settingAttribute.getUuid()));
      }
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  private void waitTillEventsProcessed(long seconds) {
    final long startTime = System.currentTimeMillis();
    long remainingCount = wingsPersistence.createQuery(KmsTransitionEvent.class, excludeAuthority).count();
    while (remainingCount > 0 && System.currentTimeMillis() < startTime + TimeUnit.SECONDS.toMillis(seconds)) {
      logger.info("remaining secrets: " + remainingCount);
      remainingCount = wingsPersistence.createQuery(KmsTransitionEvent.class, excludeAuthority).count();
      Morpheus.sleep(ofSeconds(1));
    }

    if (remainingCount != 0) {
      throw new RuntimeException(
          "could not process all the events in " + seconds + " seconds. Remaining secrets: " + remainingCount);
    }

    Morpheus.sleep(ofSeconds(2));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @RealMongo
  public void saveConfigFileWithEncryption() throws IOException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);
    VaultConfig fromConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, fromConfig);

    String secretName = UUID.randomUUID().toString();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId = secretManager.saveFile(accountId, kmsId, secretName, fileToSave.length(), null,
        new BoundedInputStream(new FileInputStream(fileToSave)));

    String encryptedUuid = wingsPersistence.createQuery(EncryptedData.class)
                               .filter(EncryptedDataKeys.type, CONFIG_FILE)
                               .filter(EncryptedDataKeys.accountId, accountId)
                               .get()
                               .getUuid();

    Service service = Service.builder().name(UUID.randomUUID().toString()).appId(appId).build();
    wingsPersistence.save(service);

    Activity activity = Activity.builder().workflowExecutionId(workflowExecutionId).environmentId(envId).build();
    activity.setAppId(appId);
    wingsPersistence.save(activity);

    ConfigFile configFile = ConfigFile.builder()
                                .templateId(UUID.randomUUID().toString())
                                .envId(UUID.randomUUID().toString())
                                .entityType(EntityType.SERVICE)
                                .entityId(service.getUuid())
                                .description(UUID.randomUUID().toString())
                                .parentConfigFileId(UUID.randomUUID().toString())
                                .relativeFilePath(UUID.randomUUID().toString())
                                .targetToAllEnv(r.nextBoolean())
                                .defaultVersion(r.nextInt())
                                .envIdVersionMapString(UUID.randomUUID().toString())
                                .setAsDefault(r.nextBoolean())
                                .notes(UUID.randomUUID().toString())
                                .overridePath(UUID.randomUUID().toString())
                                .configOverrideType(ConfigOverrideType.CUSTOM)
                                .configOverrideExpression(UUID.randomUUID().toString())
                                .encryptedFileId(secretFileId)
                                .encrypted(true)
                                .build();

    configFile.setAccountId(accountId);
    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(appId);

    String configFileId = configService.save(configFile, null);
    File download = configService.download(appId, configFileId);
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()));
    assertThat(wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).count())
        .isEqualTo(numOfEncRecords + 1);

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                                                .filter(EncryptedDataKeys.type, CONFIG_FILE)
                                                .filter(EncryptedDataKeys.accountId, accountId)
                                                .asList();
    assertThat(encryptedFileData).hasSize(1);
    assertThat(encryptedFileData.get(0).getParentIds().isEmpty()).isFalse();
    // test update
    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManager.updateFile(accountId, newSecretName, encryptedUuid, fileToUpdate.length(), null,
        new BoundedInputStream(new FileInputStream(fileToUpdate)));

    download = configService.download(appId, configFileId);
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()));
    assertThat(wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).count())
        .isEqualTo(numOfEncRecords + 1);

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                            .filter(EncryptedDataKeys.accountId, accountId)
                            .filter(EncryptedDataKeys.type, CONFIG_FILE)
                            .asList();
    assertThat(encryptedFileData).hasSize(1);
    assertThat(encryptedFileData.get(0).getParentIds().isEmpty()).isFalse();

    int numOfAccess = 7;
    for (int i = 0; i < numOfAccess; i++) {
      configService.downloadForActivity(appId, configFileId, activity.getUuid());
    }
    List<SecretUsageLog> usageLogs =
        secretManager.getUsageLogs(aPageRequest().build(), accountId, encryptedUuid, CONFIG_FILE);
    assertThat(usageLogs).hasSize(numOfAccess);

    for (SecretUsageLog usageLog : usageLogs) {
      assertThat(usageLog.getWorkflowExecutionName()).isEqualTo(workflowName);
      assertThat(usageLog.getAccountId()).isEqualTo(accountId);
      assertThat(usageLog.getEnvId()).isEqualTo(envId);
      assertThat(usageLog.getAppId()).isEqualTo(appId);
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void reuseYamlPasswordVaultEncryption() throws IllegalAccessException {
    VaultConfig fromConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, fromConfig);

    int numOfSettingAttributes = 5;
    String password = "password";
    Set<String> attributeIds = new HashSet<>();
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    attributeIds.add(wingsPersistence.save(settingAttribute));

    // yamlRef will be an URL format like: "hashicorpvault://vaultManagerName/harness/APP_DYNAMICS/...#value" for Vault
    // based secrets
    String yamlRef = secretManager.getEncryptedYamlRef(appDynamicsConfig);
    assertThat(yamlRef).isNotNull();
    assertThat(yamlRef.startsWith(EncryptionType.VAULT.getYamlName() + "://" + fromConfig.getName() + "/harness/"))
        .isTrue();
    assertThat(yamlRef.contains("#value")).isTrue();

    for (int i = 1; i < numOfSettingAttributes; i++) {
      appDynamicsConfig = getAppDynamicsConfig(accountId, null, yamlRef);
      settingAttribute = getSettingAttribute(appDynamicsConfig);

      attributeIds.add(wingsPersistence.save(settingAttribute));
    }

    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(numOfSettingAttributes);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncRecords + 1);

    List<EncryptedData> encryptedDatas = wingsPersistence.createQuery(EncryptedData.class)
                                             .filter(EncryptedDataKeys.encryptionType, EncryptionType.VAULT)
                                             .filter(EncryptedDataKeys.accountId, accountId)
                                             .asList();
    assertThat(encryptedDatas).hasSize(1);
    EncryptedData encryptedData = encryptedDatas.get(0);
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.VAULT);
    assertThat(encryptedData.getAccountId()).isEqualTo(accountId);
    assertThat(encryptedData.isEnabled()).isTrue();
    assertThat(encryptedData.getKmsId()).isEqualTo(fromConfig.getUuid());
    assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.APP_DYNAMICS);
    assertThat(encryptedData.getParentIds()).hasSize(numOfSettingAttributes);
    assertThat(encryptedData.getParentIds()).isEqualTo(attributeIds);

    for (String attributeId : attributeIds) {
      SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, attributeId);
      AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
      assertThat(savedConfig.getAccountId()).isEqualTo(accountId);
      assertThat(savedConfig.getPassword()).isNull();
      assertThat(isBlank(savedConfig.getEncryptedPassword())).isFalse();

      encryptionService.decrypt(
          savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
      assertThat(String.valueOf(savedConfig.getPassword())).isEqualTo(password);
    }

    // delete configs and check
    int i = 0;
    Set<String> remainingAttrs = new HashSet<>(attributeIds);
    for (String attributeId : attributeIds) {
      wingsPersistence.delete(accountId, SettingAttribute.class, attributeId);
      remainingAttrs.remove(attributeId);
      encryptedDatas = wingsPersistence.createQuery(EncryptedData.class)
                           .filter(EncryptedDataKeys.accountId, accountId)
                           .filter(EncryptedDataKeys.encryptionType, EncryptionType.VAULT)
                           .asList();
      if (i == numOfSettingAttributes - 1) {
        assertThat(encryptedDatas.isEmpty()).isTrue();
      } else {
        assertThat(encryptedDatas).hasSize(1);
        encryptedData = encryptedDatas.get(0);
        assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.VAULT);
        assertThat(encryptedData.getAccountId()).isEqualTo(accountId);
        assertThat(encryptedData.isEnabled()).isTrue();
        assertThat(encryptedData.getKmsId()).isEqualTo(fromConfig.getUuid());
        assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.APP_DYNAMICS);
        assertThat(encryptedData.getParentIds()).hasSize(numOfSettingAttributes - (i + 1));

        assertThat(encryptedData.getParentIds().contains(attributeId)).isFalse();
        assertThat(encryptedData.getParentIds()).isEqualTo(remainingAttrs);
      }
      i++;
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void vaultSecretManager_Crud_shouldGenerate_Audit() {
    if (isKmsEnabled) {
      // Doesn't make any difference to run with KMS enabled...
      return;
    }

    VaultConfig vaultConfig = getVaultConfig();
    vaultConfig.setAccountId(accountId);

    String secretManagerId = vaultService.saveVaultConfig(accountId, KryoUtils.clone(vaultConfig));
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(eq(accountId), eq(null), any(VaultConfig.class), eq(Event.Type.CREATE));

    vaultConfig.setUuid(secretManagerId);
    vaultConfig.setDefault(false);
    vaultConfig.setName(vaultConfig.getName() + "_Updated");
    vaultService.saveVaultConfig(accountId, KryoUtils.clone(vaultConfig));
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(
            eq(accountId), any(VaultConfig.class), any(VaultConfig.class), eq(Event.Type.UPDATE));

    vaultService.deleteVaultConfig(accountId, secretManagerId);
    verify(auditServiceHelper).reportDeleteForAuditingUsingAccountId(eq(accountId), any(VaultConfig.class));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void secretText_createdBeforeLocalEncryption_shouldBeReturned() throws Exception {
    VaultConfig vaultConfig = getVaultConfig();
    vaultConfig.setAccountId(accountId);
    vaultService.saveVaultConfig(accountId, KryoUtils.clone(vaultConfig));

    String secretName = UUID.randomUUID().toString();
    secretManager.saveSecret(accountId, kmsId, secretName, "MySecret", null, new UsageRestrictions());

    PageRequest<EncryptedData> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter("accountId", Operator.EQ, accountId)
            .addFilter("type", Operator.IN, new Object[] {SettingVariableTypes.SECRET_TEXT.name()})
            .build();

    // The above created secret text should be returned by the list secrets call.
    PageResponse<EncryptedData> response = secretManager.listSecrets(accountId, pageRequest, null, null, true);
    assertThat(response.getResponse()).isNotEmpty();
    assertThat(response.getResponse().size()).isEqualTo(1);
    EncryptedData encryptedData = response.getResponse().get(0);
    assertThat(encryptedData.getName()).isEqualTo(secretName);
    assertThat(encryptedData.getEncryptedBy()).isNotEmpty();

    // Enable local encryption on this account.
    Account account = accountService.get(accountId);
    account.setLocalEncryptionEnabled(true);
    wingsPersistence.save(account);

    // The old secret should still be returned by the list secrets call.
    response = secretManager.listSecrets(accountId, pageRequest, null, null, true);
    assertThat(response.getResponse()).isNotEmpty();
    assertThat(response.getResponse().size()).isEqualTo(1);

    // But the secret manager field will be null instead.
    encryptedData = response.getResponse().get(0);
    assertThat(encryptedData.getName()).isEqualTo(secretName);
    assertThat(encryptedData.getEncryptedBy()).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void WinRmConnections_shouldBeReturned_in_listSettingAttributes() {
    VaultConfig fromConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, fromConfig);

    final String password = UUID.randomUUID().toString();
    WinRmConnectionAttributes winRmConnectionAttributes = getWinRmConnectionAttribute(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(winRmConnectionAttributes);
    SettingAttribute savedAttribute = settingsService.save(settingAttribute);

    SettingAttribute savedSettingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttribute.getUuid());
    assertThat(savedSettingAttribute).isNotNull();
    assertThat(savedSettingAttribute.getCategory()).isEqualTo(SettingCategory.SETTING);

    Collection<SettingAttribute> encryptedValues =
        secretManagementResource.listEncryptedSettingAttributes(accountId, SettingCategory.SETTING.name())
            .getResource();
    assertThat(encryptedValues).isNotEmpty();
    assertThat(encryptedValues.size()).isEqualTo(1);
    assertThat(encryptedValues.iterator().next().getCategory()).isEqualTo(SettingCategory.SETTING);
  }

  private Thread startTransitionListener() throws IllegalAccessException {
    transitionEventListener = new KmsTransitionEventListener(kmsTransitionConsumer);
    FieldUtils.writeField(transitionEventListener, "timer", new TimerScheduledExecutorService(), true);
    FieldUtils.writeField(transitionEventListener, "queueController", new ConfigurationController(1), true);
    FieldUtils.writeField(transitionEventListener, "queueConsumer", transitionKmsQueue, true);
    FieldUtils.writeField(transitionEventListener, "secretManager", secretManager, true);

    Thread eventListenerThread = new Thread(() -> transitionEventListener.run());
    eventListenerThread.start();
    return eventListenerThread;
  }

  private void stopTransitionListener(Thread thread) throws InterruptedException {
    transitionEventListener.shutDown();
    thread.join();
  }
}
