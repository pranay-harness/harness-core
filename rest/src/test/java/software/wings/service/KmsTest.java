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

import clojure.lang.IFn.LO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
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
import software.wings.beans.EmbeddedUser;
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
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.core.queue.Queue;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.RealMongo;
import software.wings.rules.RepeatRule.Repeat;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.security.KmsDelegateServiceImpl;
import software.wings.service.impl.security.KmsServiceImpl;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsDelegateService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
  private String workflowId;
  private String workflowName;

  @Before
  public void setup() {
    initMocks(this);
    appId = UUID.randomUUID().toString();
    workflowName = UUID.randomUUID().toString();
    workflowId = wingsPersistence.save(WorkflowBuilder.aWorkflow().withName(workflowName).build());
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(new KmsDelegateServiceImpl());
    setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(wingsPersistence, "kmsService", kmsService);
    setInternalState(configService, "kmsService", kmsService);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);
  }

  @Test
  public void getKmsConfigGlobal() throws IOException {
    String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(Base.GLOBAL_ACCOUNT_ID);

    KmsConfig savedConfig = kmsService.getKmsConfig(UUID.randomUUID().toString());
    assertNull(savedConfig);

    kmsService.saveGlobalKmsConfig(accountId, kmsConfig);

    savedConfig = kmsService.getKmsConfig(UUID.randomUUID().toString());
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

    KmsConfig savedConfig = kmsService.getKmsConfig(kmsConfig.getAccountId());
    kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    assertEquals(kmsConfig, savedConfig);
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
    encryptionService.decrypt((Encryptable) savedAttribute.getValue(),
        kmsService.getEncryptionDetails((Encryptable) savedAttribute.getValue(), workflowId, appId));
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertNull(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword());
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertTrue(query.asList().isEmpty());
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
    assertNull((savedConfig).getEncryptedPassword());
    encryptionService.decrypt(savedConfig, kmsService.getEncryptionDetails(savedConfig, workflowId, appId));
    assertEquals(appDynamicsConfig, savedConfig);

    assertEquals(password, new String(savedConfig.getPassword()));
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertTrue(query.asList().isEmpty());
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
    assertNotEquals(password, new String(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()));
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertTrue(StringUtils.isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()));
    enableKmsFeatureFlag();
    encryptionService.decrypt((Encryptable) savedAttribute.getValue(),
        kmsService.getEncryptionDetails((Encryptable) savedAttribute.getValue(), workflowId, appId));
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
        wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(settingAttribute.getUuid());
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
        wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(settingAttribute.getUuid());
    assertEquals(1, encryptedDataQuery.asList().size());
    EncryptedData encryptedData = encryptedDataQuery.asList().get(0);

    kmsService.getEncryptionDetails((Encryptable) savedAttribute.getValue(), workflowId, appId);
    Query<SecretUsageLog> query = wingsPersistence.createQuery(SecretUsageLog.class);
    assertEquals(1, query.asList().size());
    SecretUsageLog usageLog = query.asList().get(0);
    assertEquals(accountId, usageLog.getAccountId());
    assertEquals(workflowId, usageLog.getWorkflowId());
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
          appDynamicsConfig, kmsService.getEncryptionDetails(appDynamicsConfig, workflowId, appId));
      assertEquals("password" + i, new String(appDynamicsConfig.getPassword()));
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(id);
      assertEquals(1, query.asList().size());
      assertEquals(kmsConfig.getUuid(), query.asList().get(0).getKmsId());
    }

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId);
    assertEquals(1, kmsConfigs.size());
    assertEquals(numOfSettingAttributes, kmsConfigs.iterator().next().getNumOfEncryptedValue());
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
        wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(savedAttributeId);
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

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(savedAttributeId);
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
  }

  @Test
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
        wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(savedAttributeId);
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

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(savedAttributeId);
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

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(savedAttributeId);
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
                                                .value(UUID.randomUUID().toString().toCharArray())
                                                .type(Type.TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    // update to encrypt the variable
    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", "newValue".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.ENCRYPTED_TEXT, savedAttribute.getType());
    assertNotEquals("newValue", new String(savedAttribute.getValue()));
    encryptionService.decrypt(savedAttribute, kmsService.getEncryptionDetails(savedAttribute, workflowId, appId));
    assertEquals("newValue", new String(savedAttribute.getValue()));
    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.TEXT);
    keyValuePairs.put("value", "unencrypted".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.TEXT, savedAttribute.getType());
    assertEquals("unencrypted", new String(savedAttribute.getValue()));
    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  public void saveServiceVariableNoEncryption() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

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
                                                .value(UUID.randomUUID().toString().toCharArray())
                                                .type(Type.TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    // update to encrypt the variable
    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", "newValue".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.ENCRYPTED_TEXT, savedAttribute.getType());
    assertNull(savedAttribute.getValue());
    encryptionService.decrypt(savedAttribute, kmsService.getEncryptionDetails(savedAttribute, workflowId, appId));
    assertEquals("newValue", new String(savedAttribute.getValue()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.TEXT);
    keyValuePairs.put("value", "unencrypted".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.TEXT, savedAttribute.getType());
    assertEquals("unencrypted", new String(savedAttribute.getValue()));
    assertEquals(numOfEncryptedValsForKms, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  public void kmsEncryptionSaveServiceVariable() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

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
                                                .value(UUID.randomUUID().toString().toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", "newName");
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", "newValue".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(savedAttributeId);
    assertEquals(1, query.asList().size());

    Collection<UuidAware> uuidAwares = kmsService.listEncryptedValues(accountId);
    assertEquals(1, uuidAwares.size());
    ServiceVariable listedVariable = (ServiceVariable) uuidAwares.iterator().next();
    assertEquals(KmsServiceImpl.SECRET_MASK, new String(listedVariable.getValue()));
    assertEquals(serviceVariable.getEntityType(), listedVariable.getEntityType());
    assertEquals(Type.ENCRYPTED_TEXT, listedVariable.getType());
    assertEquals(SettingVariableTypes.SERVICE_VARIABLE, listedVariable.getSettingType());
    assertEquals("newName", listedVariable.getName());

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertEquals(2, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(user.getUuid(), secretChangeLog.getUser().getUuid());
    assertEquals(user.getEmail(), secretChangeLog.getUser().getEmail());
    assertEquals(user.getName(), secretChangeLog.getUser().getName());
    assertEquals("Changed value", secretChangeLog.getDescription());

    secretChangeLog = changeLogs.get(1);
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
                                                .value(UUID.randomUUID().toString().toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Collection<UuidAware> uuidAwares = kmsService.listEncryptedValues(accountId);
    assertEquals(1, uuidAwares.size());
    ServiceVariable listedVariable = (ServiceVariable) uuidAwares.iterator().next();
    assertEquals(KmsServiceImpl.SECRET_MASK, new String(listedVariable.getValue()));
    assertEquals(serviceVariable.getEntityType(), listedVariable.getEntityType());
    assertEquals(Type.ENCRYPTED_TEXT, listedVariable.getType());
    assertEquals(SettingVariableTypes.SERVICE_VARIABLE, listedVariable.getSettingType());
    assertEquals(serviceId, listedVariable.getServiceId());
  }

  @Test
  public void kmsEncryptionUpdateServiceVariable() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

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
                                                .value(UUID.randomUUID().toString().toCharArray())
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
    char[] updatedValue = UUID.randomUUID().toString().toCharArray();

    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("envId", updatedEnvId);
    keyValuePairs.put("value", updatedValue);

    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    assertEquals(updatedName, updatedAttribute.getName());
    assertNull(updatedAttribute.getValue());
    encryptionService.decrypt(updatedAttribute, kmsService.getEncryptionDetails(updatedAttribute, workflowId, appId));
    assertEquals(new String(updatedValue), new String(updatedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
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

    KmsConfig savedKmsConfig = kmsService.getKmsConfig(accountId);
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
      encryptedEntities.add(settingAttribute);
    }

    kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);

    Collection<UuidAware> encryptedValues = kmsService.listEncryptedValues(accountId);
    assertEquals(encryptedEntities.size(), encryptedValues.size());
    assertTrue(encryptedEntities.containsAll(encryptedValues));
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

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId);
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

    kmsConfigs = kmsService.listKmsConfigs(accountId);
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

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId);
    assertEquals(1, kmsConfigs.size());
    assertTrue(kmsConfigs.iterator().next().isDefault());

    int numOfKms = 10;
    for (int i = 1; i <= numOfKms; i++) {
      KmsConfig kmsConfig = getKmsConfig();
      kmsConfig.setDefault(true);
      kmsConfig.setName("kms" + i);
      kmsService.saveKmsConfig(accountId, kmsConfig);
    }

    kmsConfigs = kmsService.listKmsConfigs(accountId);
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
    kmsConfigs = kmsService.listKmsConfigs(accountId);
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

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId);
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

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId);
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

    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId);
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

    kmsConfigs = kmsService.listKmsConfigs(accountId);
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

    kmsConfigs = kmsService.listKmsConfigs(accountId);
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

    kmsService.transitionKms(accountId, fromConfig.getUuid(), toKmsConfig.getUuid());
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

    stopTransitionListener(listenerThread);
  }

  @Test
  public void transitionAndDeleteKms() throws IOException, InterruptedException {
    Thread listenerThread = startTransitionListener();
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

    kmsService.transitionKms(accountId, fromConfig.getUuid(), toKmsConfig.getUuid());
    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
    kmsService.deleteKmsConfig(accountId, fromConfig.getUuid());
    assertEquals(1, wingsPersistence.createQuery(KmsConfig.class).asList().size());

    query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes, query.asList().size());
    stopTransitionListener(listenerThread);
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

    Collection<UuidAware> uuidAwares = kmsService.listEncryptedValues(accountId);
    assertEquals(encryptedEntities.size(), uuidAwares.size());
  }

  @Test
  @RealMongo
  public void saveUpdateConfigFileNoKms() throws IOException, InterruptedException {
    final long seed = System.currentTimeMillis();
    System.out.println("seed: " + seed);
    Random r = new Random(seed);
    final String accountId = UUID.randomUUID().toString();
    final String appId = UUID.randomUUID().toString();

    Service service = Service.Builder.aService().withName(UUID.randomUUID().toString()).withAppId(appId).build();
    wingsPersistence.save(service);

    ConfigFile.Builder configFileBuilder = ConfigFile.Builder.aConfigFile()
                                               .withTemplateId(UUID.randomUUID().toString())
                                               .withEnvId(UUID.randomUUID().toString())
                                               .withEntityType(EntityType.SERVICE)
                                               .withEntityId(service.getUuid())
                                               .withDescription(UUID.randomUUID().toString())
                                               .withParentConfigFileId(UUID.randomUUID().toString())
                                               .withRelativeFilePath(UUID.randomUUID().toString())
                                               .withTargetToAllEnv(r.nextBoolean())
                                               .withDefaultVersion(r.nextInt())
                                               .withEnvIdVersionMapString(UUID.randomUUID().toString())
                                               .withSetAsDefault(r.nextBoolean())
                                               .withNotes(UUID.randomUUID().toString())
                                               .withOverridePath(UUID.randomUUID().toString())
                                               .withConfigOverrideType(ConfigOverrideType.CUSTOM)
                                               .withConfigOverrideExpression(UUID.randomUUID().toString())
                                               .withAppId(appId)
                                               .withAccountId(accountId)
                                               .withFileName(UUID.randomUUID().toString())
                                               .withName(UUID.randomUUID().toString())
                                               .withEncrypted(false);

    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());

    String configFileId =
        configService.save(configFileBuilder.but().build(), new BoundedInputStream(new FileInputStream(fileToSave)));
    File download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave), FileUtils.readFileToString(download));
    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).asList().size());
    ConfigFile savedConfigFile = configService.get(appId, configFileId);
    assertFalse(savedConfigFile.isEncrypted());
    assertTrue(StringUtils.isEmpty(savedConfigFile.getEncryptedFileId()));

    // now make the same file encrypted
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    configService.update(configFileBuilder.withUuid(configFileId).withEncrypted(true).but().build(),
        new BoundedInputStream(new FileInputStream(fileToUpdate)));
    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate), FileUtils.readFileToString(download));
    savedConfigFile = configService.get(appId, configFileId);
    assertTrue(savedConfigFile.isEncrypted());
    assertTrue(StringUtils.isEmpty(savedConfigFile.getEncryptedFileId()));

    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    // now make the same file not encrypted
    fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    configService.update(configFileBuilder.withUuid(configFileId).withEncrypted(false).but().build(),
        new BoundedInputStream(new FileInputStream(fileToUpdate)));
    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate), FileUtils.readFileToString(download));
    savedConfigFile = configService.get(appId, configFileId);
    assertFalse(savedConfigFile.isEncrypted());
    assertTrue(StringUtils.isEmpty(savedConfigFile.getEncryptedFileId()));

    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).asList().size());
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

    ConfigFile configFile = ConfigFile.Builder.aConfigFile()
                                .withTemplateId(UUID.randomUUID().toString())
                                .withEnvId(UUID.randomUUID().toString())
                                .withEntityType(EntityType.SERVICE)
                                .withEntityId(service.getUuid())
                                .withDescription(UUID.randomUUID().toString())
                                .withParentConfigFileId(UUID.randomUUID().toString())
                                .withRelativeFilePath(UUID.randomUUID().toString())
                                .withTargetToAllEnv(r.nextBoolean())
                                .withDefaultVersion(r.nextInt())
                                .withEnvIdVersionMapString(UUID.randomUUID().toString())
                                .withSetAsDefault(r.nextBoolean())
                                .withNotes(UUID.randomUUID().toString())
                                .withOverridePath(UUID.randomUUID().toString())
                                .withConfigOverrideType(ConfigOverrideType.CUSTOM)
                                .withConfigOverrideExpression(UUID.randomUUID().toString())
                                .withAppId(appId)
                                .withAccountId(accountId)
                                .withFileName(UUID.randomUUID().toString())
                                .withName(UUID.randomUUID().toString())
                                .withEncrypted(false)
                                .build();

    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());

    configService.save(configFile, new BoundedInputStream(new FileInputStream(fileToSave)));
    File download = configService.download(appId, configFile.getUuid());
    assertEquals(FileUtils.readFileToString(fileToSave), FileUtils.readFileToString(download));
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

    Activity activity = Activity.builder().workflowId(workflowId).build();
    activity.setAppId(appId);
    wingsPersistence.save(activity);

    ConfigFile.Builder configFileBuilder = ConfigFile.Builder.aConfigFile()
                                               .withTemplateId(UUID.randomUUID().toString())
                                               .withEnvId(UUID.randomUUID().toString())
                                               .withEntityType(EntityType.SERVICE)
                                               .withEntityId(service.getUuid())
                                               .withDescription(UUID.randomUUID().toString())
                                               .withParentConfigFileId(UUID.randomUUID().toString())
                                               .withRelativeFilePath(UUID.randomUUID().toString())
                                               .withTargetToAllEnv(r.nextBoolean())
                                               .withDefaultVersion(r.nextInt())
                                               .withEnvIdVersionMapString(UUID.randomUUID().toString())
                                               .withSetAsDefault(r.nextBoolean())
                                               .withNotes(UUID.randomUUID().toString())
                                               .withOverridePath(UUID.randomUUID().toString())
                                               .withConfigOverrideType(ConfigOverrideType.CUSTOM)
                                               .withConfigOverrideExpression(UUID.randomUUID().toString())
                                               .withAppId(appId)
                                               .withAccountId(accountId)
                                               .withFileName(UUID.randomUUID().toString())
                                               .withName(UUID.randomUUID().toString())
                                               .withEncrypted(true);

    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());

    String configFileId =
        configService.save(configFileBuilder.but().build(), new BoundedInputStream(new FileInputStream(fileToSave)));
    File download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave), FileUtils.readFileToString(download));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                                                .field("type")
                                                .equal(SettingVariableTypes.CONFIG_FILE)
                                                .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(StringUtils.isBlank(encryptedFileData.get(0).getParentId()));
    // test update
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    configService.update(configFileBuilder.withUuid(configFileId).but().build(),
        new BoundedInputStream(new FileInputStream(fileToUpdate)));
    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate), FileUtils.readFileToString(download));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                            .field("type")
                            .equal(SettingVariableTypes.CONFIG_FILE)
                            .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(StringUtils.isBlank(encryptedFileData.get(0).getParentId()));

    int numOfAccess = 7;
    for (int i = 0; i < numOfAccess; i++) {
      configService.downloadForActivity(appId, configFileId, activity.getUuid());
    }
    List<SecretUsageLog> usageLogs = secretManager.getUsageLogs(configFileId, SettingVariableTypes.CONFIG_FILE);
    assertEquals(numOfAccess, usageLogs.size());

    for (SecretUsageLog usageLog : usageLogs) {
      assertEquals(workflowName, usageLog.getWorkflowName());
      assertEquals(accountId, usageLog.getAccountId());
    }
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

    Activity activity = Activity.builder().workflowId(workflowId).build();
    activity.setAppId(appId);
    wingsPersistence.save(activity);

    ConfigFile.Builder configFileBuilder = ConfigFile.Builder.aConfigFile()
                                               .withTemplateId(UUID.randomUUID().toString())
                                               .withEnvId(UUID.randomUUID().toString())
                                               .withEntityType(EntityType.SERVICE_TEMPLATE)
                                               .withEntityId(serviceTemplateId)
                                               .withTemplateId(serviceTemplateId)
                                               .withDescription(UUID.randomUUID().toString())
                                               .withParentConfigFileId(UUID.randomUUID().toString())
                                               .withRelativeFilePath(UUID.randomUUID().toString())
                                               .withTargetToAllEnv(r.nextBoolean())
                                               .withDefaultVersion(r.nextInt())
                                               .withEnvIdVersionMapString(UUID.randomUUID().toString())
                                               .withSetAsDefault(r.nextBoolean())
                                               .withNotes(UUID.randomUUID().toString())
                                               .withOverridePath(UUID.randomUUID().toString())
                                               .withConfigOverrideType(ConfigOverrideType.CUSTOM)
                                               .withConfigOverrideExpression(UUID.randomUUID().toString())
                                               .withAppId(appId)
                                               .withAccountId(accountId)
                                               .withFileName(UUID.randomUUID().toString())
                                               .withName(UUID.randomUUID().toString())
                                               .withEncrypted(true);

    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());

    String configFileId =
        configService.save(configFileBuilder.but().build(), new BoundedInputStream(new FileInputStream(fileToSave)));
    File download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave), FileUtils.readFileToString(download));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                                                .field("type")
                                                .equal(SettingVariableTypes.CONFIG_FILE)
                                                .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(StringUtils.isBlank(encryptedFileData.get(0).getParentId()));
    // test update
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    configService.update(configFileBuilder.withUuid(configFileId).but().build(),
        new BoundedInputStream(new FileInputStream(fileToUpdate)));
    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate), FileUtils.readFileToString(download));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                            .field("type")
                            .equal(SettingVariableTypes.CONFIG_FILE)
                            .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(StringUtils.isBlank(encryptedFileData.get(0).getParentId()));

    Collection<UuidAware> uuidAwares = kmsService.listEncryptedValues(accountId);
    assertEquals(1, uuidAwares.size());
    ConfigFile listedVariable = (ConfigFile) uuidAwares.iterator().next();
    assertEquals(EntityType.SERVICE_TEMPLATE, listedVariable.getEntityType());
    assertTrue(listedVariable.isEncrypted());
    assertEquals(SettingVariableTypes.CONFIG_FILE, listedVariable.getSettingType());
    assertEquals(serviceId, listedVariable.getServiceId());
  }

  @Test
  public void retrialsTest() throws IOException {
    KmsDelegateService delegateService = new KmsDelegateServiceImpl();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setKmsArn("invalid krn");
    String toEncrypt = UUID.randomUUID().toString();
    try {
      delegateService.encrypt(toEncrypt.toCharArray(), kmsConfig);
      fail("should have been failed");
    } catch (IOException e) {
      assertEquals("Encryption failed after " + KmsDelegateServiceImpl.NUM_OF_RETRIES + " retries", e.getMessage());
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
      assertEquals("Decryption failed after " + KmsDelegateServiceImpl.NUM_OF_RETRIES + " retries", e.getMessage());
    }
  }

  @Test
  public void localEncryptionYaml() throws IllegalAccessException, NoSuchFieldException {
    String password = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String name = UUID.randomUUID().toString();
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
                                            .withName(name)
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    appDynamicsConfig.setPassword(password.toCharArray());
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);

    String encryptedYamlRef =
        kmsService.getEncryptedYamlRef((Encryptable) savedAttribute.getValue(), Base.GLOBAL_APP_ID, "password", name);
    char[] decryptedPassword = kmsService.decryptYamlRef(encryptedYamlRef);
    assertEquals(password, new String(decryptedPassword));

    password = UUID.randomUUID().toString();
    name = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();

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
                                                .name(name)
                                                .value(password.toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();
    serviceVariable.setAppId(appId);

    savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedServiceVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptedYamlRef = kmsService.getEncryptedYamlRef(savedServiceVariable, appId, "value", name);
    decryptedPassword = kmsService.decryptYamlRef(encryptedYamlRef);
    assertEquals(password, new String(decryptedPassword));
  }

  @Test
  public void kmsEncryptionYaml() throws IllegalAccessException, NoSuchFieldException {
    String password = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String name = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();
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
                                            .withName(name)
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    appDynamicsConfig.setPassword(password.toCharArray());
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);

    String encryptedYamlRef =
        kmsService.getEncryptedYamlRef((Encryptable) savedAttribute.getValue(), Base.GLOBAL_APP_ID, "password", name);
    char[] decryptedPassword = kmsService.decryptYamlRef(encryptedYamlRef);
    assertEquals(password, new String(decryptedPassword));

    password = UUID.randomUUID().toString();
    name = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();

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
                                                .name(name)
                                                .value(password.toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();
    serviceVariable.setAppId(appId);

    savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedServiceVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptedYamlRef = kmsService.getEncryptedYamlRef(savedServiceVariable, appId, "value", name);
    decryptedPassword = kmsService.decryptYamlRef(encryptedYamlRef);
    assertEquals(password, new String(decryptedPassword));
  }

  @Test
  public void getUsageLogs() throws IOException, IllegalAccessException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);
    enableKmsFeatureFlag();

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
                                                .value(UUID.randomUUID().toString().toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    List<SecretUsageLog> usageLogs =
        secretManager.getUsageLogs(savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertEquals(0, usageLogs.size());

    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    kmsService.getEncryptionDetails(savedAttribute, workflowId, appId);
    usageLogs = secretManager.getUsageLogs(savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE);
    assertEquals(1, usageLogs.size());
    assertEquals(workflowName, usageLogs.get(0).getWorkflowName());
    assertEquals(accountId, usageLogs.get(0).getAccountId());

    kmsService.getEncryptionDetails(savedAttribute, workflowId, appId);
    kmsService.getEncryptionDetails(savedAttribute, workflowId, appId);
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
      kmsService.getEncryptionDetails((Encryptable) appDAttribute.getValue(), workflowId, appId);
    }
    usageLogs = secretManager.getUsageLogs(appDAttributeId, SettingVariableTypes.APP_DYNAMICS);
    assertEquals(numOfAccess, usageLogs.size());
    for (SecretUsageLog usageLog : usageLogs) {
      assertEquals(workflowName, usageLog.getWorkflowName());
      assertEquals(accountId, usageLog.getAccountId());
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
    final KmsTransitionEventListener transitionEventListener = new KmsTransitionEventListener();
    setInternalState(transitionEventListener, "timer", new ScheduledThreadPoolExecutor(1));
    setInternalState(transitionEventListener, "queue", transitionKmsQueue);
    setInternalState(transitionEventListener, "kmsService", kmsService);

    Thread eventListenerThread = new Thread(() -> transitionEventListener.run());
    eventListenerThread.start();
    return eventListenerThread;
  }

  private void enableKmsFeatureFlag() {
    FeatureFlag kmsFeatureFlag =
        FeatureFlag.builder().name(FeatureName.KMS.name()).enabled(true).obsolete(false).build();
    wingsPersistence.save(kmsFeatureFlag);
  }

  private void stopTransitionListener(Thread thread) {
    thread.interrupt();
  }
}
