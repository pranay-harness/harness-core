package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.settings.SettingValue.SettingVariableTypes.CONFIG_FILE;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.KmsOperationException;
import io.harness.exception.WingsException;
import io.harness.persistence.UuidAware;
import io.harness.queue.Queue;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.rest.RestResponse;
import io.harness.rule.RealMongo;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoUtils;
import io.harness.stream.BoundedInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.Activity;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigOverrideType;
import software.wings.beans.EntityType;
import software.wings.beans.Environment.EnvironmentType;
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
import software.wings.beans.WorkflowExecution;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.features.api.PremiumFeature;
import software.wings.resources.KmsResource;
import software.wings.resources.SecretManagementResource;
import software.wings.resources.ServiceVariableResource;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRestrictionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.impl.UsageRestrictionsServiceImplTest;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;
import software.wings.settings.UsageRestrictions.AppEnvRestriction.AppEnvRestrictionBuilder;

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
 * Created by rsingh on 9/29/17.
 */
@Slf4j
public class KmsTest extends WingsBaseTest {
  @Inject private KmsResource kmsResource;
  @Inject private SecretManagementResource secretManagementResource;
  @Mock private AccountService accountService;
  @Inject @InjectMocks private KmsService kmsService;
  @Inject @InjectMocks private SecretManagerConfigService secretManagerConfigService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HarnessUserGroupService harnessUserGroupService;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;
  @Inject private ConfigService configService;
  @Inject private EncryptionService encryptionService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SettingsService settingsService;
  @Inject private SettingValidationService settingValidationService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceVariableResource serviceVariableResource;
  @Inject private SecretManagementDelegateService delegateService;
  @Mock private SecretManagementDelegateService secretManagementDelegateService;
  @Mock private ContainerService containerService;
  @Mock private NewRelicService newRelicService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private PremiumFeature secretsManagementFeature;
  private final int numOfEncryptedValsForKms = 3;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();
  private String userId;
  private String accountId;
  private String appId;
  private String workflowExecutionId;
  private String workflowName;
  private String envId;
  private KmsTransitionEventListener transitionEventListener;

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    initMocks(this);

    Account account = getAccount(AccountType.PAID);
    accountId = account.getUuid();
    when(accountService.get(accountId)).thenReturn(account);

    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(true);

    appId =
        wingsPersistence.save(Application.Builder.anApplication().name(generateUuid()).accountId(accountId).build());
    workflowName = generateUuid();
    envId = wingsPersistence.save(
        anEnvironment().environmentType(EnvironmentType.PROD).appId(appId).accountId(accountId).build());
    workflowExecutionId =
        wingsPersistence.save(WorkflowExecution.builder().name(workflowName).appId(appId).envId(envId).build());
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
    when(delegateProxyFactory.get(eq(EncryptionService.class), any(SyncTaskContext.class)))
        .thenReturn(encryptionService);
    when(delegateProxyFactory.get(eq(ContainerService.class), any(SyncTaskContext.class))).thenReturn(containerService);
    when(containerService.validate(any(ContainerServiceParams.class))).thenReturn(true);
    doNothing().when(newRelicService).validateConfig(anyObject(), anyObject(), anyObject());
    FieldUtils.writeField(kmsService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(managerDecryptionService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(secretManager, "kmsService", kmsService, true);
    FieldUtils.writeField(wingsPersistence, "secretManager", secretManager, true);
    FieldUtils.writeField(configService, "secretManager", secretManager, true);
    FieldUtils.writeField(settingValidationService, "newRelicService", newRelicService, true);
    FieldUtils.writeField(settingsService, "settingValidationService", settingValidationService, true);
    FieldUtils.writeField(encryptionService, "secretManagementDelegateService", secretManagementDelegateService, true);
    FieldUtils.writeField(infrastructureMappingService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(kmsResource, "kmsService", kmsService, true);
    FieldUtils.writeField(secretManagementResource, "secretManager", secretManager, true);
    userId = wingsPersistence.save(user);
    UserThreadLocal.set(user);

    // Add current user to harness user group so that save-global-kms operation can succeed
    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(userId))
                                            .actions(Sets.newHashSet(Action.READ))
                                            .build();
    harnessUserGroupService.save(harnessUserGroup);
  }

  @Test
  @Category(UnitTests.class)
  public void getKmsConfigGlobal() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);

    KmsConfig savedConfig =
        (KmsConfig) secretManagerConfigService.getDefaultSecretManager(UUID.randomUUID().toString());
    assertThat(savedConfig).isNull();

    kmsResource.saveGlobalKmsConfig(accountId, KryoUtils.clone(kmsConfig));

    savedConfig = (KmsConfig) secretManagerConfigService.getDefaultSecretManager(UUID.randomUUID().toString());
    kmsConfig.setUuid(savedConfig.getUuid());
    assertEquals(kmsConfig, savedConfig);
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void getGetGlobalKmsConfig() {
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setName("Global config");
    globalKmsConfig.setDefault(true);
    kmsResource.saveGlobalKmsConfig(accountId, KryoUtils.clone(globalKmsConfig));

    KmsConfig savedGlobalKmsConfig = kmsService.getGlobalKmsConfig();
    assertNotNull(savedGlobalKmsConfig);

    // Verified that retrieved global KMS config secret fields are decrypted properly.
    assertEquals(globalKmsConfig.getName(), savedGlobalKmsConfig.getName());
    assertEquals(globalKmsConfig.getAccessKey(), savedGlobalKmsConfig.getAccessKey());
    assertEquals(globalKmsConfig.getSecretKey(), savedGlobalKmsConfig.getSecretKey());
    assertEquals(globalKmsConfig.getKmsArn(), savedGlobalKmsConfig.getKmsArn());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void updateFileWithGlobalKms() throws IOException {
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setName("Global config");
    globalKmsConfig.setDefault(true);
    kmsResource.saveGlobalKmsConfig(accountId, globalKmsConfig);

    String randomAccountId = UUID.randomUUID().toString();

    String secretName = UUID.randomUUID().toString();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId = secretManager.saveFile(
        randomAccountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToSave)));
    assertNotNull(secretFileId);

    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    boolean result = secretManager.updateFile(
        randomAccountId, newSecretName, secretFileId, null, new BoundedInputStream(new FileInputStream(fileToUpdate)));
    assertTrue(result);

