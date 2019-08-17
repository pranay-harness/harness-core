package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.settings.SettingValue.SettingVariableTypes.CONFIG_FILE;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.queue.Queue;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.rule.RealMongo;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.stream.BoundedInputStream;
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
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.User;
import software.wings.beans.VaultConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.features.api.PremiumFeature;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;

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
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigService configService;
  @Inject private EncryptionService encryptionService;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SecretManagementDelegateService secretManagementDelegateService;
  @Mock private PremiumFeature secretsManagementFeature;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();
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
    when(secretManagementDelegateService.encrypt(anyString(), anyObject(), anyString(), any(SettingVariableTypes.class),
             any(VaultConfig.class), any(EncryptedData.class)))
        .then(invocation -> {
          Object[] args = invocation.getArguments();
          return encrypt((String) args[0], (String) args[1], (String) args[2], (SettingVariableTypes) args[3],
              (VaultConfig) args[4], (EncryptedData) args[5]);
        });

    when(secretManagementDelegateService.decrypt(anyObject(), any(VaultConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (VaultConfig) args[1]);
    });

    when(secretManagementDelegateService.encrypt(anyString(), anyObject(), anyObject())).then(invocation -> {
      Object[] args = invocation.getArguments();
      return encrypt((String) args[0], (char[]) args[1], (KmsConfig) args[2]);
    });

    when(secretManagementDelegateService.decrypt(anyObject(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (KmsConfig) args[1]);
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
  @Category(UnitTests.class)
  public void invalidConfig() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setAuthToken("invalidKey");
    vaultConfig.setAccountId(accountId);

    try {
      vaultService.saveVaultConfig(accountId, vaultConfig);
      fail("Saved invalid vault config");
    } catch (WingsException e) {
      assertEquals(ErrorCode.VAULT_OPERATION_ERROR, e.getCode());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void saveConfig() {
    if (isKmsEnabled) {
      kmsService.deleteKmsConfig(accountId, kmsId);
    }

    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setDefault(false);
    String vaultConfigId = vaultService.saveVaultConfig(accountId, vaultConfig);

    List<SecretManagerConfig> encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertEquals(1, encryptionConfigs.size());
    VaultConfig next = (VaultConfig) encryptionConfigs.get(0);
    assertFalse(next.isDefault());
    assertEquals(accountId, next.getAccountId());
    assertEquals(SECRET_MASK, String.valueOf(next.getAuthToken()));
    assertEquals(vaultConfig.getName(), next.getName());
    assertEquals(vaultConfig.getVaultUrl(), next.getVaultUrl());
    assertFalse(next.isDefault());

    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);

    encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertEquals(2, encryptionConfigs.size());
    int numOfVaultDefaults = 0;
    int numOfKmsDefaults = 0;

    for (EncryptionConfig encryptionConfig : encryptionConfigs) {
      if (encryptionConfig.getEncryptionType() == EncryptionType.KMS) {
        assertTrue(encryptionConfig.isDefault());
        assertEquals(kmsId, encryptionConfig.getUuid());
        numOfKmsDefaults++;
      }

      if (encryptionConfig.getEncryptionType() == EncryptionType.VAULT) {
        assertFalse(encryptionConfig.isDefault());
        assertEquals(vaultConfigId, encryptionConfig.getUuid());
        numOfVaultDefaults++;
      }
    }

    assertEquals(1, numOfKmsDefaults);
    assertEquals(1, numOfVaultDefaults);

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setDefault(true);
    vaultConfigId = vaultService.saveVaultConfig(accountId, vaultConfig);

    encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertEquals(3, encryptionConfigs.size());

    numOfVaultDefaults = 0;
    numOfKmsDefaults = 0;

    for (EncryptionConfig encryptionConfig : encryptionConfigs) {
      if (encryptionConfig.getEncryptionType() == EncryptionType.KMS) {
        assertFalse(encryptionConfig.isDefault());
        assertEquals(kmsId, encryptionConfig.getUuid());
        numOfKmsDefaults++;
      }

      if (encryptionConfig.getEncryptionType() == EncryptionType.VAULT) {
        if (encryptionConfig.getUuid().equals(vaultConfigId)) {
          assertTrue(encryptionConfig.isDefault());
          numOfVaultDefaults++;
        } else {
          assertFalse(encryptionConfig.isDefault());
        }
      }
    }

    assertEquals(1, numOfKmsDefaults);
    assertEquals(1, numOfVaultDefaults);

    kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);

    encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertEquals(4, encryptionConfigs.size());

    numOfVaultDefaults = 0;
    numOfKmsDefaults = 0;

    for (EncryptionConfig encryptionConfig : encryptionConfigs) {
      if (encryptionConfig.getEncryptionType() == EncryptionType.KMS) {
        if (encryptionConfig.getUuid().equals(kmsId)) {
          assertTrue(encryptionConfig.isDefault());
          numOfKmsDefaults++;
        } else {
          assertFalse(encryptionConfig.isDefault());
        }
      }

      if (encryptionConfig.getEncryptionType() == EncryptionType.VAULT) {
        assertFalse(encryptionConfig.isDefault());
        numOfVaultDefaults++;
      }
    }

    assertEquals(1, numOfKmsDefaults);
    assertEquals(2, numOfVaultDefaults);
  }

  @Test
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
    assertEquals(vaultConfig, savedConfig);

    // Testing getVaultConfigByName API is working properly
    savedConfig = vaultService.getVaultConfigByName(renameAccountId, vaultConfig.getName());
    assertEquals(vaultConfig, savedConfig);

    List<EncryptedData> encryptedDataList = wingsPersistence.createQuery(EncryptedData.class)
                                                .filter(EncryptedDataKeys.type, SettingVariableTypes.VAULT)
                                                .filter(EncryptedDataKeys.accountId, renameAccountId)
                                                .asList();
    assertEquals(1, encryptedDataList.size());
    assertEquals(1, encryptedDataList.get(0).getParentIds().size());
    assertEquals(savedConfig.getUuid(), encryptedDataList.get(0).getParentIds().iterator().next());
    assertEquals(name + "_token", encryptedDataList.get(0).getName());

    name = UUID.randomUUID().toString();
    vaultConfig = getVaultConfig();
    savedConfig.setAuthToken(vaultConfig.getAuthToken());
    savedConfig.setName(name);
    vaultService.saveVaultConfig(renameAccountId, savedConfig);
    encryptedDataList = wingsPersistence.createQuery(EncryptedData.class)
                            .filter(EncryptedDataKeys.accountId, renameAccountId)
                            .filter(EncryptedDataKeys.type, SettingVariableTypes.VAULT)
                            .asList();
    assertEquals(1, encryptedDataList.size());
    assertEquals(1, encryptedDataList.get(0).getParentIds().size());
    assertEquals(savedConfig.getUuid(), encryptedDataList.get(0).getParentIds().iterator().next());
    assertEquals(name + "_token", encryptedDataList.get(0).getName());
  }

  @Test
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
    assertEquals(1, encryptionConfigs.size());
    KmsConfig savedKmsConfig = (KmsConfig) encryptionConfigs.get(0);
    assertTrue(savedKmsConfig.isDefault());
    assertEquals(GLOBAL_ACCOUNT_ID, savedKmsConfig.getAccountId());

    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    encryptionConfigs = secretManager.listSecretManagers(accountId);
    assertEquals(2, encryptionConfigs.size());

    VaultConfig savedVaultConfig = (VaultConfig) encryptionConfigs.get(0);
    assertTrue(savedVaultConfig.isDefault());
    assertEquals(accountId, savedVaultConfig.getAccountId());

    savedKmsConfig = (KmsConfig) encryptionConfigs.get(1);
    assertFalse(savedKmsConfig.isDefault());
    assertEquals(GLOBAL_ACCOUNT_ID, savedKmsConfig.getAccountId());
  }

  @Test
  @Category(UnitTests.class)
  public void saveConfigDefault() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    Collection<SecretManagerConfig> vaultConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertEquals(1, vaultConfigs.size());
    VaultConfig next = (VaultConfig) vaultConfigs.iterator().next();
    assertTrue(next.isDefault());

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName("config1");
    vaultConfig.setDefault(true);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    vaultConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertEquals(2, vaultConfigs.size());

    int numOfDefault = 0;
    int numOfNonDefault = 0;

    for (SecretManagerConfig config : vaultConfigs) {
      if (config.getName().equals(getVaultConfig(VAULT_TOKEN).getName())) {
        assertFalse(config.isDefault());
        numOfNonDefault++;
      }

      if (config.getName().equals("config1")) {
        assertTrue(config.isDefault());
        numOfDefault++;
      }
    }

    assertEquals(1, numOfDefault);
    assertEquals(1, numOfNonDefault);

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName("config2");
    vaultConfig.setDefault(true);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    vaultConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertEquals(3, vaultConfigs.size());

    for (SecretManagerConfig config : vaultConfigs) {
      if (config.getName().equals(getVaultConfig(VAULT_TOKEN).getName()) || config.getName().equals("config1")) {
        assertFalse(config.isDefault());
        numOfNonDefault++;
      }

      if (config.getName().equals("config2")) {
        assertTrue(config.isDefault());
        numOfDefault++;
      }
    }
  }

  @Test
  @Category(UnitTests.class)
  public void getConfigDefault() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    Collection<SecretManagerConfig> vaultConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertEquals(1, vaultConfigs.size());
    VaultConfig next = (VaultConfig) vaultConfigs.iterator().next();
    assertTrue(next.isDefault());

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName("config1");
    vaultConfig.setDefault(true);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultConfig.setName("config2");
    vaultConfig.setDefault(false);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    vaultConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertEquals(3, vaultConfigs.size());

    VaultConfig defaultConfig = (VaultConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
    assertNotNull(defaultConfig);

    assertEquals(accountId, defaultConfig.getAccountId());
    assertEquals(VAULT_TOKEN, String.valueOf(defaultConfig.getAuthToken()));
    assertEquals("config1", defaultConfig.getName());
    assertEquals(vaultConfig.getVaultUrl(), defaultConfig.getVaultUrl());
    assertTrue(defaultConfig.isDefault());
  }

  @Test
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
      fail();
    } catch (WingsException e) {
      assertEquals(ErrorCode.INVALID_REQUEST, e.getCode());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void vaultNullEncryption() throws Exception {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);
    final String keyToEncrypt = null;
    final String name = "password";
    final String password = UUID.randomUUID().toString();
    EncryptedData encryptedData =
        vaultService.encrypt(name, keyToEncrypt, accountId, SettingVariableTypes.APP_DYNAMICS, vaultConfig, null);
    assertNull(encryptedData.getEncryptedValue());
    assertNotNull(encryptedData.getEncryptionKey());
    assertFalse(isBlank(encryptedData.getEncryptionKey()));

    char[] decryptedValue = vaultService.decrypt(encryptedData, accountId, vaultConfig);
    assertNull(decryptedValue);

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
    assertNotNull(encryptedData.getEncryptedValue());
    assertNotNull(encryptedData.getEncryptionKey());
    assertFalse(isBlank(encryptedData.getEncryptionKey()));

    decryptedValue = vaultService.decrypt(encryptedData, accountId, vaultConfig);
    assertEquals(password, String.valueOf(decryptedValue));
  }

  @Test
  @Category(UnitTests.class)
  public void vaultEncryptionWhileSaving() throws IllegalAccessException {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertNull(((AppDynamicsConfig) savedAttribute.getValue()).getPassword());
    assertFalse(isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()));

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(settingAttribute.getUuid());
    assertEquals(1, query.count());
    EncryptedData encryptedData = query.get();
    assertEquals(vaultConfig.getUuid(), encryptedData.getKmsId());
    assertEquals(user.getUuid(), encryptedData.getCreatedBy().getUuid());
    assertEquals(userEmail, encryptedData.getCreatedBy().getEmail());
    assertEquals(userName, encryptedData.getCreatedBy().getName());

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());

    query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(numOfEncRecords + 1, query.count());

    encryptionService.decrypt((EncryptableSetting) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), workflowExecutionId, appId));

    AppDynamicsConfig value = (AppDynamicsConfig) savedAttribute.getValue();
    assertEquals(password, String.valueOf(value.getPassword()));
  }

  @Test
  @Category(UnitTests.class)
  public void vaultEncryptionSaveMultiple() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      String password = "password" + i;
      final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
      SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

      settingAttributes.add(settingAttribute);
    }
    wingsPersistence.save(settingAttributes);

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncRecords + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());
    for (int i = 0; i < numOfSettingAttributes; i++) {
      String id = settingAttributes.get(i).getUuid();
      SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, id);
      assertEquals(settingAttributes.get(i), savedAttribute);
      AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttributes.get(i).getValue();
      assertNull(appDynamicsConfig.getPassword());

      encryptionService.decrypt(
          appDynamicsConfig, secretManager.getEncryptionDetails(appDynamicsConfig, workflowExecutionId, appId));
      assertEquals("password" + i, new String(appDynamicsConfig.getPassword()));
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(id);
      assertEquals(1, query.count());
      assertEquals(vaultConfig.getUuid(), query.get().getKmsId());
    }

    Collection<SecretManagerConfig> vaultConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true);
    assertEquals(1, vaultConfigs.size());
    assertEquals(numOfSettingAttributes, vaultConfigs.iterator().next().getNumOfEncryptedValue());
  }

  @Test
  @Category(UnitTests.class)
  public void vaultEncryptionUpdateObject() throws IllegalAccessException {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncRecords + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    User user1 = User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName("user1").build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncRecords + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.count());
    EncryptedData encryptedData = query.get();
    assertEquals(vaultConfig.getUuid(), encryptedData.getKmsId());

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(2, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(user1.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user1.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user1.getName(), secretChangeLog.getUser().getName());
    assertEquals("Changed password", secretChangeLog.getDescription());

    secretChangeLog = changeLogs.get(1);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Created", secretChangeLog.getDescription());

    User user2 = User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName("user2").build();
    wingsPersistence.save(user2);
    UserThreadLocal.set(user2);
    wingsPersistence.save(savedAttribute);

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.count());

    changeLogs = secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(3, changeLogs.size());
    secretChangeLog = changeLogs.get(0);
    assertEquals(user2.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user2.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user2.getName(), secretChangeLog.getUser().getName());
    assertEquals("Changed password", secretChangeLog.getDescription());

    secretChangeLog = changeLogs.get(1);
    assertEquals(user1.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user1.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user1.getName(), secretChangeLog.getUser().getName());
    assertEquals("Changed password", secretChangeLog.getDescription());

    secretChangeLog = changeLogs.get(2);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Created", secretChangeLog.getDescription());
  }

  @Test
  @Category(UnitTests.class)
  public void vaultEncryptionUpdateFieldSettingAttribute() throws IllegalAccessException {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).count());
    assertEquals(numOfEncRecords + 1, wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).count());

    String updatedAppId = UUID.randomUUID().toString();
    wingsPersistence.updateField(SettingAttribute.class, savedAttributeId, "appId", updatedAppId);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(updatedAppId, updatedAttribute.getAppId());
    savedAttribute.setAppId(updatedAppId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).count());
    assertEquals(numOfEncRecords + 1, wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).count());

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                     .field("parentIds")
                                     .hasThisOne(savedAttributeId);
    assertEquals(1, query.count());

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);

    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Created", secretChangeLog.getDescription());

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = getAppDynamicsConfig(accountId, newPassWord);

    updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("value", newAppDynamicsConfig);

    User user1 =
        User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName(UUID.randomUUID().toString()).build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);

    query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                .field("parentIds")
                .hasThisOne(savedAttributeId);
    assertEquals(1, query.count());
    EncryptedData encryptedData = query.get();

    changeLogs = secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(2, changeLogs.size());
    secretChangeLog = changeLogs.get(0);
    assertEquals(user1.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user1.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user1.getName(), secretChangeLog.getUser().getName());
    assertEquals("Changed password", secretChangeLog.getDescription());

    secretChangeLog = changeLogs.get(1);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Created", secretChangeLog.getDescription());

    assertEquals(user.getUuid(), encryptedData.getCreatedBy().getUuid());
    assertEquals(userEmail, encryptedData.getCreatedBy().getEmail());
    assertEquals(userName, encryptedData.getCreatedBy().getName());

    updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(updatedAppId, updatedAttribute.getAppId());
    assertEquals(updatedName, updatedAttribute.getName());

    newAppDynamicsConfig.setPassword(null);
    assertEquals(newAppDynamicsConfig, updatedAttribute.getValue());
    newAppDynamicsConfig.setPassword(newPassWord.toCharArray());

    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).count());
    assertEquals(numOfEncRecords + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    User user2 =
        User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName(UUID.randomUUID().toString()).build();
    wingsPersistence.save(user2);
    UserThreadLocal.set(user2);
    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);

    query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                .field("parentIds")
                .hasThisOne(savedAttributeId);
    assertEquals(1, query.count());
    encryptedData = query.get();

    changeLogs = secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(3, changeLogs.size());
    secretChangeLog = changeLogs.get(0);
    assertEquals(user2.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user2.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user2.getName(), secretChangeLog.getUser().getName());
    assertEquals("Changed password", secretChangeLog.getDescription());

    secretChangeLog = changeLogs.get(1);
    assertEquals(user1.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user1.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user1.getName(), secretChangeLog.getUser().getName());
    assertEquals("Changed password", secretChangeLog.getDescription());

    secretChangeLog = changeLogs.get(2);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Created", secretChangeLog.getDescription());

    // test decryption
    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertNull(savedConfig.getPassword());
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertEquals(newPassWord, String.valueOf(savedConfig.getPassword()));
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void vaultEncryptionSaveServiceVariable() throws IllegalAccessException {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

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
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(numOfEncRecords + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    secretName = UUID.randomUUID().toString();
    secretValue = UUID.randomUUID().toString();
    secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", "newName");
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    assertEquals(numOfEncRecords + 2, wingsPersistence.createQuery(EncryptedData.class).count());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.count());

    // decrypt and verify
    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, workflowExecutionId, appId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Created", secretChangeLog.getDescription());
  }

  @Test
  @Category(UnitTests.class)
  public void vaultEncryptionSaveServiceVariableTemplate() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

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
    assertEquals(serviceVariable, savedAttribute);
    assertNull(savedAttribute.getValue());
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(numOfEncRecords + 1, wingsPersistence.createQuery(EncryptedData.class).count());
  }

  @Test
  @Category(UnitTests.class)
  public void vaultEncryptionDeleteSettingAttribute() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      String password = UUID.randomUUID().toString();
      final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
      SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

      wingsPersistence.save(settingAttribute);
      settingAttributes.add(settingAttribute);
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncRecords + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(settingAttributes.get(i));
      assertEquals(numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).count());
      assertEquals(numOfEncRecords + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).count());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void vaultEncryptionDeleteSettingAttributeQueryUuid() {
    VaultConfig vaultConfig = getVaultConfig(VAULT_TOKEN);
    vaultService.saveVaultConfig(accountId, vaultConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      String password = UUID.randomUUID().toString();
      final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
      SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

      wingsPersistence.save(settingAttribute);
      settingAttributes.add(settingAttribute);
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncRecords + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(accountId, SettingAttribute.class, settingAttributes.get(i).getUuid());
      assertEquals(numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).count());
      assertEquals(numOfEncRecords + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).count());
    }

    wingsPersistence.save(settingAttributes);
    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncRecords + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(
          SettingAttribute.class, settingAttributes.get(i).getAppId(), settingAttributes.get(i).getUuid());
      assertEquals(numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).count());
      assertEquals(numOfEncRecords + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).count());
    }
  }

  @Test
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
      assertEquals(numOfEncRecords + numOfSettingAttributes, query.count());
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null || data.getType() == SettingVariableTypes.VAULT) {
          continue;
        }
        encryptedData.add(data);
        assertEquals(fromConfig.getUuid(), data.getKmsId());
        assertEquals(accountId, data.getAccountId());
      }

      assertEquals(numOfSettingAttributes, encryptedData.size());

      VaultConfig toConfig = getVaultConfig(VAULT_TOKEN);
      vaultService.saveVaultConfig(accountId, toConfig);

      secretManager.transitionSecrets(
          accountId, EncryptionType.VAULT, fromConfig.getUuid(), EncryptionType.VAULT, toConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      query = wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId);

      assertEquals(numOfEncRecords + 1 + numOfSettingAttributes, query.count());
      encryptedData = new ArrayList<>();
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null || data.getType() == SettingVariableTypes.VAULT) {
          continue;
        }
        encryptedData.add(data);
        assertEquals(toConfig.getUuid(), data.getKmsId());
        assertEquals(accountId, data.getAccountId());
      }
      assertEquals(numOfSettingAttributes, encryptedData.size());

      // read the values and compare
      PageResponse<SettingAttribute> attributeQuery = wingsPersistence.query(
          SettingAttribute.class, aPageRequest().addFilter("accountId", Operator.EQ, accountId).build());
      assertEquals(numOfSettingAttributes, attributeQuery.size());
      for (SettingAttribute settingAttribute : attributeQuery) {
        assertEquals(encryptedEntities.get(settingAttribute.getUuid()), settingAttribute);
      }
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  @Repeat(times = 3, successes = 1)
  @Category(UnitTests.class)
  public void transitionAndDeleteVault() throws InterruptedException, IllegalAccessException {
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
      assertEquals(numOfEncRecords + numOfSettingAttributes, query.count());
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null || data.getType() == SettingVariableTypes.VAULT) {
          continue;
        }
        encryptedData.add(data);
        assertEquals(fromConfig.getUuid(), data.getKmsId());
        assertEquals(accountId, data.getAccountId());
      }

      assertEquals(numOfSettingAttributes, encryptedData.size());

      VaultConfig toConfig = getVaultConfig(VAULT_TOKEN);
      vaultService.saveVaultConfig(accountId, toConfig);

      assertEquals(
          2, secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true).size());
      try {
        vaultService.deleteVaultConfig(accountId, fromConfig.getUuid());
        fail("Was able to delete vault which has reference in encrypted secrets");
      } catch (WingsException e) {
        // expected
      }

      secretManager.transitionSecrets(
          accountId, EncryptionType.VAULT, fromConfig.getUuid(), EncryptionType.VAULT, toConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      vaultService.deleteVaultConfig(accountId, fromConfig.getUuid());
      assertEquals(
          1, secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.VAULT, true).size());

      query = wingsPersistence.createQuery(EncryptedData.class);
      assertEquals(numOfEncRecords + numOfSettingAttributes, query.count());
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
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

      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                       .filter(EncryptedDataKeys.type, SettingVariableTypes.APP_DYNAMICS);
      assertEquals(numOfSettingAttributes, query.count());
      for (EncryptedData data : query.asList()) {
        assertEquals(fromConfig.getUuid(), data.getKmsId());
        assertEquals(accountId, data.getAccountId());
        assertEquals(EncryptionType.KMS, data.getEncryptionType());
      }

      VaultConfig toConfig = getVaultConfig(VAULT_TOKEN);
      vaultService.saveVaultConfig(accountId, toConfig);

      secretManager.transitionSecrets(
          accountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.VAULT, toConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      query = wingsPersistence.createQuery(EncryptedData.class)
                  .filter(EncryptedDataKeys.type, SettingVariableTypes.APP_DYNAMICS);

      assertEquals(numOfSettingAttributes, query.count());
      for (EncryptedData data : query.asList()) {
        assertEquals(toConfig.getUuid(), data.getKmsId());
        assertEquals(accountId, data.getAccountId());
        assertEquals(EncryptionType.VAULT, data.getEncryptionType());
      }

      secretManager.transitionSecrets(
          accountId, EncryptionType.VAULT, toConfig.getUuid(), EncryptionType.KMS, fromConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      query = wingsPersistence.createQuery(EncryptedData.class)
                  .filter(EncryptedDataKeys.type, SettingVariableTypes.APP_DYNAMICS);

      assertEquals(numOfSettingAttributes, query.count());
      for (EncryptedData data : query.asList()) {
        assertEquals(fromConfig.getUuid(), data.getKmsId());
        assertEquals(accountId, data.getAccountId());
        assertEquals(EncryptionType.KMS, data.getEncryptionType());
      }

      // read the values and compare
      PageResponse<SettingAttribute> attributeQuery =
          wingsPersistence.query(SettingAttribute.class, aPageRequest().build());
      assertEquals(numOfSettingAttributes, attributeQuery.size());
      for (SettingAttribute settingAttribute : attributeQuery) {
        assertEquals(encryptedEntities.get(settingAttribute.getUuid()), settingAttribute);
      }
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
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
    String secretFileId =
        secretManager.saveFile(accountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToSave)));

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
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncRecords + 1,
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).count());

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                                                .filter(EncryptedDataKeys.type, CONFIG_FILE)
                                                .filter(EncryptedDataKeys.accountId, accountId)
                                                .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(encryptedFileData.get(0).getParentIds().isEmpty());
    // test update
    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManager.updateFile(
        accountId, newSecretName, encryptedUuid, null, new BoundedInputStream(new FileInputStream(fileToUpdate)));

    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncRecords + 1,
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).count());

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                            .filter(EncryptedDataKeys.accountId, accountId)
                            .filter(EncryptedDataKeys.type, CONFIG_FILE)
                            .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(encryptedFileData.get(0).getParentIds().isEmpty());

    int numOfAccess = 7;
    for (int i = 0; i < numOfAccess; i++) {
      configService.downloadForActivity(appId, configFileId, activity.getUuid());
    }
    List<SecretUsageLog> usageLogs =
        secretManager.getUsageLogs(aPageRequest().build(), accountId, encryptedUuid, CONFIG_FILE);
    assertEquals(numOfAccess, usageLogs.size());

    for (SecretUsageLog usageLog : usageLogs) {
      assertEquals(workflowName, usageLog.getWorkflowExecutionName());
      assertEquals(accountId, usageLog.getAccountId());
      assertEquals(envId, usageLog.getEnvId());
      assertEquals(appId, usageLog.getAppId());
    }
  }

  @Test
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
    assertNotNull(yamlRef);
    assertTrue(yamlRef.startsWith(EncryptionType.VAULT.getYamlName() + "://" + fromConfig.getName() + "/harness/"));
    assertTrue(yamlRef.contains("#value"));

    for (int i = 1; i < numOfSettingAttributes; i++) {
      appDynamicsConfig = getAppDynamicsConfig(accountId, null, yamlRef);
      settingAttribute = getSettingAttribute(appDynamicsConfig);

      attributeIds.add(wingsPersistence.save(settingAttribute));
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncRecords + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    List<EncryptedData> encryptedDatas = wingsPersistence.createQuery(EncryptedData.class)
                                             .filter(EncryptedDataKeys.encryptionType, EncryptionType.VAULT)
                                             .filter(EncryptedDataKeys.accountId, accountId)
                                             .asList();
    assertEquals(1, encryptedDatas.size());
    EncryptedData encryptedData = encryptedDatas.get(0);
    assertEquals(EncryptionType.VAULT, encryptedData.getEncryptionType());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(fromConfig.getUuid(), encryptedData.getKmsId());
    assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
    assertEquals(numOfSettingAttributes, encryptedData.getParentIds().size());
    assertEquals(attributeIds, encryptedData.getParentIds());

    for (String attributeId : attributeIds) {
      SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, attributeId);
      AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
      assertEquals(accountId, savedConfig.getAccountId());
      assertNull(savedConfig.getPassword());
      assertFalse(isBlank(savedConfig.getEncryptedPassword()));

      encryptionService.decrypt(
          savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
      assertEquals(password, String.valueOf(savedConfig.getPassword()));
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
        assertTrue(encryptedDatas.isEmpty());
      } else {
        assertEquals(1, encryptedDatas.size());
        encryptedData = encryptedDatas.get(0);
        assertEquals(EncryptionType.VAULT, encryptedData.getEncryptionType());
        assertEquals(accountId, encryptedData.getAccountId());
        assertTrue(encryptedData.isEnabled());
        assertEquals(fromConfig.getUuid(), encryptedData.getKmsId());
        assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
        assertEquals(numOfSettingAttributes - (i + 1), encryptedData.getParentIds().size());

        assertFalse(encryptedData.getParentIds().contains(attributeId));
        assertEquals(remainingAttrs, encryptedData.getParentIds());
      }
      i++;
    }
  }

  private Thread startTransitionListener() throws IllegalAccessException {
    transitionEventListener = new KmsTransitionEventListener();
    FieldUtils.writeField(transitionEventListener, "timer", new TimerScheduledExecutorService(), true);
    FieldUtils.writeField(transitionEventListener, "queueController", new ConfigurationController(1), true);
    FieldUtils.writeField(transitionEventListener, "queue", transitionKmsQueue, true);
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
