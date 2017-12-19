package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.settings.SettingValue.SettingVariableTypes.CONFIG_FILE;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.annotation.Encryptable;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Activity;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigOverrideType;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.User;
import software.wings.beans.UuidAware;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.core.queue.Queue;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.RealMongo;
import software.wings.rules.RepeatRule.Repeat;
import software.wings.security.EncryptionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.security.KmsServiceImpl;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/29/17.
 */
public class KmsTest extends WingsBaseTest {
  @Inject private KmsService kmsService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;
  @Inject private ConfigService configService;
  @Inject private EncryptionService encryptionService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  final int numOfEncryptedValsForKms = 3;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();
  private String appId;
  private String workflowExecutionId;
  private String workflowName;
  private String envId;
  private KmsTransitionEventListener transitionEventListener;

  @Before
  public void setup() {
    initMocks(this);
    appId = UUID.randomUUID().toString();
    workflowName = UUID.randomUUID().toString();
    envId = UUID.randomUUID().toString();
    workflowExecutionId = wingsPersistence.save(
        WorkflowExecutionBuilder.aWorkflowExecution().withName(workflowName).withEnvId(envId).build());
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class)))
        .thenReturn(new SecretManagementDelegateServiceImpl());
    setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(secretManager, "kmsService", kmsService);
    setInternalState(wingsPersistence, "secretManager", secretManager);
    setInternalState(configService, "secretManager", secretManager);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);
  }

  @Test
  public void getKmsConfigGlobal() throws IOException {
    String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(Base.GLOBAL_ACCOUNT_ID);

    KmsConfig savedConfig = kmsService.getSecretConfig(UUID.randomUUID().toString());
    assertNull(savedConfig);

    kmsService.saveGlobalKmsConfig(accountId, kmsConfig);

    savedConfig = kmsService.getSecretConfig(UUID.randomUUID().toString());
    kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(Base.GLOBAL_ACCOUNT_ID);
    assertEquals(kmsConfig, savedConfig);
  }

  @Test
  public void validateConfig() throws IOException {
    String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setSecretKey(UUID.randomUUID().toString());

    try {
      kmsService.saveKmsConfig(kmsConfig.getAccountId(), kmsConfig);
      fail("Saved invalid kms config");
    } catch (WingsException e) {
      assertEquals(ErrorCode.KMS_OPERATION_ERROR, e.getResponseMessageList().get(0).getCode());
    }
  }

  @Test
  public void getKmsConfigForAccount() throws IOException {
    String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);

    kmsService.saveKmsConfig(kmsConfig.getAccountId(), kmsConfig);

    KmsConfig savedConfig = kmsService.getSecretConfig(kmsConfig.getAccountId());
    kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    assertEquals(kmsConfig, savedConfig);
  }

  @Test
  public void saveAndEditConfig() throws IOException {
    String accountId = UUID.randomUUID().toString();
    String name = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setName(name);
    kmsConfig.setAccountId(accountId);

    kmsService.saveKmsConfig(kmsConfig.getAccountId(), kmsConfig);

    KmsConfig savedConfig = kmsService.getSecretConfig(kmsConfig.getAccountId());
    kmsConfig = getKmsConfig();
    kmsConfig.setName(name);
    kmsConfig.setAccountId(accountId);
    assertEquals(kmsConfig, savedConfig);
    assertEquals(name, savedConfig.getName());
    List<EncryptedData> encryptedDataList = wingsPersistence.createQuery(EncryptedData.class).asList();
    assertEquals(numOfEncryptedValsForKms, encryptedDataList.size());
    for (EncryptedData encryptedData : encryptedDataList) {
      assertTrue(encryptedData.getName().equals(name + "_accessKey")
          || encryptedData.getName().equals(name + "_secretKey") || encryptedData.getName().equals(name + "_arn"));
      assertEquals(1, encryptedData.getParentIds().size());
      assertEquals(savedConfig.getUuid(), encryptedData.getParentIds().iterator().next());
    }

    name = UUID.randomUUID().toString();
    kmsConfig = getKmsConfig();
    savedConfig.setAccessKey(kmsConfig.getAccessKey());
    savedConfig.setSecretKey(kmsConfig.getSecretKey());
    savedConfig.setKmsArn(kmsConfig.getKmsArn());
    savedConfig.setName(name);
    kmsService.saveKmsConfig(accountId, savedConfig);
    encryptedDataList = wingsPersistence.createQuery(EncryptedData.class).asList();
    assertEquals(numOfEncryptedValsForKms, encryptedDataList.size());
    for (EncryptedData encryptedData : encryptedDataList) {
      assertTrue(encryptedData.getName().equals(name + "_accessKey")
          || encryptedData.getName().equals(name + "_secretKey") || encryptedData.getName().equals(name + "_arn"));
      assertEquals(1, encryptedData.getParentIds().size());
      assertEquals(savedConfig.getUuid(), encryptedData.getParentIds().iterator().next());
    }
  }

  @Test
  public void localNullEncryption() throws Exception {
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, null, null);
    assertNull(encryptedData.getEncryptedValue());
    assertFalse(StringUtils.isBlank(encryptedData.getEncryptionKey()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, null);
    assertNull(decryptedValue);
  }

  @Test
  public void localEncryption() throws Exception {
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt.toCharArray(), null, null);
    assertNotEquals(keyToEncrypt, new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, null);
    assertEquals(keyToEncrypt, new String(decryptedValue));
  }

  @Test
  public void kmsNullEncryption() throws Exception {
    final KmsConfig kmsConfig = getKmsConfig();
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, UUID.randomUUID().toString(), kmsConfig);
    assertNull(encryptedData.getEncryptedValue());
    assertFalse(StringUtils.isBlank(encryptedData.getEncryptionKey()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, kmsConfig);
    assertNull(decryptedValue);
  }

  @Test
  public void kmsEncryption() throws Exception {
    final KmsConfig kmsConfig = getKmsConfig();
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData =
        kmsService.encrypt(keyToEncrypt.toCharArray(), UUID.randomUUID().toString(), kmsConfig);
    assertNotEquals(keyToEncrypt, new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, kmsConfig);
    assertEquals(keyToEncrypt, new String(decryptedValue));
  }

  @Test
  public void localEncryptionWhileSaving() {
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(UUID.randomUUID().toString())
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(password.toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(appDynamicsConfig.getAccountId())
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    appDynamicsConfig.setPassword(password.toCharArray());
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertNotNull(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword());
    assertNull(((AppDynamicsConfig) savedAttribute.getValue()).getPassword());
    encryptionService.decrypt((Encryptable) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((Encryptable) savedAttribute.getValue(), workflowExecutionId, appId));
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertNotNull(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword());
    assertEquals(password, new String(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()));
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(1, query.asList().size());
  }

  @Test
  public void localEncryptionWhileSavingNullEncryptedData() {
    final ArtifactoryConfig artifactoryConfig = ArtifactoryConfig.builder()
                                                    .accountId(UUID.randomUUID().toString())
                                                    .artifactoryUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(null)
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(artifactoryConfig.getAccountId())
                                            .withValue(artifactoryConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertNotNull(((ArtifactoryConfig) savedAttribute.getValue()).getEncryptedPassword());
    assertNull(((ArtifactoryConfig) savedAttribute.getValue()).getPassword());
    encryptionService.decrypt((Encryptable) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((Encryptable) savedAttribute.getValue(), workflowExecutionId, appId));
    assertEquals(artifactoryConfig, savedAttribute.getValue());
    assertNotNull(((ArtifactoryConfig) savedAttribute.getValue()).getEncryptedPassword());
    assertNull(((ArtifactoryConfig) savedAttribute.getValue()).getPassword());
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(1, query.asList().size());
  }

  @Test
  public void kmsEncryptionWhileSavingFeatureDisabled() {
    final String accountId = UUID.randomUUID().toString();
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(password.toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(appDynamicsConfig.getAccountId())
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    appDynamicsConfig.setPassword(password.toCharArray());
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertNotNull((savedConfig).getEncryptedPassword());
    assertNull((savedConfig).getPassword());
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertEquals(appDynamicsConfig, savedConfig);

    assertNotNull(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword());
    assertEquals(password, new String(savedConfig.getPassword()));
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(1, query.asList().size());
  }

  @Test
  public void enableKmsAfterSaving() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(password.toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertNull(((AppDynamicsConfig) savedAttribute.getValue()).getPassword());
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertFalse(StringUtils.isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()));
    enableKmsFeatureFlag();
    encryptionService.decrypt((Encryptable) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((Encryptable) savedAttribute.getValue(), workflowExecutionId, appId));
    assertEquals(password, new String(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()));
  }

  @Test
  public void kmsEncryptionWhileSaving() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertFalse(StringUtils.isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()));

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(settingAttribute.getUuid());
    assertEquals(1, query.asList().size());
    EncryptedData encryptedData = query.asList().get(0);
    assertEquals(kmsConfig.getUuid(), encryptedData.getKmsId());
    assertEquals(user.getUuid(), encryptedData.getCreatedBy().getUuid());
    assertEquals(userEmail, encryptedData.getCreatedBy().getEmail());
    assertEquals(userName, encryptedData.getCreatedBy().getName());

    List<SecretChangeLog> changeLogs = secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());

    query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(numOfEncryptedValsForKms + 1, query.asList().size());
  }

  @Test
  public void secretUsageLog() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);

    Query<EncryptedData> encryptedDataQuery =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(settingAttribute.getUuid());
    assertEquals(1, encryptedDataQuery.asList().size());
    EncryptedData encryptedData = encryptedDataQuery.asList().get(0);

    secretManager.getEncryptionDetails((Encryptable) savedAttribute.getValue(), appId, workflowExecutionId);
    Query<SecretUsageLog> query = wingsPersistence.createQuery(SecretUsageLog.class);
    assertEquals(1, query.asList().size());
    SecretUsageLog usageLog = query.asList().get(0);
    assertEquals(accountId, usageLog.getAccountId());
    assertEquals(workflowExecutionId, usageLog.getWorkflowExecutionId());
    assertEquals(appId, usageLog.getAppId());
    assertEquals(encryptedData.getUuid(), usageLog.getEncryptedDataId());
  }

  @Test
  public void kmsEncryptionSaveMultiple() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      String password = "password" + i;
      final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                      .accountId(accountId)
                                                      .controllerUrl(UUID.randomUUID().toString())
                                                      .username(UUID.randomUUID().toString())
                                                      .password(password.toCharArray())
                                                      .accountname(UUID.randomUUID().toString())
                                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(appDynamicsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CONNECTOR)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      settingAttributes.add(settingAttribute);
    }
    wingsPersistence.save(settingAttributes);

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());
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
      assertEquals(1, query.asList().size());
      assertEquals(kmsConfig.getUuid(), query.asList().get(0).getKmsId());
    }

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(1, kmsConfigs.size());
    assertEquals(numOfSettingAttributes, kmsConfigs.iterator().next().getNumOfEncryptedValue());
  }

  @Test
  public void noKmsEncryptionUpdateObject() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    User user1 = User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName("user1").build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.asList().size());
    EncryptedData encryptedData = query.asList().get(0);
    assertNull(encryptedData.getKmsId());

    List<SecretChangeLog> changeLogs = secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
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
    assertEquals(1, query.asList().size());

    changeLogs = secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
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

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = AppDynamicsConfig.builder()
                                                       .accountId(accountId)
                                                       .controllerUrl(UUID.randomUUID().toString())
                                                       .username(UUID.randomUUID().toString())
                                                       .password(newPassWord.toCharArray())
                                                       .accountname(UUID.randomUUID().toString())
                                                       .build();

    String updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("value", newAppDynamicsConfig);

    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);
    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.asList().size());

    encryptedData = query.asList().get(0);
    assertEquals(accountId, encryptedData.getAccountId());
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());
    assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
    assertNull(encryptedData.getKmsId());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());

    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(updatedName, savedAttribute.getName());
    assertEquals(updatedAppId, savedAttribute.getAppId());

    AppDynamicsConfig updatedAppdynamicsConfig = (AppDynamicsConfig) savedAttribute.getValue();
    encryptionService.decrypt(
        updatedAppdynamicsConfig, secretManager.getEncryptionDetails(updatedAppdynamicsConfig, null, null));
    assertEquals(newPassWord, String.valueOf(updatedAppdynamicsConfig.getPassword()));
  }

  @Test
  public void noKmsEncryptionUpdateServiceVariable() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);

    ServiceVariable serviceVariable = ServiceVariable.builder()
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

    String savedServiceVariableId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedServiceVariableId);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
    assertNull(savedVariable.getValue());
    assertNotNull(savedVariable.getEncryptedValue());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedServiceVariableId);
    assertEquals(1, query.asList().size());
    EncryptedData encryptedData = query.asList().get(0);
    assertNull(encryptedData.getKmsId());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(serviceVariable, appId, workflowExecutionId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));

    secretName = UUID.randomUUID().toString();
    secretValue = UUID.randomUUID().toString();
    secretId = secretManager.saveSecret(accountId, secretName, secretValue);

    String updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());

    wingsPersistence.updateFields(ServiceVariable.class, savedServiceVariableId, keyValuePairs);
    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedServiceVariableId);
    assertEquals(1, query.asList().size());

    encryptedData = query.asList().get(0);
    assertEquals(accountId, encryptedData.getAccountId());
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());
    assertEquals(SettingVariableTypes.SECRET_TEXT, encryptedData.getType());
    assertNull(encryptedData.getKmsId());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedServiceVariableId);
    assertEquals(updatedName, savedVariable.getName());
    assertEquals(updatedAppId, savedVariable.getAppId());
    assertEquals(secretId, savedVariable.getEncryptedValue());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));
  }

  @Test
  public void kmsEncryptionUpdateObject() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    User user1 = User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName("user1").build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.asList().size());
    EncryptedData encryptedData = query.asList().get(0);
    assertEquals(kmsConfig.getUuid(), encryptedData.getKmsId());

    List<SecretChangeLog> changeLogs = secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
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
    assertEquals(1, query.asList().size());

    changeLogs = secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
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

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = AppDynamicsConfig.builder()
                                                       .accountId(accountId)
                                                       .controllerUrl(UUID.randomUUID().toString())
                                                       .username(UUID.randomUUID().toString())
                                                       .password(newPassWord.toCharArray())
                                                       .accountname(UUID.randomUUID().toString())
                                                       .build();

    String updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("value", newAppDynamicsConfig);

    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);
    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.asList().size());

    encryptedData = query.asList().get(0);
    assertEquals(accountId, encryptedData.getAccountId());
    assertEquals(EncryptionType.KMS, encryptedData.getEncryptionType());
    assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
    assertEquals(kmsConfig.getUuid(), encryptedData.getKmsId());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());

    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(updatedName, savedAttribute.getName());
    assertEquals(updatedAppId, savedAttribute.getAppId());

    AppDynamicsConfig updatedAppdynamicsConfig = (AppDynamicsConfig) savedAttribute.getValue();
    encryptionService.decrypt(
        updatedAppdynamicsConfig, secretManager.getEncryptionDetails(updatedAppdynamicsConfig, null, null));
    assertEquals(newPassWord, String.valueOf(updatedAppdynamicsConfig.getPassword()));
  }

  @Test
  @Ignore
  public void kmsEncryptionUpdateFieldSettingAttribute() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    String updatedAppId = UUID.randomUUID().toString();
    wingsPersistence.updateField(SettingAttribute.class, savedAttributeId, "appId", updatedAppId);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(updatedAppId, updatedAttribute.getAppId());
    savedAttribute.setAppId(updatedAppId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.asList().size());

    List<SecretChangeLog> changeLogs = secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);

    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Created", secretChangeLog.getDescription());

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = AppDynamicsConfig.builder()
                                                       .accountId(accountId)
                                                       .controllerUrl(UUID.randomUUID().toString())
                                                       .username(UUID.randomUUID().toString())
                                                       .password(newPassWord.toCharArray())
                                                       .accountname(UUID.randomUUID().toString())
                                                       .build();

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

    // test decryption
    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertNull(savedConfig.getPassword());
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertEquals(newPassWord, String.valueOf(savedConfig.getPassword()));

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.asList().size());
    EncryptedData encryptedData = query.asList().get(0);

    changeLogs = secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
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

    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    User user2 =
        User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName(UUID.randomUUID().toString()).build();
    wingsPersistence.save(user2);
    UserThreadLocal.set(user2);
    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.asList().size());
    encryptedData = query.asList().get(0);

    changeLogs = secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.APP_DYNAMICS);
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
  public void saveServiceVariableNoKMS() throws IOException {
    final String accountId = UUID.randomUUID().toString();

    String value = UUID.randomUUID().toString();
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
                                                .value(value.toCharArray())
                                                .type(Type.TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(value, String.valueOf(savedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    // update to encrypt the variable
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.ENCRYPTED_TEXT, savedAttribute.getType());
    assertNull(savedAttribute.getValue());
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, workflowExecutionId, appId));
    assertEquals(secretValue, String.valueOf(savedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.TEXT);
    keyValuePairs.put("value", "unencrypted".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.TEXT, savedAttribute.getType());
    assertEquals("unencrypted", String.valueOf(savedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  public void saveServiceVariableNoEncryption() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    String value = UUID.randomUUID().toString();
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
                                                .value(value.toCharArray())
                                                .type(Type.TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(value, String.valueOf(savedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    // update to encrypt the variable
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.ENCRYPTED_TEXT, savedAttribute.getType());
    assertNull(savedAttribute.getValue());
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, workflowExecutionId, appId));
    assertEquals(secretValue, String.valueOf(savedAttribute.getValue()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.TEXT);
    keyValuePairs.put("value", "unencrypted".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.TEXT, savedAttribute.getType());
    assertEquals("unencrypted", new String(savedAttribute.getValue()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
    assertNull(wingsPersistence.createQuery(EncryptedData.class)
                   .field("type")
                   .equal(SettingVariableTypes.SECRET_TEXT)
                   .asList()
                   .get(0)
                   .getParentIds());
  }

  @Test
  public void kmsEncryptionSaveServiceVariable() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);

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
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    secretName = UUID.randomUUID().toString();
    secretValue = UUID.randomUUID().toString();
    secretId = secretManager.saveSecret(accountId, secretName, secretValue);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", "newName");
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    assertEquals(numOfEncryptedValsForKms + 2, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.asList().size());

    // decrypt and verify
    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, workflowExecutionId, appId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Created", secretChangeLog.getDescription());
  }

  @Test
  public void kmsEncryptionSaveServiceVariableTemplate() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);

    String serviceId = wingsPersistence.save(Service.Builder.aService().withName(UUID.randomUUID().toString()).build());
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
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId));
    assertEquals(secretValue, String.valueOf(savedAttribute.getValue()));

    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  public void kmsEncryptionUpdateServiceVariable() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);

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
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    String updatedEnvId = UUID.randomUUID().toString();
    wingsPersistence.updateField(ServiceVariable.class, savedAttributeId, "envId", updatedEnvId);

    ServiceVariable updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    savedAttribute.setEnvId(updatedEnvId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    updatedEnvId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    String updatedSecretName = UUID.randomUUID().toString();
    String updatedSecretValue = UUID.randomUUID().toString();
    String updatedSecretId = secretManager.saveSecret(accountId, updatedSecretName, updatedSecretValue);

    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("envId", updatedEnvId);
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", updatedSecretId.toCharArray());

    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    assertEquals(updatedName, updatedAttribute.getName());
    assertNull(updatedAttribute.getValue());
    encryptionService.decrypt(
        updatedAttribute, secretManager.getEncryptionDetails(updatedAttribute, workflowExecutionId, appId));
    assertEquals(updatedSecretValue, String.valueOf(updatedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 2, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  public void kmsEncryptionDeleteSettingAttribute() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                      .accountId(accountId)
                                                      .controllerUrl(UUID.randomUUID().toString())
                                                      .username(UUID.randomUUID().toString())
                                                      .password(UUID.randomUUID().toString().toCharArray())
                                                      .accountname(UUID.randomUUID().toString())
                                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(appDynamicsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CONNECTOR)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      settingAttributes.add(settingAttribute);
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(settingAttributes.get(i));
      assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }

  @Test
  public void kmsEncryptionDeleteSettingAttributeQueryUuid() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                      .accountId(accountId)
                                                      .controllerUrl(UUID.randomUUID().toString())
                                                      .username(UUID.randomUUID().toString())
                                                      .password(UUID.randomUUID().toString().toCharArray())
                                                      .accountname(UUID.randomUUID().toString())
                                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(appDynamicsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CONNECTOR)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      settingAttributes.add(settingAttribute);
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(SettingAttribute.class, settingAttributes.get(i).getUuid());
      assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }

    wingsPersistence.save(settingAttributes);
    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(
          SettingAttribute.class, settingAttributes.get(i).getAppId(), settingAttributes.get(i).getUuid());
      assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }

  @Test
  public void kmsEncryptionDeleteSettingAttributeQuery() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                      .accountId(accountId)
                                                      .controllerUrl(UUID.randomUUID().toString())
                                                      .username(UUID.randomUUID().toString())
                                                      .password(UUID.randomUUID().toString().toCharArray())
                                                      .accountname(UUID.randomUUID().toString())
                                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(appDynamicsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CONNECTOR)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      settingAttributes.add(settingAttribute);
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Set<String> idsToDelete = new HashSet<>();
    idsToDelete.add(settingAttributes.get(0).getUuid());
    idsToDelete.add(settingAttributes.get(1).getUuid());
    Query<SettingAttribute> query =
        wingsPersistence.createQuery(SettingAttribute.class).field(Mapper.ID_KEY).hasAnyOf(idsToDelete);
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(query);
      assertEquals(numOfSettingAttributes - idsToDelete.size(),
          wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - idsToDelete.size(),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }

  @Test
  public void kmsEncryptionSaveGlobalConfig() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();

    kmsService.saveKmsConfig(Base.GLOBAL_ACCOUNT_ID, kmsConfig);
    assertEquals(1, wingsPersistence.createQuery(KmsConfig.class).asList().size());

    KmsConfig savedKmsConfig = kmsService.getSecretConfig(accountId);
    assertNotNull(savedKmsConfig);

    kmsConfig = getKmsConfig();
    assertEquals(Base.GLOBAL_ACCOUNT_ID, savedKmsConfig.getAccountId());
    assertEquals(kmsConfig.getAccessKey(), savedKmsConfig.getAccessKey());
    assertEquals(kmsConfig.getSecretKey(), savedKmsConfig.getSecretKey());
    assertEquals(kmsConfig.getKmsArn(), savedKmsConfig.getKmsArn());

    KmsConfig encryptedKms = wingsPersistence.getDatastore().createQuery(KmsConfig.class).asList().get(0);

    assertNotEquals(encryptedKms.getAccessKey(), savedKmsConfig.getAccessKey());
    assertNotEquals(encryptedKms.getSecretKey(), savedKmsConfig.getSecretKey());
    assertNotEquals(encryptedKms.getKmsArn(), savedKmsConfig.getKmsArn());
  }

  @Test
  public void listEncryptedValues() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    int numOfSettingAttributes = 5;
    List<Object> encryptedEntities = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                      .accountId(accountId)
                                                      .controllerUrl(UUID.randomUUID().toString())
                                                      .username(UUID.randomUUID().toString())
                                                      .password(UUID.randomUUID().toString().toCharArray())
                                                      .accountname(UUID.randomUUID().toString())
                                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(appDynamicsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CONNECTOR)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      appDynamicsConfig.setPassword(null);
      settingAttribute.setEncryptionType(EncryptionType.KMS);
      settingAttribute.setEncryptedBy(kmsConfig.getName());
      encryptedEntities.add(settingAttribute);
    }

    kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);

    Collection<UuidAware> encryptedValues = secretManager.listEncryptedValues(accountId);
    assertEquals(encryptedEntities.size(), encryptedValues.size());
    assertTrue(encryptedEntities.containsAll(encryptedValues));

    for (UuidAware encryptedValue : encryptedValues) {
      assertEquals(EncryptionType.KMS, ((SettingAttribute) encryptedValue).getEncryptionType());
    }
  }

  @Test
  public void listKmsConfigMultiple() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig1 = getKmsConfig();
    kmsConfig1.setDefault(true);
    kmsConfig1.setName(UUID.randomUUID().toString());
    kmsService.saveKmsConfig(accountId, kmsConfig1);
    kmsConfig1.setAccessKey(getKmsConfig().getAccessKey());
    kmsConfig1.setKmsArn(getKmsConfig().getKmsArn());

    KmsConfig kmsConfig2 = getKmsConfig();
    kmsConfig2.setDefault(false);
    kmsConfig2.setName(UUID.randomUUID().toString());
    kmsService.saveKmsConfig(accountId, kmsConfig2);
    kmsConfig2.setAccessKey(getKmsConfig().getAccessKey());
    kmsConfig2.setKmsArn(getKmsConfig().getKmsArn());

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(2, kmsConfigs.size());

    int defaultConfig = 0;
    int nonDefaultConfig = 0;

    for (KmsConfig actualConfig : kmsConfigs) {
      if (actualConfig.isDefault()) {
        defaultConfig++;
        assertEquals(kmsConfig1.getName(), actualConfig.getName());
        assertEquals(kmsConfig1.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(kmsConfig1.getKmsArn(), actualConfig.getKmsArn());
        assertEquals(KmsServiceImpl.SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(StringUtils.isEmpty(actualConfig.getUuid()));
        assertEquals(accountId, actualConfig.getAccountId());
      } else {
        nonDefaultConfig++;
        assertEquals(kmsConfig2.getName(), actualConfig.getName());
        assertEquals(kmsConfig2.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(kmsConfig2.getKmsArn(), actualConfig.getKmsArn());
        assertEquals(KmsServiceImpl.SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(StringUtils.isEmpty(actualConfig.getUuid()));
        assertEquals(accountId, actualConfig.getAccountId());
      }
    }

    assertEquals(1, defaultConfig);
    assertEquals(1, nonDefaultConfig);

    kmsConfig2.setSecretKey(getKmsConfig().getSecretKey());
    kmsConfig2.setName(UUID.randomUUID().toString());
    kmsConfig2.setDefault(true);

    kmsService.saveKmsConfig(accountId, kmsConfig2);
    kmsConfig2.setAccessKey(getKmsConfig().getAccessKey());
    kmsConfig2.setKmsArn(getKmsConfig().getKmsArn());

    kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(2, kmsConfigs.size());

    defaultConfig = 0;
    nonDefaultConfig = 0;
    for (KmsConfig actualConfig : kmsConfigs) {
      if (actualConfig.isDefault()) {
        defaultConfig++;
        assertEquals(kmsConfig2.getName(), actualConfig.getName());
        assertEquals(kmsConfig2.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(kmsConfig2.getKmsArn(), actualConfig.getKmsArn());
        assertEquals(KmsServiceImpl.SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(StringUtils.isEmpty(actualConfig.getUuid()));
        assertEquals(accountId, actualConfig.getAccountId());
      } else {
        nonDefaultConfig++;
        assertEquals(kmsConfig1.getName(), actualConfig.getName());
        assertEquals(kmsConfig1.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(kmsConfig1.getKmsArn(), actualConfig.getKmsArn());
        assertEquals(KmsServiceImpl.SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(StringUtils.isEmpty(actualConfig.getUuid()));
        assertEquals(accountId, actualConfig.getAccountId());
      }
    }

    assertEquals(1, defaultConfig);
    assertEquals(1, nonDefaultConfig);
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void listKmsGlobalDefault() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setName("Global config");

    globalKmsConfig.setDefault(false);
    kmsService.saveGlobalKmsConfig(accountId, globalKmsConfig);

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(1, kmsConfigs.size());
    assertTrue(kmsConfigs.iterator().next().isDefault());

    int numOfKms = 10;
    for (int i = 1; i <= numOfKms; i++) {
      KmsConfig kmsConfig = getKmsConfig();
      kmsConfig.setDefault(true);
      kmsConfig.setName("kms" + i);
      kmsService.saveKmsConfig(accountId, kmsConfig);
    }

    kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(numOfKms + 1, kmsConfigs.size());

    int kmsNum = numOfKms;
    for (KmsConfig kmsConfig : kmsConfigs) {
      if (kmsConfig.getAccountId().equals(Base.GLOBAL_ACCOUNT_ID)) {
        assertFalse(kmsConfig.isDefault());
        assertEquals("Global config", kmsConfig.getName());
      } else {
        assertEquals("kms" + kmsNum, kmsConfig.getName());
      }
      if (kmsNum == numOfKms) {
        assertTrue(kmsConfig.isDefault());
      } else {
        assertFalse(kmsConfig.isDefault());
      }
      kmsNum--;
    }

    // delete the default and global should become default
    kmsService.deleteKmsConfig(accountId, kmsConfigs.iterator().next().getUuid());
    kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(numOfKms, kmsConfigs.size());

    int defaultSet = 0;
    kmsNum = numOfKms - 1;
    for (KmsConfig kmsConfig : kmsConfigs) {
      if (kmsConfig.getAccountId().equals(Base.GLOBAL_ACCOUNT_ID)) {
        assertTrue(kmsConfig.isDefault());
        assertEquals("Global config", kmsConfig.getName());
        defaultSet++;
      } else {
        assertFalse(kmsConfig.isDefault());
        assertEquals("kms" + kmsNum, kmsConfig.getName());
      }
      kmsNum--;
    }

    assertEquals(1, defaultSet);
  }

  @Test
  public void listKmsConfigOrder() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    int numOfKms = 10;
    for (int i = 1; i <= numOfKms; i++) {
      KmsConfig kmsConfig = getKmsConfig();
      kmsConfig.setDefault(true);
      kmsConfig.setName("kms" + i);
      kmsService.saveKmsConfig(accountId, kmsConfig);
    }

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(numOfKms, kmsConfigs.size());

    int kmsNum = numOfKms;
    for (KmsConfig kmsConfig : kmsConfigs) {
      if (kmsNum == numOfKms) {
        assertTrue(kmsConfig.isDefault());
      } else {
        assertFalse(kmsConfig.isDefault());
      }
      assertEquals("kms" + kmsNum, kmsConfig.getName());
      kmsNum--;
    }
  }

  @Test
  public void listKmsConfigHasDefault() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setDefault(false);
    globalKmsConfig.setName("global-kms-config");
    kmsService.saveGlobalKmsConfig(accountId, globalKmsConfig);
    globalKmsConfig = getKmsConfig();
    globalKmsConfig.setDefault(false);
    globalKmsConfig.setName("global-kms-config");

    KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    kmsConfig = getKmsConfig();

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(2, kmsConfigs.size());

    int defaultConfig = 0;
    int accountConfig = 0;

    for (KmsConfig actualConfig : kmsConfigs) {
      if (actualConfig.isDefault()) {
        accountConfig++;
        assertEquals(kmsConfig.getName(), actualConfig.getName());
        assertEquals(kmsConfig.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(kmsConfig.getKmsArn(), actualConfig.getKmsArn());
        assertEquals(KmsServiceImpl.SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(StringUtils.isEmpty(actualConfig.getUuid()));
      } else {
        defaultConfig++;
        assertEquals(globalKmsConfig.getName(), actualConfig.getName());
        assertEquals(globalKmsConfig.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(globalKmsConfig.getKmsArn(), actualConfig.getKmsArn());
        assertEquals(KmsServiceImpl.SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(StringUtils.isEmpty(actualConfig.getUuid()));
        assertEquals(Base.GLOBAL_ACCOUNT_ID, actualConfig.getAccountId());
      }
    }

    assertEquals(1, defaultConfig);
    assertEquals(1, accountConfig);
  }

  @Test
  public void listKmsConfig() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    kmsConfig = getKmsConfig();

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(1, kmsConfigs.size());
    KmsConfig actualConfig = kmsConfigs.iterator().next();
    assertEquals(kmsConfig.getName(), actualConfig.getName());
    assertEquals(kmsConfig.getAccessKey(), actualConfig.getAccessKey());
    assertEquals(kmsConfig.getKmsArn(), actualConfig.getKmsArn());
    assertEquals(KmsServiceImpl.SECRET_MASK, actualConfig.getSecretKey());
    assertFalse(StringUtils.isEmpty(actualConfig.getUuid()));
    assertTrue(actualConfig.isDefault());

    // add another kms
    String name = UUID.randomUUID().toString();
    kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsConfig.setName(name);
    kmsService.saveKmsConfig(accountId, kmsConfig);

    kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(2, kmsConfigs.size());

    int defaultPresent = 0;
    for (KmsConfig config : kmsConfigs) {
      if (config.getName().equals(name)) {
        defaultPresent++;
        assertTrue(config.isDefault());
      } else {
        assertFalse(config.isDefault());
      }
    }

    assertEquals(1, defaultPresent);

    name = UUID.randomUUID().toString();
    kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsConfig.setName(name);
    kmsService.saveKmsConfig(accountId, kmsConfig);

    kmsConfigs = kmsService.listKmsConfigs(accountId, true);
    assertEquals(3, kmsConfigs.size());

    defaultPresent = 0;
    for (KmsConfig config : kmsConfigs) {
      if (config.getName().equals(name)) {
        defaultPresent++;
        assertTrue(config.isDefault());
      } else {
        assertFalse(config.isDefault());
      }
    }
    assertEquals(1, defaultPresent);
  }

  @Test
  public void transitionKms() throws IOException, InterruptedException {
    Thread listenerThread = startTransitionListener();
    try {
      final String accountId = UUID.randomUUID().toString();
      KmsConfig fromConfig = getKmsConfig();
      kmsService.saveKmsConfig(accountId, fromConfig);
      enableKmsFeatureFlag();

      int numOfSettingAttributes = 5;
      Map<String, SettingAttribute> encryptedEntities = new HashMap<>();
      for (int i = 0; i < numOfSettingAttributes; i++) {
        String password = UUID.randomUUID().toString();
        final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                        .accountId(accountId)
                                                        .controllerUrl(UUID.randomUUID().toString())
                                                        .username(UUID.randomUUID().toString())
                                                        .password(password.toCharArray())
                                                        .accountname(UUID.randomUUID().toString())
                                                        .build();

        SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                                .withAccountId(accountId)
                                                .withValue(appDynamicsConfig)
                                                .withAppId(UUID.randomUUID().toString())
                                                .withCategory(Category.CONNECTOR)
                                                .withEnvId(UUID.randomUUID().toString())
                                                .withName(UUID.randomUUID().toString())
                                                .build();

        wingsPersistence.save(settingAttribute);
        appDynamicsConfig.setPassword(null);
        encryptedEntities.put(settingAttribute.getUuid(), settingAttribute);
      }

      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      List<EncryptedData> encryptedData = new ArrayList<>();
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes, query.asList().size());
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null) {
          continue;
        }
        encryptedData.add(data);
        assertEquals(fromConfig.getUuid(), data.getKmsId());
        assertEquals(accountId, data.getAccountId());
      }

      assertEquals(numOfSettingAttributes, encryptedData.size());

      KmsConfig toKmsConfig = getKmsConfig();
      toKmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/e1aebd89-277b-4ec7-a4e9-9a238f8b2594");
      kmsService.saveKmsConfig(accountId, toKmsConfig);

      secretManager.transitionSecrets(
          accountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.KMS, toKmsConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      query = wingsPersistence.createQuery(EncryptedData.class);
      // 2 kms configs have been saved so far
      assertEquals(2 * numOfEncryptedValsForKms + numOfSettingAttributes, query.asList().size());
      encryptedData = new ArrayList<>();
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null) {
          continue;
        }
        encryptedData.add(data);
        assertEquals(toKmsConfig.getUuid(), data.getKmsId());
        assertEquals(accountId, data.getAccountId());
      }
      assertEquals(numOfSettingAttributes, encryptedData.size());

      // read the values and compare
      PageResponse<SettingAttribute> attributeQuery =
          wingsPersistence.query(SettingAttribute.class, Builder.aPageRequest().build());
      assertEquals(numOfSettingAttributes, attributeQuery.size());
      for (SettingAttribute settingAttribute : attributeQuery) {
        assertEquals(encryptedEntities.get(settingAttribute.getUuid()), settingAttribute);
      }
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  public void transitionAndDeleteKms() throws IOException, InterruptedException {
    Thread listenerThread = startTransitionListener();
    try {
      final String accountId = UUID.randomUUID().toString();
      KmsConfig fromConfig = getKmsConfig();
      kmsService.saveKmsConfig(accountId, fromConfig);
      enableKmsFeatureFlag();

      int numOfSettingAttributes = 5;
      Map<String, SettingAttribute> encryptedEntities = new HashMap<>();
      for (int i = 0; i < numOfSettingAttributes; i++) {
        String password = UUID.randomUUID().toString();
        final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                        .accountId(accountId)
                                                        .controllerUrl(UUID.randomUUID().toString())
                                                        .username(UUID.randomUUID().toString())
                                                        .password(password.toCharArray())
                                                        .accountname(UUID.randomUUID().toString())
                                                        .build();

        SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                                .withAccountId(accountId)
                                                .withValue(appDynamicsConfig)
                                                .withAppId(UUID.randomUUID().toString())
                                                .withCategory(Category.CONNECTOR)
                                                .withEnvId(UUID.randomUUID().toString())
                                                .withName(UUID.randomUUID().toString())
                                                .build();

        wingsPersistence.save(settingAttribute);
        appDynamicsConfig.setPassword(password.toCharArray());
        encryptedEntities.put(settingAttribute.getUuid(), settingAttribute);
      }

      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes, query.asList().size());

      KmsConfig toKmsConfig = getKmsConfig();
      toKmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/e1aebd89-277b-4ec7-a4e9-9a238f8b2594");
      kmsService.saveKmsConfig(accountId, toKmsConfig);
      assertEquals(2, wingsPersistence.createQuery(KmsConfig.class).asList().size());

      try {
        kmsService.deleteKmsConfig(accountId, fromConfig.getUuid());
        fail("Was able to delete kms which has reference in encrypted secrets");
      } catch (WingsException e) {
        // expected
      }

      secretManager.transitionSecrets(
          accountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.KMS, toKmsConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      kmsService.deleteKmsConfig(accountId, fromConfig.getUuid());
      assertEquals(1, wingsPersistence.createQuery(KmsConfig.class).asList().size());

      query = wingsPersistence.createQuery(EncryptedData.class);
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes, query.asList().size());
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  public void saveAwsConfig() throws IOException, InterruptedException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig fromConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, fromConfig);
    enableKmsFeatureFlag();

    int numOfSettingAttributes = 5;
    Map<String, SettingAttribute> encryptedEntities = new HashMap<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      final AwsConfig awsConfig = AwsConfig.builder()
                                      .accountId(accountId)
                                      .accessKey(UUID.randomUUID().toString())
                                      .secretKey(UUID.randomUUID().toString().toCharArray())
                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(awsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CLOUD_PROVIDER)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      encryptedEntities.put(settingAttribute.getUuid(), settingAttribute);
    }

    Collection<UuidAware> uuidAwares = secretManager.listEncryptedValues(accountId);
    assertEquals(encryptedEntities.size(), uuidAwares.size());
    for (UuidAware encryptedValue : uuidAwares) {
      assertEquals(EncryptionType.KMS, ((SettingAttribute) encryptedValue).getEncryptionType());
    }
  }

  @Test
  @RealMongo
  public void saveUpdateConfigFileNoKms() throws IOException, InterruptedException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    System.out.println("seed: " + seed);
    Random r = new Random(seed);
    final String accountId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();

    Service service = Service.Builder.aService().withName(UUID.randomUUID().toString()).withAppId(appId).build();
    wingsPersistence.save(service);

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
                                .accountId(accountId)
                                .encrypted(false)
                                .build();

    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(appId);

    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());

    String configFileId = configService.save(configFile, new BoundedInputStream(new FileInputStream(fileToSave)));
    File download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).asList().size());
    ConfigFile savedConfigFile = configService.get(appId, configFileId);
    assertFalse(savedConfigFile.isEncrypted());
    assertTrue(StringUtils.isEmpty(savedConfigFile.getEncryptedFileId()));
    assertTrue(wingsPersistence.createQuery(EncryptedData.class).asList().isEmpty());

    // now make the same file encrypted
    String secretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, new BoundedInputStream(new FileInputStream(fileToUpdate)));
    configFile.setEncrypted(true);
    configFile.setEncryptedFileId(secretFileId);
    configService.update(configFile, null);
    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    savedConfigFile = configService.get(appId, configFileId);
    assertTrue(savedConfigFile.isEncrypted());
    assertFalse(StringUtils.isEmpty(savedConfigFile.getEncryptedFileId()));

    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
    EncryptedData encryptedData = wingsPersistence.createQuery(EncryptedData.class).asList().get(0);
    assertEquals(accountId, encryptedData.getAccountId());
    assertEquals(1, encryptedData.getParentIds().size());
    assertTrue(encryptedData.getParentIds().contains(configFileId));
    assertEquals(accountId, encryptedData.getEncryptionKey());
    assertEquals(SettingVariableTypes.CONFIG_FILE, encryptedData.getType());
    assertTrue(encryptedData.isEnabled());
    assertTrue(StringUtils.isBlank(encryptedData.getKmsId()));
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());

    // now make the same file not encrypted
    fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    configFile.setEncrypted(false);
    configService.update(configFile, new BoundedInputStream(new FileInputStream(fileToUpdate)));
    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    savedConfigFile = configService.get(appId, configFileId);
    assertFalse(savedConfigFile.isEncrypted());
    assertTrue(StringUtils.isEmpty(savedConfigFile.getEncryptedFileId()));

    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
    List<SecretChangeLog> changeLogs = secretManager.getChangeLogs(secretFileId, SettingVariableTypes.CONFIG_FILE);
    assertEquals(1, changeLogs.size());
    SecretChangeLog changeLog = changeLogs.get(0);
    assertEquals(accountId, changeLog.getAccountId());
    assertEquals(secretFileId, changeLog.getEncryptedDataId());
    assertEquals(userName, changeLog.getUser().getName());
    assertEquals(userEmail, changeLog.getUser().getEmail());
    assertEquals(user.getUuid(), changeLog.getUser().getUuid());
    assertEquals("File uploaded", changeLog.getDescription());
  }

  @Test
  @RealMongo
  public void saveConfigFileNoEncryption() throws IOException, InterruptedException {
    final long seed = System.currentTimeMillis();
    System.out.println("seed: " + seed);
    Random r = new Random(seed);
    final String accountId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();
    KmsConfig fromConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, fromConfig);
    enableKmsFeatureFlag();

    Service service = Service.Builder.aService().withName(UUID.randomUUID().toString()).withAppId(appId).build();
    wingsPersistence.save(service);

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
                                .accountId(accountId)
                                .encrypted(false)
                                .build();

    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(appId);

    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());

    configService.save(configFile, new BoundedInputStream(new FileInputStream(fileToSave)));
    File download = configService.download(appId, configFile.getUuid());
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  @RealMongo
  public void saveConfigFileWithEncryption() throws IOException, InterruptedException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    System.out.println("seed: " + seed);
    Random r = new Random(seed);
    final String accountId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();
    KmsConfig fromConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, fromConfig);
    enableKmsFeatureFlag();

    Service service = Service.Builder.aService().withName(UUID.randomUUID().toString()).withAppId(appId).build();
    wingsPersistence.save(service);

    Activity activity = Activity.builder().workflowExecutionId(workflowExecutionId).environmentId(envId).build();
    activity.setAppId(appId);
    wingsPersistence.save(activity);

    String secretName = UUID.randomUUID().toString();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, new BoundedInputStream(new FileInputStream(fileToSave)));
    String encryptedUuid =
        wingsPersistence.createQuery(EncryptedData.class).field("type").equal(CONFIG_FILE).asList().get(0).getUuid();

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
                                .accountId(accountId)
                                .encryptedFileId(secretFileId)
                                .encrypted(true)
                                .build();

    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(appId);

    String configFileId = configService.save(configFile, null);
    File download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                                                .field("type")
                                                .equal(SettingVariableTypes.CONFIG_FILE)
                                                .asList();
    assertEquals(1, encryptedFileData.size());
    assertEquals(1, encryptedFileData.get(0).getParentIds().size());
    assertTrue(encryptedFileData.get(0).getParentIds().contains(configFileId));

    // test update
    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManager.updateFile(
        accountId, newSecretName, encryptedUuid, new BoundedInputStream(new FileInputStream(fileToUpdate)));

    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                            .field("type")
                            .equal(SettingVariableTypes.CONFIG_FILE)
                            .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(encryptedFileData.get(0).getParentIds().isEmpty());

    int numOfAccess = 7;
    for (int i = 0; i < numOfAccess; i++) {
      configService.downloadForActivity(appId, configFileId, activity.getUuid());
    }
    List<SecretUsageLog> usageLogs = secretManager.getUsageLogs(encryptedUuid, SettingVariableTypes.CONFIG_FILE);
    assertEquals(numOfAccess, usageLogs.size());

    for (SecretUsageLog usageLog : usageLogs) {
      assertEquals(workflowName, usageLog.getWorkflowExecutionName());
      assertEquals(accountId, usageLog.getAccountId());
      assertEquals(envId, usageLog.getEnvId());
      assertEquals(appId, usageLog.getAppId());
    }

    List<SecretChangeLog> changeLogs = secretManager.getChangeLogs(secretFileId, SettingVariableTypes.CONFIG_FILE);
    assertEquals(2, changeLogs.size());
    SecretChangeLog changeLog = changeLogs.get(0);
    assertEquals(accountId, changeLog.getAccountId());
    assertEquals(secretFileId, changeLog.getEncryptedDataId());
    assertEquals(userName, changeLog.getUser().getName());
    assertEquals(userEmail, changeLog.getUser().getEmail());
    assertEquals(user.getUuid(), changeLog.getUser().getUuid());
    assertEquals("Changed Name and File", changeLog.getDescription());

    changeLog = changeLogs.get(1);
    assertEquals(accountId, changeLog.getAccountId());
    assertEquals(secretFileId, changeLog.getEncryptedDataId());
    assertEquals(userName, changeLog.getUser().getName());
    assertEquals(userEmail, changeLog.getUser().getEmail());
    assertEquals(user.getUuid(), changeLog.getUser().getUuid());
    assertEquals("File uploaded", changeLog.getDescription());
  }

  @Test
  @RealMongo
  public void saveConfigFileTemplateWithEncryption() throws IOException, InterruptedException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    System.out.println("seed: " + seed);
    Random r = new Random(seed);
    final String accountId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();
    KmsConfig fromConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, fromConfig);
    enableKmsFeatureFlag();

    Service service = Service.Builder.aService().withName(UUID.randomUUID().toString()).build();
    service.setAppId(appId);
    String serviceId = wingsPersistence.save(service);
    ServiceTemplate serviceTemplate = ServiceTemplate.Builder.aServiceTemplate().withServiceId(serviceId).build();
    serviceTemplate.setAppId(appId);
    String serviceTemplateId = wingsPersistence.save(serviceTemplate);

    Activity activity = Activity.builder().workflowId(workflowExecutionId).build();
    activity.setAppId(appId);
    wingsPersistence.save(activity);

    String secretName = UUID.randomUUID().toString();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, new BoundedInputStream(new FileInputStream(fileToSave)));
    String encryptedUuid =
        wingsPersistence.createQuery(EncryptedData.class).field("type").equal(CONFIG_FILE).asList().get(0).getUuid();

    ConfigFile configFile = ConfigFile.builder()
                                .templateId(UUID.randomUUID().toString())
                                .envId(UUID.randomUUID().toString())
                                .entityType(EntityType.SERVICE_TEMPLATE)
                                .entityId(serviceTemplateId)
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
                                .accountId(accountId)
                                .encryptedFileId(secretFileId)
                                .templateId(serviceTemplateId)
                                .encrypted(true)
                                .build();

    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(appId);

    String configFileId = configService.save(configFile, null);
    File download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                                                .field("type")
                                                .equal(SettingVariableTypes.CONFIG_FILE)
                                                .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(encryptedFileData.get(0).getParentIds().isEmpty());
    // test update
    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManager.updateFile(
        accountId, newSecretName, encryptedUuid, new BoundedInputStream(new FileInputStream(fileToUpdate)));

    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                            .field("type")
                            .equal(SettingVariableTypes.CONFIG_FILE)
                            .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(encryptedFileData.get(0).getParentIds().isEmpty());
  }

  @Test
  public void retrialsTest() throws IOException {
    SecretManagementDelegateService delegateService = new SecretManagementDelegateServiceImpl();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setKmsArn("invalid krn");
    String toEncrypt = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    try {
      delegateService.encrypt(accountId, toEncrypt.toCharArray(), kmsConfig);
      fail("should have been failed");
    } catch (IOException e) {
      assertEquals(
          "Encryption failed after " + SecretManagementDelegateServiceImpl.NUM_OF_RETRIES + " retries", e.getMessage());
    }

    kmsConfig = getKmsConfig();
    try {
      delegateService.decrypt(EncryptedData.builder()
                                  .encryptionKey(UUID.randomUUID().toString())
                                  .encryptedValue(toEncrypt.toCharArray())
                                  .build(),
          kmsConfig);
      fail("should have been failed");
    } catch (IOException e) {
      assertEquals(
          "Decryption failed after " + SecretManagementDelegateServiceImpl.NUM_OF_RETRIES + " retries", e.getMessage());
    }
  }

  @Test
  public void reuseYamlPasswordNoEncryption() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();

    int numOfSettingAttributes = 5;
    String password = "password";
    Set<String> attributeIds = new HashSet<>();
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(UUID.randomUUID().toString())
                                              .username(UUID.randomUUID().toString())
                                              .password(password.toCharArray())
                                              .accountname(UUID.randomUUID().toString())
                                              .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();
    attributeIds.add(wingsPersistence.save(settingAttribute));

    String yamlRef = secretManager.getEncryptedYamlRef(appDynamicsConfig);

    for (int i = 1; i < numOfSettingAttributes; i++) {
      appDynamicsConfig = AppDynamicsConfig.builder()
                              .accountId(accountId)
                              .controllerUrl(UUID.randomUUID().toString())
                              .username(UUID.randomUUID().toString())
                              .password(null)
                              .encryptedPassword(yamlRef)
                              .accountname(UUID.randomUUID().toString())
                              .build();

      settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                             .withAccountId(accountId)
                             .withValue(appDynamicsConfig)
                             .withAppId(UUID.randomUUID().toString())
                             .withCategory(Category.CONNECTOR)
                             .withEnvId(UUID.randomUUID().toString())
                             .withName(UUID.randomUUID().toString())
                             .build();

      attributeIds.add(wingsPersistence.save(settingAttribute));
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    List<EncryptedData> encryptedDatas =
        wingsPersistence.createQuery(EncryptedData.class).field("encryptionType").notEqual(EncryptionType.KMS).asList();
    assertEquals(1, encryptedDatas.size());
    EncryptedData encryptedData = encryptedDatas.get(0);
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertNull(encryptedData.getKmsId());
    assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
    assertEquals(numOfSettingAttributes, encryptedData.getParentIds().size());
    assertEquals(attributeIds, encryptedData.getParentIds());

    for (String attributeId : attributeIds) {
      SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, attributeId);
      AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
      assertEquals(accountId, savedConfig.getAccountId());
      assertNull(savedConfig.getPassword());
      assertFalse(StringUtils.isBlank(savedConfig.getEncryptedPassword()));

      encryptionService.decrypt(
          savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
      assertEquals(password, String.valueOf(savedConfig.getPassword()));
    }

    // delete configs and check
    int i = 0;
    Set<String> remainingAttrs = new HashSet<>(attributeIds);
    for (String attributeId : attributeIds) {
      wingsPersistence.delete(SettingAttribute.class, attributeId);
      remainingAttrs.remove(attributeId);
      encryptedDatas = wingsPersistence.createQuery(EncryptedData.class).asList();
      if (i == numOfSettingAttributes - 1) {
        assertTrue(encryptedDatas.isEmpty());
      } else {
        assertEquals(1, encryptedDatas.size());
        encryptedData = encryptedDatas.get(0);
        assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());
        assertEquals(accountId, encryptedData.getAccountId());
        assertTrue(encryptedData.isEnabled());
        assertNull(encryptedData.getKmsId());
        assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
        assertEquals(numOfSettingAttributes - (i + 1), encryptedData.getParentIds().size());

        assertFalse(encryptedData.getParentIds().contains(attributeId));
        assertEquals(remainingAttrs, encryptedData.getParentIds());
      }
      i++;
    }
  }

  @Test
  public void reuseYamlPasswordKmsEncryption() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig fromConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, fromConfig);
    enableKmsFeatureFlag();

    int numOfSettingAttributes = 5;
    String password = "password";
    Set<String> attributeIds = new HashSet<>();
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(UUID.randomUUID().toString())
                                              .username(UUID.randomUUID().toString())
                                              .password(password.toCharArray())
                                              .accountname(UUID.randomUUID().toString())
                                              .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();
    attributeIds.add(wingsPersistence.save(settingAttribute));

    String yamlRef = secretManager.getEncryptedYamlRef(appDynamicsConfig);
    for (int i = 1; i < numOfSettingAttributes; i++) {
      appDynamicsConfig = AppDynamicsConfig.builder()
                              .accountId(accountId)
                              .controllerUrl(UUID.randomUUID().toString())
                              .username(UUID.randomUUID().toString())
                              .password(null)
                              .encryptedPassword(yamlRef)
                              .accountname(UUID.randomUUID().toString())
                              .build();

      settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                             .withAccountId(accountId)
                             .withValue(appDynamicsConfig)
                             .withAppId(UUID.randomUUID().toString())
                             .withCategory(Category.CONNECTOR)
                             .withEnvId(UUID.randomUUID().toString())
                             .withName(UUID.randomUUID().toString())
                             .build();

      attributeIds.add(wingsPersistence.save(settingAttribute));
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    List<EncryptedData> encryptedDatas =
        wingsPersistence.createQuery(EncryptedData.class).field("encryptionType").equal(EncryptionType.KMS).asList();
    assertEquals(1, encryptedDatas.size());
    EncryptedData encryptedData = encryptedDatas.get(0);
    assertEquals(EncryptionType.KMS, encryptedData.getEncryptionType());
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
      assertFalse(StringUtils.isBlank(savedConfig.getEncryptedPassword()));

      encryptionService.decrypt(
          savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
      assertEquals(password, String.valueOf(savedConfig.getPassword()));
    }

    // delete configs and check
    int i = 0;
    Set<String> remainingAttrs = new HashSet<>(attributeIds);
    for (String attributeId : attributeIds) {
      wingsPersistence.delete(SettingAttribute.class, attributeId);
      remainingAttrs.remove(attributeId);
      encryptedDatas =
          wingsPersistence.createQuery(EncryptedData.class).field("encryptionType").equal(EncryptionType.KMS).asList();
      if (i == numOfSettingAttributes - 1) {
        assertTrue(encryptedDatas.isEmpty());
      } else {
        assertEquals(1, encryptedDatas.size());
        encryptedData = encryptedDatas.get(0);
        assertEquals(EncryptionType.KMS, encryptedData.getEncryptionType());
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

  @Test
  public void getUsageLogs() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);

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
    List<SecretUsageLog> usageLogs =
        secretManager.getUsageLogs(savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertEquals(0, usageLogs.size());

    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId);
    usageLogs = secretManager.getUsageLogs(savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertEquals(1, usageLogs.size());
    assertEquals(workflowName, usageLogs.get(0).getWorkflowExecutionName());
    assertEquals(accountId, usageLogs.get(0).getAccountId());
    assertEquals(envId, usageLogs.get(0).getEnvId());
    assertEquals(appId, usageLogs.get(0).getAppId());

    secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId);
    secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId);
    usageLogs = secretManager.getUsageLogs(savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertEquals(3, usageLogs.size());

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute appDAttribute = SettingAttribute.Builder.aSettingAttribute()
                                         .withAccountId(appDynamicsConfig.getAccountId())
                                         .withValue(appDynamicsConfig)
                                         .withAppId(UUID.randomUUID().toString())
                                         .withCategory(Category.CONNECTOR)
                                         .withEnvId(UUID.randomUUID().toString())
                                         .withName(UUID.randomUUID().toString())
                                         .build();

    String appDAttributeId = wingsPersistence.save(appDAttribute);
    usageLogs = secretManager.getUsageLogs(appDAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(0, usageLogs.size());
    int numOfAccess = 13;
    for (int i = 0; i < numOfAccess; i++) {
      secretManager.getEncryptionDetails((Encryptable) appDAttribute.getValue(), appId, workflowExecutionId);
    }
    usageLogs = secretManager.getUsageLogs(appDAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(numOfAccess, usageLogs.size());
    for (SecretUsageLog usageLog : usageLogs) {
      assertEquals(workflowName, usageLog.getWorkflowExecutionName());
      assertEquals(accountId, usageLog.getAccountId());
      assertEquals(envId, usageLog.getEnvId());
      assertEquals(appId, usageLog.getAppId());
    }
  }

  @Test
  public void getChangeLogs() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute appDAttribute = SettingAttribute.Builder.aSettingAttribute()
                                         .withAccountId(appDynamicsConfig.getAccountId())
                                         .withValue(appDynamicsConfig)
                                         .withAppId(UUID.randomUUID().toString())
                                         .withCategory(Category.CONNECTOR)
                                         .withEnvId(UUID.randomUUID().toString())
                                         .withName(UUID.randomUUID().toString())
                                         .build();

    String appDAttributeId = wingsPersistence.save(appDAttribute);
    int numOfUpdates = 13;
    for (int i = 0; i < numOfUpdates; i++) {
      appDynamicsConfig.setPassword(UUID.randomUUID().toString().toCharArray());
      wingsPersistence.save(appDAttribute);
    }

    List<SecretChangeLog> changeLogs = secretManager.getChangeLogs(appDAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(numOfUpdates + 1, changeLogs.size());
    for (int i = 0; i <= numOfUpdates; i++) {
      SecretChangeLog secretChangeLog = changeLogs.get(i);
      assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
      assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
      assertEquals(user.getName(), secretChangeLog.getUser().getName());

      if (i == numOfUpdates) {
        assertEquals("Created", secretChangeLog.getDescription());
      } else {
        assertEquals("Changed password", secretChangeLog.getDescription());
      }
    }
  }

  private KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    kmsConfig.setAccessKey("AKIAJLEKM45P4PO5QUFQ");
    kmsConfig.setSecretKey("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");
    return kmsConfig;
  }

  private Thread startTransitionListener() {
    setInternalState(kmsService, "transitionKmsQueue", transitionKmsQueue);
    transitionEventListener = new KmsTransitionEventListener();
    setInternalState(transitionEventListener, "timer", new ScheduledThreadPoolExecutor(1));
    setInternalState(transitionEventListener, "queue", transitionKmsQueue);
    setInternalState(transitionEventListener, "secretManager", secretManager);

    Thread eventListenerThread = new Thread(() -> transitionEventListener.run());
    eventListenerThread.start();
    return eventListenerThread;
  }

  private void enableKmsFeatureFlag() {
    FeatureFlag kmsFeatureFlag =
        FeatureFlag.builder().name(FeatureName.KMS.name()).enabled(true).obsolete(false).build();
    wingsPersistence.save(kmsFeatureFlag);
  }

  private void stopTransitionListener(Thread thread) throws InterruptedException {
    transitionEventListener.shutDown();
    thread.join();
  }
}