    assertTrue(secretManager.deleteFile(randomAccountId, secretFileId));
  }

  @Test
  @Category(UnitTests.class)
  public void validateConfig() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setAccessKey("invalidKey");

    try {
      kmsResource.saveKmsConfig(kmsConfig.getAccountId(), kmsConfig);
      fail("Saved invalid kms config");
    } catch (KmsOperationException e) {
      assertTrue(true);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void getKmsConfigForAccount() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);

    kmsResource.saveKmsConfig(kmsConfig.getAccountId(), KryoUtils.clone(kmsConfig));

    KmsConfig savedConfig = (KmsConfig) secretManagerConfigService.getDefaultSecretManager(kmsConfig.getAccountId());
    kmsConfig.setUuid(savedConfig.getUuid());
    assertEquals(kmsConfig, savedConfig);
  }

  @Test
  @Repeat(times = 3, successes = 1)
  @Category(UnitTests.class)
  public void saveAndEditConfig() {
    String name = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setName(name);
    kmsConfig.setAccountId(accountId);

    kmsResource.saveKmsConfig(kmsConfig.getAccountId(), KryoUtils.clone(kmsConfig));

    KmsConfig savedConfig = (KmsConfig) secretManagerConfigService.getDefaultSecretManager(kmsConfig.getAccountId());
    kmsConfig.setUuid(savedConfig.getUuid());
    assertEquals(kmsConfig, savedConfig);
    assertEquals(name, savedConfig.getName());
    List<EncryptedData> encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).asList();
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
    kmsResource.saveKmsConfig(accountId, savedConfig);
    encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).asList();
    assertEquals(numOfEncryptedValsForKms, encryptedDataList.size());
    for (EncryptedData encryptedData : encryptedDataList) {
      assertTrue(encryptedData.getName().equals(name + "_accessKey")
          || encryptedData.getName().equals(name + "_secretKey") || encryptedData.getName().equals(name + "_arn"));
      assertEquals(1, encryptedData.getParentIds().size());
      assertEquals(savedConfig.getUuid(), encryptedData.getParentIds().iterator().next());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void localNullEncryption() {
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, null, null);
    assertThat(encryptedData.getEncryptedValue()).isNull();
    assertFalse(isBlank(encryptedData.getEncryptionKey()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, null);
    assertThat(decryptedValue).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void localEncryption() {
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt.toCharArray(), null, null);
    assertNotEquals(keyToEncrypt, new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, null);
    assertEquals(keyToEncrypt, new String(decryptedValue));
  }

  @Test
  @Category(UnitTests.class)
  public void kmsNullEncryption() {
    final KmsConfig kmsConfig = getKmsConfig();
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, UUID.randomUUID().toString(), kmsConfig);
    assertThat(encryptedData.getEncryptedValue()).isNull();
    assertFalse(isBlank(encryptedData.getEncryptionKey()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, kmsConfig);
    assertThat(decryptedValue).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryption() {
    final KmsConfig kmsConfig = getKmsConfig();
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData =
        kmsService.encrypt(keyToEncrypt.toCharArray(), UUID.randomUUID().toString(), kmsConfig);
    assertNotEquals(keyToEncrypt, new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, kmsConfig);
    assertEquals(keyToEncrypt, new String(decryptedValue));
  }

  @Test
  @Category(UnitTests.class)
  public void localEncryptionWhileSaving() {
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    appDynamicsConfig.setPassword(password.toCharArray());
    appDynamicsConfig.setEncryptedPassword(null);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertNotNull(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword());
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()).isNull();
    encryptionService.decrypt((EncryptableSetting) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), workflowExecutionId, appId));
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()).isNull();
    assertEquals(password, new String(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()));
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(1, query.count());
  }

  @Test
  @Category(UnitTests.class)
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
                                            .withCategory(SettingCategory.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertNotNull(((ArtifactoryConfig) savedAttribute.getValue()).getEncryptedPassword());
    assertThat(((ArtifactoryConfig) savedAttribute.getValue()).getPassword()).isNull();
    encryptionService.decrypt((EncryptableSetting) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), workflowExecutionId, appId));
    artifactoryConfig.setEncryptedPassword(null);
    assertEquals(artifactoryConfig, savedAttribute.getValue());
    assertThat(((ArtifactoryConfig) savedAttribute.getValue()).getEncryptedPassword()).isNull();
    assertThat(((ArtifactoryConfig) savedAttribute.getValue()).getPassword()).isNull();
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(1, query.count());
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryptionWhileSavingFeatureDisabled() {
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    appDynamicsConfig.setPassword(password.toCharArray());
    appDynamicsConfig.setEncryptedPassword(null);
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertNotNull(savedConfig.getEncryptedPassword());
    assertThat(savedConfig.getPassword()).isNull();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertEquals(appDynamicsConfig, savedConfig);
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()).isNull();
    assertEquals(password, new String(savedConfig.getPassword()));
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertEquals(1, query.count());
  }

  @Test
  @Category(UnitTests.class)
  public void enableKmsAfterSaving() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()).isNull();
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertFalse(isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()));
    encryptionService.decrypt((EncryptableSetting) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), workflowExecutionId, appId));
    assertEquals(password, new String(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()));
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryptionWhileSaving() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(appDynamicsConfig, savedAttribute.getValue());
    assertFalse(isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()));

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(settingAttribute.getUuid());
    assertEquals(1, query.count());
    EncryptedData encryptedData = query.get();
    assertEquals(kmsConfig.getUuid(), encryptedData.getKmsId());
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
    assertEquals(numOfEncryptedValsForKms + 1, query.count());
  }

  @Test
  @Category(UnitTests.class)
  public void testNewKmsConfigIfUnavailable() {
    Account account = getAccount(AccountType.PAID);
    String accountId = account.getUuid();

    when(accountService.get(accountId)).thenReturn(account);
    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(false);

    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);

    try {
      kmsService.saveKmsConfig(accountId, kmsConfig);
      fail();
    } catch (WingsException e) {
      assertEquals(ErrorCode.INVALID_REQUEST, e.getCode());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void secretUsageLog() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);

    Query<EncryptedData> encryptedDataQuery =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(settingAttribute.getUuid());
    assertEquals(1, encryptedDataQuery.count());
    EncryptedData encryptedData = encryptedDataQuery.get();

    secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), appId, workflowExecutionId);
    Query<SecretUsageLog> query = wingsPersistence.createQuery(SecretUsageLog.class);
    assertEquals(1, query.count());
    SecretUsageLog usageLog = query.get();
    assertEquals(accountId, usageLog.getAccountId());
    assertEquals(workflowExecutionId, usageLog.getWorkflowExecutionId());
    assertEquals(appId, usageLog.getAppId());
    assertEquals(encryptedData.getUuid(), usageLog.getEncryptedDataId());
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryptionSaveMultiple() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(
        numOfEncryptedValsForKms + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());
    for (int i = 0; i < numOfSettingAttributes; i++) {
      String id = settingAttributes.get(i).getUuid();
      SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, id);
      assertEquals(settingAttributes.get(i), savedAttribute);
      AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttributes.get(i).getValue();
      assertThat(appDynamicsConfig.getPassword()).isNull();

      encryptionService.decrypt(
          appDynamicsConfig, secretManager.getEncryptionDetails(appDynamicsConfig, workflowExecutionId, appId));
      assertEquals("password" + i, new String(appDynamicsConfig.getPassword()));
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(id);
      assertEquals(1, query.count());
      assertEquals(kmsConfig.getUuid(), query.get().getKmsId());
    }

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(1, kmsConfigs.size());
    assertEquals(numOfSettingAttributes, kmsConfigs.iterator().next().getNumOfEncryptedValue());
  }

  @Test
  @Category(UnitTests.class)
  public void testNumOfEncryptedValue() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveGlobalKmsConfig(accountId, kmsConfig);

    int numOfSettingAttributes1 = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes1; i++) {
      String password = "password" + i;
      final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
      SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

      settingAttributes.add(settingAttribute);
    }
    wingsPersistence.save(settingAttributes);

    final String accountId2 = UUID.randomUUID().toString();

    int numOfSettingAttributes2 = 7;
    settingAttributes.clear();
    for (int i = 0; i < numOfSettingAttributes2; i++) {
      String password = "password" + i;
      final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId2, password);
      SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

      settingAttributes.add(settingAttribute);
    }
    wingsPersistence.save(settingAttributes);

    List<SecretManagerConfig> encryptionConfigs =
        secretManagementResource.listEncryptionConfig(accountId).getResource();
    assertEquals(1, encryptionConfigs.size());
    assertEquals(numOfSettingAttributes1, encryptionConfigs.get(0).getNumOfEncryptedValue());

    encryptionConfigs = secretManagementResource.listEncryptionConfig(accountId2).getResource();
    assertEquals(1, encryptionConfigs.size());
    assertEquals(numOfSettingAttributes2, encryptionConfigs.get(0).getNumOfEncryptedValue());
  }

  @Test
  @Category(UnitTests.class)
  public void noKmsEncryptionUpdateObject() throws IllegalAccessException {
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).count());

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    User user1 = User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName("user1").build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).count());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.count());
    EncryptedData encryptedData = query.get();
    assertNotNull(encryptedData.getKmsId());

    verifyChangeLogs(savedAttributeId, savedAttribute, user1);

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = getAppDynamicsConfig(accountId, newPassWord);

    String updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("value", newAppDynamicsConfig);

    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);
    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.count());

    encryptedData = query.get();
    assertEquals(accountId, encryptedData.getAccountId());
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());
    assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
    assertNotNull(encryptedData.getKmsId());
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

  private void verifyChangeLogs(String savedAttributeId, SettingAttribute savedAttribute, User user1)
      throws IllegalAccessException {
    Query<EncryptedData> query;
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
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void noKmsEncryptionUpdateServiceVariable() {
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

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
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).count());
    assertThat(savedVariable.getValue()).isNull();
    assertNotNull(savedVariable.getEncryptedValue());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedServiceVariableId);
    assertEquals(1, query.count());
    EncryptedData encryptedData = query.get();
    assertNotNull(encryptedData.getKmsId());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(serviceVariable, appId, workflowExecutionId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));

    secretName = UUID.randomUUID().toString();
    secretValue = UUID.randomUUID().toString();
    secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

    String updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());

    wingsPersistence.updateFields(ServiceVariable.class, savedServiceVariableId, keyValuePairs);
    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedServiceVariableId);
    assertEquals(1, query.count());

    encryptedData = query.get();
    assertEquals(accountId, encryptedData.getAccountId());
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());
    assertEquals(SettingVariableTypes.SECRET_TEXT, encryptedData.getType());
    assertNotNull(encryptedData.getKmsId());
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
  @Category(UnitTests.class)
  public void kmsEncryptionUpdateObject() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    User user1 = User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName("user1").build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.count());
    EncryptedData encryptedData = query.get();
    assertEquals(kmsConfig.getUuid(), encryptedData.getKmsId());

    verifyChangeLogs(savedAttributeId, savedAttribute, user1);

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = getAppDynamicsConfig(accountId, newPassWord);

    String updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("value", newAppDynamicsConfig);

    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);
    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertEquals(1, query.count());

    encryptedData = query.get();
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
  @Category(UnitTests.class)
  public void kmsEncryptionUpdateFieldSettingAttribute() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    String updatedAppId = UUID.randomUUID().toString();
    wingsPersistence.updateField(SettingAttribute.class, savedAttributeId, "appId", updatedAppId);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(updatedAppId, updatedAttribute.getAppId());
    savedAttribute.setAppId(updatedAppId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
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

    // test decryption
    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertThat(savedConfig.getPassword()).isNull();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertEquals(newPassWord, String.valueOf(savedConfig.getPassword()));

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
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

    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    User user2 =
        User.Builder.anUser().withEmail(UUID.randomUUID().toString()).withName(UUID.randomUUID().toString()).build();
    wingsPersistence.save(user2);
    UserThreadLocal.set(user2);
    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
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
  }

  @Test
  @Category(UnitTests.class)
  public void updateSettingAttributeAfterKmsEnabled() {
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).count());
    List<EncryptedData> encryptedDataList = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                                .filter("type", SettingVariableTypes.APP_DYNAMICS)
                                                .asList();
    assertEquals(1, encryptedDataList.size());
    EncryptedData encryptedData = encryptedDataList.get(0);
    assertEquals(accountId, encryptedData.getEncryptionKey());
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());

    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = getAppDynamicsConfig(accountId, newPassWord);

    String updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("value", newAppDynamicsConfig);

    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);

    encryptedDataList = wingsPersistence.createQuery(EncryptedData.class)
                            .filter(EncryptedDataKeys.accountId, accountId)
                            .filter(EncryptedDataKeys.type, SettingVariableTypes.APP_DYNAMICS)
                            .asList();
    assertEquals(1, encryptedDataList.size());
    encryptedData = encryptedDataList.get(0);
    assertNotEquals(accountId, encryptedData.getEncryptionKey());
    assertEquals(EncryptionType.KMS, encryptedData.getEncryptionType());
    // test decryption
    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertThat(savedConfig.getPassword()).isNull();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertEquals(newPassWord, String.valueOf(savedConfig.getPassword()));
  }

  @Test
  @Category(UnitTests.class)
  public void saveServiceVariableNoKMS() {
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
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(0, wingsPersistence.createQuery(EncryptedData.class).count());

    // update to encrypt the variable
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.ENCRYPTED_TEXT, savedAttribute.getType());
    assertThat(savedAttribute.getValue()).isNull();
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, workflowExecutionId, appId));
    assertEquals(secretValue, String.valueOf(savedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).count());

    keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.TEXT);
    keyValuePairs.put("value", "unencrypted".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.TEXT, savedAttribute.getType());
    assertEquals("unencrypted", String.valueOf(savedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).count());
  }

  @Test
  @Category(UnitTests.class)
  public void saveServiceVariableNoEncryption() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(numOfEncryptedValsForKms, wingsPersistence.createQuery(EncryptedData.class).count());

    // update to encrypt the variable
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.ENCRYPTED_TEXT, savedAttribute.getType());
    assertThat(savedAttribute.getValue()).isNull();
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, workflowExecutionId, appId));
    assertEquals(secretValue, String.valueOf(savedAttribute.getValue()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.TEXT);
    keyValuePairs.put("value", "unencrypted".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertEquals(Type.TEXT, savedAttribute.getType());
    assertEquals("unencrypted", new String(savedAttribute.getValue()));
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());
    assertTrue(isEmpty(wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                           .filter("type", SettingVariableTypes.SECRET_TEXT)
                           .asList()
                           .get(0)
                           .getParentIds()));
  }

  @Test
  @Category(UnitTests.class)
  public void getSecretMappedToAccount() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    // update to encrypt the variable
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    secretManager.saveSecret(accountId, secretName, secretValue, null, null);

    UserPermissionInfo userPermissionInfo = UsageRestrictionsServiceImplTest.getUserPermissionInfo(
        ImmutableList.of(appId), ImmutableList.of(envId), ImmutableSet.of(Action.UPDATE));

    User user = User.Builder.anUser().withName(USER_NAME).withUuid(USER_ID).build();
    user.setUserRequestContext(
        UserRequestContext.builder().appIds(ImmutableSet.of(appId)).userPermissionInfo(userPermissionInfo).build());
    UserThreadLocal.set(user);

    EncryptedData secretByName = secretManager.getSecretMappedToAccountByName(accountId, secretName);
    assertThat(secretByName).isNotNull();
    assertThat(secretByName.getName()).isEqualTo(secretName);

    secretByName = secretManager.getSecretMappedToAppByName(accountId, appId, envId, secretName);
    assertThat(secretByName).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void getSecretMappedToApp() {
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();

    AppEnvRestrictionBuilder appEnvRestrictionBuilder =
        AppEnvRestriction.builder()
            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .envFilter(EnvFilter.builder().filterTypes(ImmutableSet.of(EnvFilter.FilterType.PROD)).build());
    secretManager.saveSecret(accountId, secretName, secretValue, null,
        UsageRestrictions.builder().appEnvRestrictions(ImmutableSet.of(appEnvRestrictionBuilder.build())).build());

    UserPermissionInfo userPermissionInfo = UsageRestrictionsServiceImplTest.getUserPermissionInfo(
        ImmutableList.of(appId), ImmutableList.of(envId), ImmutableSet.of(Action.UPDATE, Action.CREATE, Action.READ));

    User user = User.Builder.anUser().withName(USER_NAME).withUuid(USER_ID).build();
    UsageRestrictions restrictionsFromUserPermissionsForRead =
        UsageRestrictions.builder().appEnvRestrictions(ImmutableSet.of(appEnvRestrictionBuilder.build())).build();

    Map<String, Set<String>> appEnvMapForRead = ImmutableMap.of(appId, ImmutableSet.of(envId));

    user.setUserRequestContext(
        UserRequestContext.builder()
            .appIds(ImmutableSet.of(appId))
            .userPermissionInfo(userPermissionInfo)
            .userRestrictionInfo(UserRestrictionInfo.builder()
                                     .appEnvMapForReadAction(appEnvMapForRead)
                                     .usageRestrictionsForReadAction(restrictionsFromUserPermissionsForRead)
                                     .build())
            .build());
    UserThreadLocal.set(user);

    EncryptedData secretByName = secretManager.getSecretMappedToAccountByName(accountId, secretName);
    assertThat(secretByName).isNull();

    secretByName = secretManager.getSecretMappedToAppByName(accountId, appId, envId, secretName);
    assertThat(secretByName).isNotNull();
    assertThat(secretByName.getName()).isEqualTo(secretName);
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void kmsEncryptionSaveServiceVariable() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

    // try with invalid secret id
    final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(generateUuid())
                                                .envId(generateUuid())
                                                .entityType(EntityType.SERVICE)
                                                .entityId(generateUuid())
                                                .parentServiceVariableId(generateUuid())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(generateUuid()))
                                                .expression(generateUuid())
                                                .accountId(accountId)
                                                .name(generateUuid())
                                                .value(generateUuid().toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    try {
      serviceVariableResource.save(appId, serviceVariable);
      fail("saved invalid service variable");
    } catch (WingsException e) {
      // expected
    }

    serviceVariable.setValue(secretId.toCharArray());
    String savedAttributeId = serviceVariableResource.save(appId, serviceVariable).getResource().getUuid();
    ServiceVariable savedAttribute = serviceVariableResource.get(appId, savedAttributeId, false).getResource();
    assertNotNull(savedAttribute.getSecretTextName());
    serviceVariable.setSecretTextName(savedAttribute.getSecretTextName());
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    secretName = UUID.randomUUID().toString();
    secretValue = UUID.randomUUID().toString();
    secretId = secretManager.saveSecret(accountId, secretName, secretValue, null, null);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", "newName");
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    assertEquals(numOfEncryptedValsForKms + 2, wingsPersistence.createQuery(EncryptedData.class).count());

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

    // decrypt at manager side and test
    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertThat(savedVariable.getValue()).isNull();
    managerDecryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, workflowExecutionId, appId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));
    assertThat(savedVariable.getEncryptedValue()).isNull();

    // update serviceVariable with invalid reference and fail
    serviceVariable.setValue(generateUuid().toCharArray());
    try {
      serviceVariableResource.update(appId, savedAttributeId, serviceVariable);
      fail("updated invalid service variable");
    } catch (WingsException e) {
      // expected
    }
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryptionSaveServiceVariableTemplate() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertThat(savedAttribute.getValue()).isNull();
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId));
    assertEquals(secretValue, String.valueOf(savedAttribute.getValue()));

    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void kmsEncryptionUpdateServiceVariable() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    String updatedEnvId = UUID.randomUUID().toString();
    wingsPersistence.updateField(ServiceVariable.class, savedAttributeId, "envId", updatedEnvId);

    ServiceVariable updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    savedAttribute.setEnvId(updatedEnvId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    updatedEnvId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    String updatedSecretName = UUID.randomUUID().toString();
    String updatedSecretValue = UUID.randomUUID().toString();
    String updatedSecretId = secretManager.saveSecret(accountId, updatedSecretName, updatedSecretValue, null, null);

    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("envId", updatedEnvId);
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", updatedSecretId.toCharArray());

    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    assertEquals(updatedName, updatedAttribute.getName());
    assertThat(updatedAttribute.getValue()).isNull();
    encryptionService.decrypt(
        updatedAttribute, secretManager.getEncryptionDetails(updatedAttribute, workflowExecutionId, appId));
    assertEquals(updatedSecretValue, String.valueOf(updatedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).count());
    assertEquals(numOfEncryptedValsForKms + 2, wingsPersistence.createQuery(EncryptedData.class).count());
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryptionDeleteSettingAttribute() throws IOException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(
        numOfEncryptedValsForKms + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(settingAttributes.get(i));
      assertEquals(numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).count());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).count());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryptionDeleteSettingAttributeQueryUuid() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(
        numOfEncryptedValsForKms + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(accountId, SettingAttribute.class, settingAttributes.get(i).getUuid());
      assertEquals(numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).count());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).count());
    }

    wingsPersistence.save(settingAttributes);
    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(
        numOfEncryptedValsForKms + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(
          SettingAttribute.class, settingAttributes.get(i).getAppId(), settingAttributes.get(i).getUuid());
      assertEquals(numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).count());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).count());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryptionDeleteSettingAttributeQuery() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(
        numOfEncryptedValsForKms + numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).count());

    Set<String> idsToDelete = new HashSet<>();
    idsToDelete.add(settingAttributes.get(0).getUuid());
    idsToDelete.add(settingAttributes.get(1).getUuid());
    Query<SettingAttribute> query = wingsPersistence.createQuery(SettingAttribute.class)
                                        .field(Mapper.ID_KEY)
                                        .hasAnyOf(idsToDelete)
                                        .filter(SettingAttributeKeys.accountId, accountId);
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(query);
      assertEquals(
          numOfSettingAttributes - idsToDelete.size(), wingsPersistence.createQuery(SettingAttribute.class).count());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - idsToDelete.size(),
          wingsPersistence.createQuery(EncryptedData.class).count());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void kmsEncryptionSaveGlobalConfig() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(GLOBAL_ACCOUNT_ID, KryoUtils.clone(kmsConfig));
    assertEquals(1, wingsPersistence.createQuery(KmsConfig.class).count());

    KmsConfig savedKmsConfig = (KmsConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
    assertNotNull(savedKmsConfig);

    assertEquals(GLOBAL_ACCOUNT_ID, savedKmsConfig.getAccountId());
    assertEquals(kmsConfig.getAccessKey(), savedKmsConfig.getAccessKey());
    assertEquals(kmsConfig.getSecretKey(), savedKmsConfig.getSecretKey());
    assertEquals(kmsConfig.getKmsArn(), savedKmsConfig.getKmsArn());

    KmsConfig encryptedKms = wingsPersistence.getDatastore(KmsConfig.class).createQuery(KmsConfig.class).get();

    assertNotEquals(encryptedKms.getAccessKey(), savedKmsConfig.getAccessKey());
    assertNotEquals(encryptedKms.getSecretKey(), savedKmsConfig.getSecretKey());
    assertNotEquals(encryptedKms.getKmsArn(), savedKmsConfig.getKmsArn());
  }

  @Test
  @Category(UnitTests.class)
  public void listEncryptedValues() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      String password = UUID.randomUUID().toString();
      final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
      SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

      wingsPersistence.save(settingAttribute);
      appDynamicsConfig.setPassword(null);
      settingAttribute.setEncryptionType(EncryptionType.KMS);
      settingAttribute.setEncryptedBy(kmsConfig.getName());
      settingAttributes.add(settingAttribute);
    }

    Collection<UuidAware> encryptedValues = secretManagementResource.listEncryptedValues(accountId).getResource();
    validateContainEncryptedValues(settingAttributes, encryptedValues);

    for (UuidAware encryptedValue : encryptedValues) {
      assertEquals(EncryptionType.KMS, ((SettingAttribute) encryptedValue).getEncryptionType());
    }

    RestResponse<PageResponse<UuidAware>> restResponse = secretManagementResource.listEncryptedValues(
        accountId, SettingVariableTypes.APP_DYNAMICS, PageRequestBuilder.aPageRequest().build());
    encryptedValues = restResponse.getResource().getResponse();
    validateContainEncryptedValues(settingAttributes, encryptedValues);

    for (UuidAware encryptedValue : encryptedValues) {
      assertEquals(EncryptionType.KMS, ((SettingAttribute) encryptedValue).getEncryptionType());
    }
  }

  private void validateContainEncryptedValues(
      List<SettingAttribute> settingAttributes, Collection<UuidAware> encryptedValues) {
    assertEquals(settingAttributes.size(), encryptedValues.size());
    Map<String, SettingAttribute> settingAttributeMap = new HashMap<>();
    for (SettingAttribute settingAttribute : settingAttributes) {
      settingAttributeMap.put(settingAttribute.getName(), settingAttribute);
    }

    for (UuidAware encryptedValue : encryptedValues) {
      SettingAttribute settingAttribute = (SettingAttribute) encryptedValue;
      assertTrue(settingAttributeMap.containsKey(settingAttribute.getName()));
      AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
      assertArrayEquals(SecretManager.ENCRYPTED_FIELD_MASK.toCharArray(), appDynamicsConfig.getPassword());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void listKmsConfigMultiple() {
    KmsConfig kmsConfig1 = getKmsConfig();
    kmsConfig1.setDefault(true);
    kmsConfig1.setName(UUID.randomUUID().toString());
    kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig1));

    KmsConfig kmsConfig2 = getKmsConfig();
    kmsConfig2.setDefault(false);
    kmsConfig2.setName(UUID.randomUUID().toString());
    String kms2Id = kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig2)).getResource();

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(2, kmsConfigs.size());

    int defaultConfig = 0;
    int nonDefaultConfig = 0;

    for (SecretManagerConfig config : kmsConfigs) {
      KmsConfig actualConfig = (KmsConfig) config;
      if (actualConfig.isDefault()) {
        defaultConfig++;
        assertEquals(kmsConfig1.getName(), actualConfig.getName());
        assertEquals(kmsConfig1.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(SECRET_MASK, actualConfig.getKmsArn());
        assertEquals(SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(isEmpty(actualConfig.getUuid()));
        assertEquals(accountId, actualConfig.getAccountId());
      } else {
        nonDefaultConfig++;
        assertEquals(kmsConfig2.getName(), actualConfig.getName());
        assertEquals(kmsConfig2.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(SECRET_MASK, actualConfig.getKmsArn());
        assertEquals(SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(isEmpty(actualConfig.getUuid()));
        assertEquals(accountId, actualConfig.getAccountId());
      }
    }

    assertEquals(1, defaultConfig);
    assertEquals(1, nonDefaultConfig);

    // Update to set the non-default to default secret manage
    kmsConfig2.setUuid(kms2Id);
    kmsConfig2.setName(UUID.randomUUID().toString());
    kmsConfig2.setDefault(true);

    kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig2));

    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(2, kmsConfigs.size());

    defaultConfig = 0;
    nonDefaultConfig = 0;
    for (SecretManagerConfig config : kmsConfigs) {
      KmsConfig actualConfig = (KmsConfig) config;
      if (actualConfig.isDefault()) {
        defaultConfig++;
        assertEquals(kmsConfig2.getName(), actualConfig.getName());
        assertEquals(kmsConfig2.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(SECRET_MASK, actualConfig.getKmsArn());
        assertEquals(SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(isEmpty(actualConfig.getUuid()));
        assertEquals(accountId, actualConfig.getAccountId());
      } else {
        nonDefaultConfig++;
        assertEquals(kmsConfig1.getName(), actualConfig.getName());
        assertEquals(kmsConfig1.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(SECRET_MASK, actualConfig.getKmsArn());
        assertEquals(SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(isEmpty(actualConfig.getUuid()));
        assertEquals(accountId, actualConfig.getAccountId());
      }
    }

    assertEquals(1, defaultConfig);
    assertEquals(1, nonDefaultConfig);
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void listKmsGlobalDefault() {
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setName("Global config");

    globalKmsConfig.setDefault(false);
    kmsResource.saveGlobalKmsConfig(accountId, globalKmsConfig);

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(1, kmsConfigs.size());
    assertTrue(kmsConfigs.iterator().next().isDefault());

    int numOfKms = 10;
    for (int i = 1; i <= numOfKms; i++) {
      KmsConfig kmsConfig = getKmsConfig();
      kmsConfig.setDefault(true);
      kmsConfig.setName("kms" + i);
      kmsResource.saveKmsConfig(accountId, kmsConfig);
    }

    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(numOfKms + 1, kmsConfigs.size());

    int kmsNum = numOfKms;
    for (SecretManagerConfig kmsConfig : kmsConfigs) {
      if (kmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
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
    kmsResource.deleteKmsConfig(accountId, kmsConfigs.iterator().next().getUuid());
    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(numOfKms, kmsConfigs.size());

    int defaultSet = 0;
    kmsNum = numOfKms - 1;
    for (SecretManagerConfig kmsConfig : kmsConfigs) {
      if (kmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
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
  @Category(UnitTests.class)
  public void listKmsConfigOrder() {
    int numOfKms = 10;
    for (int i = 1; i <= numOfKms; i++) {
      KmsConfig kmsConfig = getKmsConfig();
      kmsConfig.setDefault(true);
      kmsConfig.setName("kms" + i);
      kmsResource.saveKmsConfig(accountId, kmsConfig);
    }

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(numOfKms, kmsConfigs.size());

    int kmsNum = numOfKms;
    for (SecretManagerConfig kmsConfig : kmsConfigs) {
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
  @Category(UnitTests.class)
  public void listKmsConfigHasDefault() {
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setDefault(false);
    globalKmsConfig.setName("global-kms-config");
    kmsResource.saveGlobalKmsConfig(accountId, KryoUtils.clone(globalKmsConfig));

    KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(2, kmsConfigs.size());

    int defaultConfig = 0;
    int accountConfig = 0;

    for (SecretManagerConfig secretManagerConfig : kmsConfigs) {
      KmsConfig actualConfig = (KmsConfig) secretManagerConfig;
      if (actualConfig.isDefault()) {
        accountConfig++;
        assertEquals(kmsConfig.getName(), actualConfig.getName());
        assertEquals(kmsConfig.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(SECRET_MASK, actualConfig.getKmsArn());
        assertEquals(SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(isEmpty(actualConfig.getUuid()));
      } else {
        defaultConfig++;
        assertEquals(globalKmsConfig.getName(), actualConfig.getName());
        assertEquals(globalKmsConfig.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(SECRET_MASK, actualConfig.getKmsArn());
        assertEquals(SECRET_MASK, actualConfig.getSecretKey());
        assertFalse(isEmpty(actualConfig.getUuid()));
        assertEquals(GLOBAL_ACCOUNT_ID, actualConfig.getAccountId());
      }
    }

    assertEquals(1, defaultConfig);
    assertEquals(1, accountConfig);

    // test with unmasked
    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, false);
    assertEquals(2, kmsConfigs.size());

    defaultConfig = 0;
    accountConfig = 0;

    for (SecretManagerConfig secretManagerConfig : kmsConfigs) {
      KmsConfig actualConfig = (KmsConfig) secretManagerConfig;
      if (actualConfig.isDefault()) {
        accountConfig++;
        assertEquals(kmsConfig.getName(), actualConfig.getName());
        assertEquals(kmsConfig.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(kmsConfig.getKmsArn(), actualConfig.getKmsArn());
        assertEquals(kmsConfig.getSecretKey(), actualConfig.getSecretKey());
        assertFalse(isEmpty(actualConfig.getUuid()));
      } else {
        defaultConfig++;
        assertEquals(globalKmsConfig.getName(), actualConfig.getName());
        assertEquals(globalKmsConfig.getAccessKey(), actualConfig.getAccessKey());
        assertEquals(globalKmsConfig.getKmsArn(), actualConfig.getKmsArn());
        assertEquals(globalKmsConfig.getSecretKey(), actualConfig.getSecretKey());
        assertFalse(isEmpty(actualConfig.getUuid()));
        assertEquals(GLOBAL_ACCOUNT_ID, actualConfig.getAccountId());
      }
    }

    assertEquals(1, defaultConfig);
    assertEquals(1, accountConfig);
  }

  @Test
  @Category(UnitTests.class)
  public void listKmsConfig() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(1, kmsConfigs.size());
    KmsConfig actualConfig = (KmsConfig) kmsConfigs.iterator().next();
    assertEquals(kmsConfig.getName(), actualConfig.getName());
    assertEquals(kmsConfig.getAccessKey(), actualConfig.getAccessKey());
    assertEquals(SECRET_MASK, actualConfig.getKmsArn());
    assertEquals(SECRET_MASK, actualConfig.getSecretKey());
    assertFalse(isEmpty(actualConfig.getUuid()));
    assertTrue(actualConfig.isDefault());

    // add another kms
    String name = UUID.randomUUID().toString();
    kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsConfig.setName(name);
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(2, kmsConfigs.size());

    int defaultPresent = 0;
    for (SecretManagerConfig config : kmsConfigs) {
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
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertEquals(3, kmsConfigs.size());

    defaultPresent = 0;
    for (SecretManagerConfig config : kmsConfigs) {
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
  @Category(UnitTests.class)
  public void transitionKms() throws InterruptedException, IllegalAccessException {
    Thread listenerThread = startTransitionListener();
    try {
      KmsConfig fromConfig = getKmsConfig();
      kmsResource.saveKmsConfig(accountId, fromConfig);

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

      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority);
      List<EncryptedData> encryptedData = new ArrayList<>();
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes, query.count());
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
      kmsResource.saveKmsConfig(accountId, toKmsConfig);

      secretManagementResource.transitionSecrets(
          accountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.KMS, toKmsConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority);
      // 2 kms configs have been saved so far
      assertEquals(2 * numOfEncryptedValsForKms + numOfSettingAttributes, query.count());
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
  @Category(UnitTests.class)
  public void transitionAndDeleteKms() throws InterruptedException, IllegalAccessException {
    Thread listenerThread = startTransitionListener();
    try {
      KmsConfig fromConfig = getKmsConfig();
      kmsResource.saveKmsConfig(accountId, fromConfig);

      int numOfSettingAttributes = 5;
      Map<String, SettingAttribute> encryptedEntities = new HashMap<>();
      for (int i = 0; i < numOfSettingAttributes; i++) {
        String password = UUID.randomUUID().toString();
        final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
        SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

        wingsPersistence.save(settingAttribute);
        appDynamicsConfig.setPassword(password.toCharArray());
        encryptedEntities.put(settingAttribute.getUuid(), settingAttribute);
      }

      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes, query.count());

      KmsConfig toKmsConfig = getKmsConfig();
      toKmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/e1aebd89-277b-4ec7-a4e9-9a238f8b2594");
      kmsResource.saveKmsConfig(accountId, toKmsConfig);
      assertEquals(2, wingsPersistence.createQuery(KmsConfig.class).count());

      try {
        kmsResource.deleteKmsConfig(accountId, fromConfig.getUuid());
        fail("Was able to delete kms which has reference in encrypted secrets");
      } catch (WingsException e) {
        // expected
      }

      secretManagementResource.transitionSecrets(
          accountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.KMS, toKmsConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      kmsResource.deleteKmsConfig(accountId, fromConfig.getUuid());
      assertEquals(1, wingsPersistence.createQuery(KmsConfig.class).count());

      query = wingsPersistence.createQuery(EncryptedData.class);
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes, query.count());
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void transitionKmsForConfigFile() throws IOException, InterruptedException, IllegalAccessException {
    Thread listenerThread = startTransitionListener();
    try {
      final long seed = System.currentTimeMillis();
      logger.info("seed: " + seed);
      Random r = new Random(seed);
      Account randomAccount = getAccount(AccountType.PAID);
      String randomAccountId = randomAccount.getUuid();
      when(accountService.get(randomAccountId)).thenReturn(randomAccount);
      final String randomAppId = UUID.randomUUID().toString();
      KmsConfig fromConfig = getKmsConfig();

      when(secretsManagementFeature.isAvailableForAccount(randomAccountId)).thenReturn(true);

      kmsResource.saveKmsConfig(randomAccountId, fromConfig);

      Service service = Service.builder().name(UUID.randomUUID().toString()).appId(randomAppId).build();
      wingsPersistence.save(service);

      String secretName = UUID.randomUUID().toString();
      File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
      String secretFileId = secretManager.saveFile(
          randomAccountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToSave)));
      String encryptedUuid = wingsPersistence.createQuery(EncryptedData.class)
                                 .filter(EncryptedDataKeys.accountId, randomAccountId)
                                 .filter(EncryptedDataKeys.type, CONFIG_FILE)
                                 .get()
                                 .getUuid();

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

      configFile.setAccountId(randomAccountId);
      configFile.setName(UUID.randomUUID().toString());
      configFile.setFileName(UUID.randomUUID().toString());
      configFile.setAppId(randomAppId);

      String configFileId = configService.save(configFile, null);
      File download = configService.download(randomAppId, configFileId);
      assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
          FileUtils.readFileToString(download, Charset.defaultCharset()));

      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedUuid);
      assertNotNull(encryptedData);
      assertEquals(fromConfig.getUuid(), encryptedData.getKmsId());

      KmsConfig toKmsConfig = getKmsConfig();
      toKmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/e1aebd89-277b-4ec7-a4e9-9a238f8b2594");
      kmsResource.saveKmsConfig(randomAccountId, toKmsConfig);

      secretManagementResource.transitionSecrets(
          randomAccountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.KMS, toKmsConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));

      download = configService.download(randomAppId, configFileId);
      assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
          FileUtils.readFileToString(download, Charset.defaultCharset()));
      encryptedData = wingsPersistence.get(EncryptedData.class, encryptedUuid);
      assertNotNull(encryptedData);
      assertEquals(toKmsConfig.getUuid(), encryptedData.getKmsId());
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void saveAwsConfig() {
    KmsConfig fromConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, fromConfig);

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
                                              .withCategory(SettingCategory.CLOUD_PROVIDER)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      encryptedEntities.put(settingAttribute.getUuid(), settingAttribute);
    }

    Collection<UuidAware> uuidAwares = secretManagementResource.listEncryptedValues(accountId).getResource();
    assertEquals(encryptedEntities.size(), uuidAwares.size());
    for (UuidAware encryptedValue : uuidAwares) {
      assertEquals(EncryptionType.KMS, ((SettingAttribute) encryptedValue).getEncryptionType());
    }
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void saveUpdateConfigFileNoKms() throws IOException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);
    final String renameAccountId = UUID.randomUUID().toString();
    final String renameAppId = UUID.randomUUID().toString();

    Service service = Service.builder().name(UUID.randomUUID().toString()).appId(renameAppId).build();
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
                                .encrypted(false)
                                .build();

    configFile.setAccountId(renameAccountId);
    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(renameAppId);

    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());

    String configFileId = configService.save(configFile, new BoundedInputStream(new FileInputStream(fileToSave)));
    File download = configService.download(renameAppId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(
        0, wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).count());
    ConfigFile savedConfigFile = configService.get(renameAppId, configFileId);
    assertFalse(savedConfigFile.isEncrypted());
    assertTrue(isEmpty(savedConfigFile.getEncryptedFileId()));
    assertThat(wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).count())
        .isEqualTo(0);

    // now make the same file encrypted
    String secretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    String secretFileId = secretManager.saveFile(
        renameAccountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToUpdate)));
    configFile.setEncrypted(true);
    configFile.setEncryptedFileId(secretFileId);
    configService.update(configFile, null);
    download = configService.download(renameAppId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    savedConfigFile = configService.get(renameAppId, configFileId);
    assertTrue(savedConfigFile.isEncrypted());
    assertFalse(isEmpty(savedConfigFile.getEncryptedFileId()));

    assertEquals(1,
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).count());
    EncryptedData encryptedData =
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).get();
    assertEquals(renameAccountId, encryptedData.getAccountId());
    assertEquals(1, encryptedData.getParentIds().size());
    assertTrue(encryptedData.getParentIds().contains(configFileId));
    assertEquals(renameAccountId, encryptedData.getEncryptionKey());
    assertEquals(SettingVariableTypes.CONFIG_FILE, encryptedData.getType());
    assertTrue(encryptedData.isEnabled());
    assertTrue(isNotEmpty(encryptedData.getKmsId()));
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());

    // now make the same file not encrypted
    fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    configFile.setEncrypted(false);
    configService.update(configFile, new BoundedInputStream(new FileInputStream(fileToUpdate)));
    download = configService.download(renameAppId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    savedConfigFile = configService.get(renameAppId, configFileId);
    assertFalse(savedConfigFile.isEncrypted());
    assertTrue(isEmpty(savedConfigFile.getEncryptedFileId()));

    assertEquals(1,
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).count());
    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(renameAccountId, secretFileId, SettingVariableTypes.CONFIG_FILE);
    assertEquals(1, changeLogs.size());
    SecretChangeLog changeLog = changeLogs.get(0);
    assertEquals(renameAccountId, changeLog.getAccountId());
    assertEquals(secretFileId, changeLog.getEncryptedDataId());
    assertEquals(userName, changeLog.getUser().getName());
    assertEquals(userEmail, changeLog.getUser().getEmail());
    assertEquals(user.getUuid(), changeLog.getUser().getUuid());
    assertEquals("File uploaded", changeLog.getDescription());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void saveConfigFileNoEncryption() throws IOException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);
    Account renameAccount = getAccount(AccountType.PAID);
    String renameAccountId = renameAccount.getUuid();
    when(accountService.get(renameAccountId)).thenReturn(renameAccount);
    final String renameAppId = UUID.randomUUID().toString();
    KmsConfig fromConfig = getKmsConfig();

    when(secretsManagementFeature.isAvailableForAccount(renameAccountId)).thenReturn(true);

    kmsResource.saveKmsConfig(renameAccountId, fromConfig);

    Service service = Service.builder().name(UUID.randomUUID().toString()).appId(renameAppId).build();
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
                                .encrypted(false)
                                .build();

    configFile.setAccountId(renameAccountId);
    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(renameAppId);

    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());

    configService.save(configFile, new BoundedInputStream(new FileInputStream(fileToSave)));
    File download = configService.download(renameAppId, configFile.getUuid());
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms, wingsPersistence.createQuery(EncryptedData.class).count());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void saveConfigFileWithEncryption() throws IOException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);
    Account randomAccount = getAccount(AccountType.PAID);
    String randomAccountId = randomAccount.getUuid();
    when(accountService.get(randomAccountId)).thenReturn(randomAccount);
    final String randomAppId = UUID.randomUUID().toString();
    KmsConfig fromConfig = getKmsConfig();

    when(secretsManagementFeature.isAvailableForAccount(randomAccountId)).thenReturn(true);

    kmsResource.saveKmsConfig(randomAccountId, fromConfig);

    Service service = Service.builder().name(UUID.randomUUID().toString()).appId(randomAppId).build();
    wingsPersistence.save(service);

    Activity activity = Activity.builder().workflowExecutionId(workflowExecutionId).environmentId(envId).build();
    activity.setAppId(randomAppId);
    wingsPersistence.save(activity);

    String secretName = UUID.randomUUID().toString();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId = secretManager.saveFile(
        randomAccountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToSave)));
    String encryptedUuid = wingsPersistence.createQuery(EncryptedData.class)
                               .filter(EncryptedDataKeys.type, CONFIG_FILE)
                               .filter(EncryptedDataKeys.accountId, randomAccountId)
                               .get()
                               .getUuid();

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

    configFile.setAccountId(randomAccountId);
    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(randomAppId);

    String configFileId = configService.save(configFile, null);
    File download = configService.download(randomAppId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms + 1,
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, randomAccountId).count());

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                                .filter("type", SettingVariableTypes.CONFIG_FILE)
                                                .filter("accountId", randomAccountId)
                                                .asList();
    assertEquals(1, encryptedFileData.size());
    assertEquals(1, encryptedFileData.get(0).getParentIds().size());
    assertTrue(encryptedFileData.get(0).getParentIds().contains(configFileId));

    // test update
    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManager.updateFile(
        randomAccountId, newSecretName, encryptedUuid, null, new BoundedInputStream(new FileInputStream(fileToUpdate)));

    download = configService.download(randomAppId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms + 1,
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, randomAccountId).count());

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                            .filter("type", SettingVariableTypes.CONFIG_FILE)
                            .filter("accountId", randomAccountId)
                            .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(encryptedFileData.get(0).getParentIds().isEmpty());

    int numOfAccess = 7;
    for (int i = 0; i < numOfAccess; i++) {
      configService.downloadForActivity(randomAppId, configFileId, activity.getUuid());
    }
    List<SecretUsageLog> usageLogs =
        (List<SecretUsageLog>) secretManagementResource
            .getUsageLogs(aPageRequest().build(), randomAccountId, encryptedUuid, SettingVariableTypes.CONFIG_FILE)
            .getResource();
    assertEquals(numOfAccess, usageLogs.size());

    for (SecretUsageLog usageLog : usageLogs) {
      assertEquals(workflowName, usageLog.getWorkflowExecutionName());
      assertEquals(randomAccountId, usageLog.getAccountId());
      assertEquals(envId, usageLog.getEnvId());
      assertEquals(randomAppId, usageLog.getAppId());
    }

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(randomAccountId, secretFileId, SettingVariableTypes.CONFIG_FILE);
    assertEquals(2, changeLogs.size());
    SecretChangeLog changeLog = changeLogs.get(0);
    assertEquals(randomAccountId, changeLog.getAccountId());
    assertEquals(secretFileId, changeLog.getEncryptedDataId());
    assertEquals(userName, changeLog.getUser().getName());
    assertEquals(userEmail, changeLog.getUser().getEmail());
    assertEquals(user.getUuid(), changeLog.getUser().getUuid());
    assertEquals("Changed Name and File", changeLog.getDescription());

    changeLog = changeLogs.get(1);
    assertEquals(randomAccountId, changeLog.getAccountId());
    assertEquals(secretFileId, changeLog.getEncryptedDataId());
    assertEquals(userName, changeLog.getUser().getName());
    assertEquals(userEmail, changeLog.getUser().getEmail());
    assertEquals(user.getUuid(), changeLog.getUser().getUuid());
    assertEquals("File uploaded", changeLog.getDescription());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void saveConfigFileTemplateWithEncryption() throws IOException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);
    Account renameAccount = getAccount(AccountType.PAID);
    String renameAccountId = renameAccount.getUuid();
    when(accountService.get(renameAccountId)).thenReturn(renameAccount);
    final String renameAppId = UUID.randomUUID().toString();

    KmsConfig fromConfig = getKmsConfig();

    when(secretsManagementFeature.isAvailableForAccount(renameAccountId)).thenReturn(true);

    kmsResource.saveKmsConfig(renameAccountId, fromConfig);

    Service service = Service.builder().name(UUID.randomUUID().toString()).build();
    service.setAppId(renameAppId);
    String serviceId = wingsPersistence.save(service);
    ServiceTemplate serviceTemplate = ServiceTemplate.Builder.aServiceTemplate().withServiceId(serviceId).build();
    serviceTemplate.setAppId(renameAppId);
    String serviceTemplateId = wingsPersistence.save(serviceTemplate);

    Activity activity = Activity.builder().workflowId(workflowExecutionId).build();
    activity.setAppId(renameAppId);
    wingsPersistence.save(activity);

    String secretName = UUID.randomUUID().toString();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId = secretManager.saveFile(
        renameAccountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToSave)));
    String encryptedUuid = wingsPersistence.createQuery(EncryptedData.class)
                               .filter(EncryptedDataKeys.accountId, renameAccountId)
                               .filter(EncryptedDataKeys.type, CONFIG_FILE)
                               .get()
                               .getUuid();

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
                                .encryptedFileId(secretFileId)
                                .templateId(serviceTemplateId)
                                .encrypted(true)
                                .build();

    configFile.setAccountId(renameAccountId);
    configFile.setName(UUID.randomUUID().toString());
    configFile.setFileName(UUID.randomUUID().toString());
    configFile.setAppId(renameAppId);

    String configFileId = configService.save(configFile, null);
    File download = configService.download(renameAppId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms + 1,
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).count());

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                                .filter("type", SettingVariableTypes.CONFIG_FILE)
                                                .filter("accountId", renameAccountId)
                                                .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(encryptedFileData.get(0).getParentIds().isEmpty());
    // test update
    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManager.updateFile(
        renameAccountId, newSecretName, encryptedUuid, null, new BoundedInputStream(new FileInputStream(fileToUpdate)));

    download = configService.download(renameAppId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
    assertEquals(numOfEncryptedValsForKms + 1,
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).count());

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                            .filter(EncryptedDataKeys.accountId, renameAccountId)
                            .filter(EncryptedDataKeys.type, SettingVariableTypes.CONFIG_FILE)
                            .asList();
    assertEquals(1, encryptedFileData.size());
    assertFalse(encryptedFileData.get(0).getParentIds().isEmpty());
  }

  @Test
  @Category(UnitTests.class)
  public void kmsExceptionTest() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setKmsArn("invalid krn");
    String toEncrypt = UUID.randomUUID().toString();
    try {
      delegateService.encrypt(accountId, toEncrypt.toCharArray(), kmsConfig);
      fail("should have been failed");
    } catch (KmsOperationException e) {
      assertTrue(true);
    }

    kmsConfig = getKmsConfig();
    try {
      delegateService.decrypt(EncryptedData.builder()
                                  .encryptionKey(UUID.randomUUID().toString())
                                  .encryptedValue(toEncrypt.toCharArray())
                                  .build(),
          kmsConfig);
      fail("should have been failed");
    } catch (KmsOperationException e) {
      assertTrue(true);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void reuseYamlPasswordNoEncryption() throws IllegalAccessException {
    int numOfSettingAttributes = 5;
    String password = "password";
    Set<String> attributeIds = new HashSet<>();
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    attributeIds.add(wingsPersistence.save(settingAttribute));

    String yamlRef = secretManager.getEncryptedYamlRef(appDynamicsConfig);

    for (int i = 1; i < numOfSettingAttributes; i++) {
      appDynamicsConfig = getAppDynamicsConfig(accountId, null, yamlRef);
      settingAttribute = getSettingAttribute(appDynamicsConfig);

      attributeIds.add(wingsPersistence.save(settingAttribute));
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).count());

    List<EncryptedData> encryptedDatas = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                             .field("encryptionType")
                                             .notEqual(EncryptionType.KMS)
                                             .asList();
    assertEquals(1, encryptedDatas.size());
    EncryptedData encryptedData = encryptedDatas.get(0);
    assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertNotNull(encryptedData.getKmsId());
    assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
    assertEquals(numOfSettingAttributes, encryptedData.getParentIds().size());
    assertEquals(attributeIds, encryptedData.getParentIds());

    for (String attributeId : attributeIds) {
      SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, attributeId);
      AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
      assertEquals(accountId, savedConfig.getAccountId());
      assertThat(savedConfig.getPassword()).isNull();
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
      encryptedDatas =
          wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).asList();
      if (i == numOfSettingAttributes - 1) {
        assertTrue(encryptedDatas.isEmpty());
      } else {
        assertEquals(1, encryptedDatas.size());
        encryptedData = encryptedDatas.get(0);
        assertEquals(EncryptionType.LOCAL, encryptedData.getEncryptionType());
        assertEquals(accountId, encryptedData.getAccountId());
        assertTrue(encryptedData.isEnabled());
        assertNotNull(encryptedData.getKmsId());
        assertEquals(SettingVariableTypes.APP_DYNAMICS, encryptedData.getType());
        assertEquals(numOfSettingAttributes - (i + 1), encryptedData.getParentIds().size());

        assertFalse(encryptedData.getParentIds().contains(attributeId));
        assertEquals(remainingAttrs, encryptedData.getParentIds());
      }
      i++;
    }
  }

  @Test
  @Category(UnitTests.class)
  public void reuseYamlPasswordKmsEncryption() throws IllegalAccessException {
    KmsConfig fromConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, fromConfig);

    int numOfSettingAttributes = 5;
    String password = "password";
    Set<String> attributeIds = new HashSet<>();
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    attributeIds.add(wingsPersistence.save(settingAttribute));

    String yamlRef = secretManager.getEncryptedYamlRef(appDynamicsConfig);
    for (int i = 1; i < numOfSettingAttributes; i++) {
      appDynamicsConfig = getAppDynamicsConfig(accountId, null, yamlRef);
      settingAttribute = getSettingAttribute(appDynamicsConfig);

      attributeIds.add(wingsPersistence.save(settingAttribute));
    }

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).count());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).count());

    List<EncryptedData> encryptedDatas = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                             .filter("encryptionType", EncryptionType.KMS)
                                             .asList();
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
      assertThat(savedConfig.getPassword()).isNull();
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
      encryptedDatas = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                           .filter("encryptionType", EncryptionType.KMS)
                           .asList();
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
  @Category(UnitTests.class)
  public void reuseYamlPasswordNewEntityKmsEncryption() throws IllegalAccessException {
    KmsConfig fromConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, fromConfig);

    String password = "password";
    Set<String> attributeIds = new HashSet<>();
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    attributeIds.add(wingsPersistence.save(settingAttribute));
    List<EncryptedData> encryptedDataList = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                                .filter("type", SettingVariableTypes.APP_DYNAMICS)
                                                .asList();
    assertEquals(1, encryptedDataList.size());

    String yamlRef = secretManager.getEncryptedYamlRef(appDynamicsConfig);

    String randomPassword = UUID.randomUUID().toString();
    appDynamicsConfig = getAppDynamicsConfig(accountId, randomPassword, yamlRef);
    settingAttribute = getSettingAttribute(appDynamicsConfig);

    SettingAttribute attributeCopy = settingsService.save(settingAttribute);
    encryptedDataList = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                            .filter("type", SettingVariableTypes.APP_DYNAMICS)
                            .asList();
    assertEquals(1, encryptedDataList.size());

    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, attributeCopy.getUuid());
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertEquals(password, String.valueOf(savedConfig.getPassword()));
  }

  @Test
  @Category(UnitTests.class)
  public void getUsageLogs() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    List<SecretUsageLog> usageLogs =
        (List<SecretUsageLog>) secretManagementResource
            .getUsageLogs(aPageRequest().build(), accountId, savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE)
            .getResource();
    assertThat(usageLogs).isEmpty();

    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId);
    usageLogs =
        (List<SecretUsageLog>) secretManagementResource
            .getUsageLogs(aPageRequest().build(), accountId, savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE)
            .getResource();
    assertEquals(1, usageLogs.size());
    assertEquals(workflowName, usageLogs.get(0).getWorkflowExecutionName());
    assertEquals(accountId, usageLogs.get(0).getAccountId());
    assertEquals(envId, usageLogs.get(0).getEnvId());
    assertEquals(appId, usageLogs.get(0).getAppId());

    secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId);
    secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId);
    usageLogs =
        (List<SecretUsageLog>) secretManagementResource
            .getUsageLogs(aPageRequest().build(), accountId, savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE)
            .getResource();
    assertEquals(3, usageLogs.size());

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute appDAttribute = getSettingAttribute(appDynamicsConfig);

    String appDAttributeId = wingsPersistence.save(appDAttribute);
    usageLogs = (List<SecretUsageLog>) secretManagementResource
                    .getUsageLogs(aPageRequest().build(), accountId, appDAttributeId, SettingVariableTypes.APP_DYNAMICS)
                    .getResource();
    assertThat(usageLogs).isEmpty();
    int numOfAccess = 13;
    for (int i = 0; i < numOfAccess; i++) {
      secretManager.getEncryptionDetails((EncryptableSetting) appDAttribute.getValue(), appId, workflowExecutionId);
    }
    usageLogs = (List<SecretUsageLog>) secretManagementResource
                    .getUsageLogs(aPageRequest().build(), accountId, appDAttributeId, SettingVariableTypes.APP_DYNAMICS)
                    .getResource();
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
  public void getChangeLogs() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute appDAttribute = getSettingAttribute(appDynamicsConfig);

    String appDAttributeId = wingsPersistence.save(appDAttribute);
    int numOfUpdates = 13;
    for (int i = 0; i < numOfUpdates; i++) {
      appDynamicsConfig.setPassword(UUID.randomUUID().toString().toCharArray());
      wingsPersistence.save(appDAttribute);
    }

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(accountId, appDAttributeId, SettingVariableTypes.APP_DYNAMICS);
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

  private KmsConfig getNonDefaultKmsConfig() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setDefault(false);

    return kmsConfig;
  }

  private KmsConfig getDefaultKmsConfig() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);

    return kmsConfig;
  }
}
