package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
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
import software.wings.beans.WorkflowExecution;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.features.api.PremiumFeature;
import software.wings.resources.KmsResource;
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
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.impl.UsageRestrictionsServiceImplTest;
import software.wings.service.impl.security.GlobalEncryptDecryptClient;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.impl.security.SecretManagementDelegateException;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.AccountService;
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
  @Mock private AccountService accountService;
  @Inject private HarnessUserGroupService harnessUserGroupService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SettingsService settingsService;
  @Inject private SettingValidationService settingValidationService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceVariableResource serviceVariableResource;
  @Inject private SecretManagementDelegateService delegateService;
  @Inject private QueueConsumer<KmsTransitionEvent> kmsTransitionConsumer;
  @Mock private ContainerService containerService;
  @Mock private NewRelicService newRelicService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SecretManagementDelegateService secretManagementDelegateService;
  @Mock private PremiumFeature secretsManagementFeature;
  @Mock protected AuditServiceHelper auditServiceHelper;
  @Mock private GlobalEncryptDecryptClient globalEncryptDecryptClient;
  @Inject @InjectMocks private KmsService kmsService;
  @Inject @InjectMocks private SecretManagerConfigService secretManagerConfigService;
  private final int numOfEncryptedValsForKms = 3;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().email(userEmail).name(userName).build();
  private String userId;
  private String accountId;
  private String kmsId;
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

    when(globalEncryptDecryptClient.encrypt(anyString(), any(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return encrypt((String) args[0], (char[]) args[1], (KmsConfig) args[2]);
    });

    when(globalEncryptDecryptClient.decrypt(anyObject(), anyString(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (KmsConfig) args[2]);
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
  @Owner(developers = UTKARSH)
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
    assertThat(savedConfig).isEqualTo(kmsConfig);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @RealMongo
  public void getGetGlobalKmsConfig() {
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setName("Global config");
    globalKmsConfig.setDefault(true);
    kmsResource.saveGlobalKmsConfig(accountId, KryoUtils.clone(globalKmsConfig));

    KmsConfig savedGlobalKmsConfig = kmsService.getGlobalKmsConfig();
    assertThat(savedGlobalKmsConfig).isNotNull();

    // Verified that retrieved global KMS config secret fields are decrypted properly.
    assertThat(savedGlobalKmsConfig.getName()).isEqualTo(globalKmsConfig.getName());
    assertThat(savedGlobalKmsConfig.getAccessKey()).isEqualTo(globalKmsConfig.getAccessKey());
    assertThat(savedGlobalKmsConfig.getSecretKey()).isEqualTo(globalKmsConfig.getSecretKey());
    assertThat(savedGlobalKmsConfig.getKmsArn()).isEqualTo(globalKmsConfig.getKmsArn());
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    String secretFileId = secretManager.saveFile(randomAccountId, kmsId, secretName, fileToSave.length(), null,
        new BoundedInputStream(new FileInputStream(fileToSave)));
    assertThat(secretFileId).isNotNull();

    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    boolean result = secretManager.updateFile(randomAccountId, newSecretName, secretFileId, fileToUpdate.length(), null,
        new BoundedInputStream(new FileInputStream(fileToUpdate)));
    assertThat(result).isTrue();

    assertThat(secretManager.deleteFile(randomAccountId, secretFileId)).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void validateConfig() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setAccessKey("invalidKey");

    try {
      kmsResource.saveKmsConfig(kmsConfig.getAccountId(), kmsConfig);
      fail("Saved invalid kms config");
    } catch (SecretManagementException e) {
      assertThat(true).isTrue();
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void getKmsConfigForAccount() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);

    kmsResource.saveKmsConfig(kmsConfig.getAccountId(), KryoUtils.clone(kmsConfig));

    KmsConfig savedConfig = (KmsConfig) secretManagerConfigService.getDefaultSecretManager(kmsConfig.getAccountId());
    kmsConfig.setUuid(savedConfig.getUuid());
    assertThat(savedConfig).isEqualTo(kmsConfig);
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(savedConfig).isEqualTo(kmsConfig);
    assertThat(savedConfig.getName()).isEqualTo(name);
    List<EncryptedData> encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).asList();
    assertThat(encryptedDataList).hasSize(numOfEncryptedValsForKms);
    for (EncryptedData encryptedData : encryptedDataList) {
      assertThat(encryptedData.getName().equals(name + "_accessKey")
          || encryptedData.getName().equals(name + "_secretKey") || encryptedData.getName().equals(name + "_arn"))
          .isTrue();
      assertThat(encryptedData.getParentIds()).hasSize(1);
      assertThat(encryptedData.getParentIds().iterator().next()).isEqualTo(savedConfig.getUuid());
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
    assertThat(encryptedDataList).hasSize(numOfEncryptedValsForKms);
    for (EncryptedData encryptedData : encryptedDataList) {
      assertThat(encryptedData.getName().equals(name + "_accessKey")
          || encryptedData.getName().equals(name + "_secretKey") || encryptedData.getName().equals(name + "_arn"))
          .isTrue();
      assertThat(encryptedData.getParentIds()).hasSize(1);
      assertThat(encryptedData.getParentIds().iterator().next()).isEqualTo(savedConfig.getUuid());
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveAndEditConfig_withMaskedSecrets_changeNameDefaultOnly() {
    String name = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setName(name);
    kmsConfig.setAccountId(accountId);

    kmsService.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));

    KmsConfig savedConfig = (KmsConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
    assertThat(savedConfig.getAccessKey()).isEqualTo(kmsConfig.getAccessKey());
    assertThat(savedConfig.getSecretKey()).isEqualTo(kmsConfig.getSecretKey());
    assertThat(savedConfig.getKmsArn()).isEqualTo(kmsConfig.getKmsArn());
    assertThat(savedConfig.getName()).isEqualTo(kmsConfig.getName());
    assertThat(savedConfig.isDefault()).isEqualTo(true);

    String newName = UUID.randomUUID().toString();
    kmsConfig.setUuid(savedConfig.getUuid());
    kmsConfig.setName(newName);
    kmsConfig.setDefault(false);
    kmsConfig.maskSecrets();

    // Masked Secrets, only name and default flag should be updated.
    kmsService.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));

    KmsConfig modifiedSavedConfig = kmsService.getKmsConfig(accountId, savedConfig.getUuid());
    assertThat(modifiedSavedConfig.getAccessKey()).isEqualTo(savedConfig.getAccessKey());
    assertThat(modifiedSavedConfig.getSecretKey()).isEqualTo(savedConfig.getSecretKey());
    assertThat(modifiedSavedConfig.getKmsArn()).isEqualTo(savedConfig.getKmsArn());
    assertThat(modifiedSavedConfig.getName()).isEqualTo(kmsConfig.getName());
    assertThat(modifiedSavedConfig.isDefault()).isEqualTo(false);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void localNullEncryption() {
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, null, null);
    assertThat(encryptedData.getEncryptedValue()).isNull();
    assertThat(isBlank(encryptedData.getEncryptionKey())).isFalse();

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, null);
    assertThat(decryptedValue).isNull();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void localEncryption() {
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt.toCharArray(), null, null);
    assertThat(keyToEncrypt).isNotEqualTo(new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, null);
    assertThat(new String(decryptedValue)).isEqualTo(keyToEncrypt);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsNullEncryption() {
    final KmsConfig kmsConfig = getKmsConfig();
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, UUID.randomUUID().toString(), kmsConfig);
    assertThat(encryptedData.getEncryptedValue()).isNull();
    assertThat(isBlank(encryptedData.getEncryptionKey())).isFalse();

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, kmsConfig);
    assertThat(decryptedValue).isNull();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryption() {
    final KmsConfig kmsConfig = getKmsConfig();
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData =
        kmsService.encrypt(keyToEncrypt.toCharArray(), UUID.randomUUID().toString(), kmsConfig);
    assertThat(keyToEncrypt).isNotEqualTo(new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null, kmsConfig);
    assertThat(new String(decryptedValue)).isEqualTo(keyToEncrypt);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void localEncryptionWhileSaving() {
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    appDynamicsConfig.setPassword(password.toCharArray());
    appDynamicsConfig.setEncryptedPassword(null);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()).isNotNull();
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getPassword()).isNull();
    encryptionService.decrypt((EncryptableSetting) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), workflowExecutionId, appId));
    assertThat(savedAttribute.getValue()).isEqualTo(appDynamicsConfig);
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()).isNull();
    assertThat(new String(((AppDynamicsConfig) savedAttribute.getValue()).getPassword())).isEqualTo(password);
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(((ArtifactoryConfig) savedAttribute.getValue()).getEncryptedPassword()).isNull();
    assertThat(((ArtifactoryConfig) savedAttribute.getValue()).getPassword()).isNull();
    encryptionService.decrypt((EncryptableSetting) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), workflowExecutionId, appId));
    artifactoryConfig.setEncryptedPassword(null);
    assertThat(savedAttribute.getValue()).isEqualTo(artifactoryConfig);
    assertThat(((ArtifactoryConfig) savedAttribute.getValue()).getEncryptedPassword()).isNull();
    assertThat(((ArtifactoryConfig) savedAttribute.getValue()).getPassword()).isNull();
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertThat(query.count()).isEqualTo(0);
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(savedConfig.getEncryptedPassword()).isNotNull();
    assertThat(savedConfig.getPassword()).isNull();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertThat(savedConfig).isEqualTo(appDynamicsConfig);
    assertThat(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword()).isNull();
    assertThat(new String(savedConfig.getPassword())).isEqualTo(password);
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(savedAttribute.getValue()).isEqualTo(appDynamicsConfig);
    assertThat(isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword())).isFalse();
    encryptionService.decrypt((EncryptableSetting) savedAttribute.getValue(),
        secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), workflowExecutionId, appId));
    assertThat(new String(((AppDynamicsConfig) savedAttribute.getValue()).getPassword())).isEqualTo(password);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryptionWhileSaving() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute.getValue()).isEqualTo(appDynamicsConfig);
    assertThat(isBlank(((AppDynamicsConfig) savedAttribute.getValue()).getEncryptedPassword())).isFalse();

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(settingAttribute.getUuid());
    assertThat(query.count()).isEqualTo(1);
    EncryptedData encryptedData = query.get();
    assertThat(encryptedData.getKmsId()).isEqualTo(kmsConfig.getUuid());
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
    assertThat(query.count()).isEqualTo(numOfEncryptedValsForKms + 1);
  }

  @Test
  @Owner(developers = ANKIT)
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
      fail("");
    } catch (WingsException e) {
      assertThat(e.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(encryptedDataQuery.count()).isEqualTo(1);
    EncryptedData encryptedData = encryptedDataQuery.get();

    secretManager.getEncryptionDetails((EncryptableSetting) savedAttribute.getValue(), appId, workflowExecutionId);
    Query<SecretUsageLog> query = wingsPersistence.createQuery(SecretUsageLog.class);
    assertThat(query.count()).isEqualTo(1);
    SecretUsageLog usageLog = query.get();
    assertThat(usageLog.getAccountId()).isEqualTo(accountId);
    assertThat(usageLog.getWorkflowExecutionId()).isEqualTo(workflowExecutionId);
    assertThat(usageLog.getAppId()).isEqualTo(appId);
    assertThat(usageLog.getEncryptedDataId()).isEqualTo(encryptedData.getUuid());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryptionSaveMultiple() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    wingsPersistence.save(settingAttributes);

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertThat(kmsConfigs).hasSize(1);
    assertThat(kmsConfigs.iterator().next().getNumOfEncryptedValue()).isEqualTo(numOfSettingAttributes);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNumOfEncryptedValue() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveGlobalKmsConfig(accountId, kmsConfig);

    int numOfSettingAttributes1 = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes1);
    wingsPersistence.save(settingAttributes);

    final String accountId2 = UUID.randomUUID().toString();
    settingAttributes.clear();

    int numOfSettingAttributes2 = 7;
    settingAttributes = getSettingAttributes(accountId2, numOfSettingAttributes2);
    wingsPersistence.save(settingAttributes);

    List<SecretManagerConfig> encryptionConfigs =
        secretManagementResource.listEncryptionConfig(accountId).getResource();
    assertThat(encryptionConfigs).hasSize(1);
    assertThat(encryptionConfigs.get(0).getNumOfEncryptedValue()).isEqualTo(numOfSettingAttributes1);

    encryptionConfigs = secretManagementResource.listEncryptionConfig(accountId2).getResource();
    assertThat(encryptionConfigs).hasSize(1);
    assertThat(encryptionConfigs.get(0).getNumOfEncryptedValue()).isEqualTo(numOfSettingAttributes2);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void noKmsEncryptionUpdateObject() throws IllegalAccessException {
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(settingAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(1);

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    User user1 = User.Builder.anUser().email(UUID.randomUUID().toString()).name("user1").build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(updatedAttribute).isEqualTo(savedAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(1);

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);
    EncryptedData encryptedData = query.get();
    assertThat(encryptedData.getKmsId()).isNotNull();

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
    assertThat(query.count()).isEqualTo(1);

    encryptedData = query.get();
    assertThat(encryptedData.getAccountId()).isEqualTo(accountId);
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
    assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.APP_DYNAMICS);
    assertThat(encryptedData.getKmsId()).isNotNull();
    assertThat(encryptedData.getEncryptionKey()).isNotNull();
    assertThat(encryptedData.getEncryptedValue()).isNotNull();

    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute.getName()).isEqualTo(updatedName);
    assertThat(savedAttribute.getAppId()).isEqualTo(updatedAppId);

    AppDynamicsConfig updatedAppdynamicsConfig = (AppDynamicsConfig) savedAttribute.getValue();
    encryptionService.decrypt(
        updatedAppdynamicsConfig, secretManager.getEncryptionDetails(updatedAppdynamicsConfig, null, null));
    assertThat(String.valueOf(updatedAppdynamicsConfig.getPassword())).isEqualTo(newPassWord);
  }

  private void verifyChangeLogs(String savedAttributeId, SettingAttribute savedAttribute, User user1)
      throws IllegalAccessException {
    Query<EncryptedData> query;
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
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void noKmsEncryptionUpdateServiceVariable() {
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

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
    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(1);
    assertThat(savedVariable.getValue()).isNull();
    assertThat(savedVariable.getEncryptedValue()).isNotNull();

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedServiceVariableId);
    assertThat(query.count()).isEqualTo(1);
    EncryptedData encryptedData = query.get();
    assertThat(encryptedData.getKmsId()).isNotNull();

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(serviceVariable, appId, workflowExecutionId));
    assertThat(String.valueOf(savedVariable.getValue())).isEqualTo(secretValue);

    secretName = UUID.randomUUID().toString();
    secretValue = UUID.randomUUID().toString();
    secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

    String updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());

    wingsPersistence.updateFields(ServiceVariable.class, savedServiceVariableId, keyValuePairs);
    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedServiceVariableId);
    assertThat(query.count()).isEqualTo(1);

    encryptedData = query.get();
    assertThat(encryptedData.getAccountId()).isEqualTo(accountId);
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
    assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.SECRET_TEXT);
    assertThat(encryptedData.getKmsId()).isNotNull();
    assertThat(encryptedData.getEncryptionKey()).isNotNull();
    assertThat(encryptedData.getEncryptedValue()).isNotNull();

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedServiceVariableId);
    assertThat(savedVariable.getName()).isEqualTo(updatedName);
    assertThat(savedVariable.getAppId()).isEqualTo(updatedAppId);
    assertThat(savedVariable.getEncryptedValue()).isEqualTo(secretId);

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertThat(String.valueOf(savedVariable.getValue())).isEqualTo(secretValue);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryptionUpdateObject() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(settingAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    User user1 = User.Builder.anUser().email(UUID.randomUUID().toString()).name("user1").build();
    wingsPersistence.save(user1);
    UserThreadLocal.set(user1);
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(updatedAttribute).isEqualTo(savedAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
    assertThat(query.count()).isEqualTo(1);
    EncryptedData encryptedData = query.get();
    assertThat(encryptedData.getKmsId()).isEqualTo(kmsConfig.getUuid());

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
    assertThat(query.count()).isEqualTo(1);

    encryptedData = query.get();
    assertThat(encryptedData.getAccountId()).isEqualTo(accountId);
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.KMS);
    assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.APP_DYNAMICS);
    assertThat(encryptedData.getKmsId()).isEqualTo(kmsConfig.getUuid());
    assertThat(encryptedData.getEncryptionKey()).isNotNull();
    assertThat(encryptedData.getEncryptedValue()).isNotNull();

    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute.getName()).isEqualTo(updatedName);
    assertThat(savedAttribute.getAppId()).isEqualTo(updatedAppId);

    AppDynamicsConfig updatedAppdynamicsConfig = (AppDynamicsConfig) savedAttribute.getValue();
    encryptionService.decrypt(
        updatedAppdynamicsConfig, secretManager.getEncryptionDetails(updatedAppdynamicsConfig, null, null));
    assertThat(String.valueOf(updatedAppdynamicsConfig.getPassword())).isEqualTo(newPassWord);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryptionUpdateFieldSettingAttribute() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(settingAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    String updatedAppId = UUID.randomUUID().toString();
    wingsPersistence.updateField(SettingAttribute.class, savedAttributeId, "appId", updatedAppId);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(updatedAttribute.getAppId()).isEqualTo(updatedAppId);
    savedAttribute.setAppId(updatedAppId);
    assertThat(updatedAttribute).isEqualTo(savedAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
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

    // test decryption
    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertThat(savedConfig.getPassword()).isNull();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertThat(String.valueOf(savedConfig.getPassword())).isEqualTo(newPassWord);

    query = wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(savedAttributeId);
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

    assertThat(updatedAttribute.getValue()).isEqualTo(newAppDynamicsConfig);
    newAppDynamicsConfig.setPassword(UUID.randomUUID().toString().toCharArray());

    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    User user2 = User.Builder.anUser().email(UUID.randomUUID().toString()).name(UUID.randomUUID().toString()).build();
    wingsPersistence.save(user2);
    UserThreadLocal.set(user2);
    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);

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
  public void updateSettingAttributeAfterKmsEnabled() {
    String password = UUID.randomUUID().toString();
    final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertThat(savedAttribute).isEqualTo(settingAttribute);
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(1);
    List<EncryptedData> encryptedDataList = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                                .filter("type", SettingVariableTypes.APP_DYNAMICS)
                                                .asList();
    assertThat(encryptedDataList).hasSize(1);
    EncryptedData encryptedData = encryptedDataList.get(0);
    assertThat(encryptedData.getEncryptionKey()).isEqualTo(accountId);
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);

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
    assertThat(encryptedDataList).hasSize(1);
    encryptedData = encryptedDataList.get(0);
    assertThat(accountId).isNotEqualTo(encryptedData.getEncryptionKey());
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.KMS);
    // test decryption
    savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    assertThat(savedConfig.getPassword()).isNull();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertThat(String.valueOf(savedConfig.getPassword())).isEqualTo(newPassWord);
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(savedAttribute).isEqualTo(serviceVariable);
    assertThat(String.valueOf(savedAttribute.getValue())).isEqualTo(value);
    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(0);

    // update to encrypt the variable
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertThat(savedAttribute.getType()).isEqualTo(Type.ENCRYPTED_TEXT);
    assertThat(savedAttribute.getValue()).isNull();
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, workflowExecutionId, appId));
    assertThat(String.valueOf(savedAttribute.getValue())).isEqualTo(secretValue);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(1);

    keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.TEXT);
    keyValuePairs.put("value", "unencrypted".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertThat(savedAttribute.getType()).isEqualTo(Type.TEXT);
    assertThat(String.valueOf(savedAttribute.getValue())).isEqualTo("unencrypted");
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(savedAttribute).isEqualTo(serviceVariable);
    assertThat(String.valueOf(savedAttribute.getValue())).isEqualTo(value);
    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms);

    // update to encrypt the variable
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertThat(savedAttribute.getType()).isEqualTo(Type.ENCRYPTED_TEXT);
    assertThat(savedAttribute.getValue()).isNull();
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, workflowExecutionId, appId));
    assertThat(String.valueOf(savedAttribute.getValue())).isEqualTo(secretValue);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    keyValuePairs = new HashMap<>();
    keyValuePairs.put("type", Type.TEXT);
    keyValuePairs.put("value", "unencrypted".toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, serviceVariable.getUuid(), keyValuePairs);
    savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);

    assertThat(savedAttribute.getType()).isEqualTo(Type.TEXT);
    assertThat(new String(savedAttribute.getValue())).isEqualTo("unencrypted");
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);
    assertThat(isEmpty(wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                           .filter("type", SettingVariableTypes.SECRET_TEXT)
                           .asList()
                           .get(0)
                           .getParentIds()))
        .isTrue();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void getSecretMappedToAccount() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    // update to encrypt the variable
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

    UserPermissionInfo userPermissionInfo = UsageRestrictionsServiceImplTest.getUserPermissionInfo(
        ImmutableList.of(appId), ImmutableList.of(envId), ImmutableSet.of(Action.UPDATE));

    User user = User.Builder.anUser().name(USER_NAME).uuid(USER_ID).build();
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
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void getSecretMappedToApp() {
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();

    AppEnvRestrictionBuilder appEnvRestrictionBuilder =
        AppEnvRestriction.builder()
            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .envFilter(EnvFilter.builder().filterTypes(ImmutableSet.of(EnvFilter.FilterType.PROD)).build());
    secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null,
        UsageRestrictions.builder().appEnvRestrictions(ImmutableSet.of(appEnvRestrictionBuilder.build())).build());

    UserPermissionInfo userPermissionInfo = UsageRestrictionsServiceImplTest.getUserPermissionInfo(
        ImmutableList.of(appId), ImmutableList.of(envId), ImmutableSet.of(Action.UPDATE, Action.CREATE, Action.READ));

    User user = User.Builder.anUser().name(USER_NAME).uuid(USER_ID).build();
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
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @RealMongo
  public void kmsEncryptionSaveServiceVariable() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

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
    assertThat(savedAttribute.getSecretTextName()).isNotNull();
    serviceVariable.setSecretTextName(savedAttribute.getSecretTextName());
    assertThat(savedAttribute).isEqualTo(serviceVariable);
    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    secretName = UUID.randomUUID().toString();
    secretValue = UUID.randomUUID().toString();
    secretId = secretManager.saveSecret(accountId, kmsId, secretName, secretValue, null, null);

    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", "newName");
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId.toCharArray());
    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 2);

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

    // decrypt at manager side and test
    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertThat(savedVariable.getValue()).isNull();
    managerDecryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, workflowExecutionId, appId));
    assertThat(String.valueOf(savedVariable.getValue())).isEqualTo(secretValue);
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryptionSaveServiceVariableTemplate() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    encryptionService.decrypt(
        savedAttribute, secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId));
    assertThat(String.valueOf(savedAttribute.getValue())).isEqualTo(secretValue);

    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @RealMongo
  public void kmsEncryptionUpdateServiceVariable() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    String updatedEnvId = UUID.randomUUID().toString();
    wingsPersistence.updateField(ServiceVariable.class, savedAttributeId, "envId", updatedEnvId);

    ServiceVariable updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertThat(updatedAttribute.getEnvId()).isEqualTo(updatedEnvId);
    savedAttribute.setEnvId(updatedEnvId);
    assertThat(updatedAttribute).isEqualTo(savedAttribute);
    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    updatedEnvId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    String updatedSecretName = UUID.randomUUID().toString();
    String updatedSecretValue = UUID.randomUUID().toString();
    String updatedSecretId =
        secretManager.saveSecret(accountId, kmsId, updatedSecretName, updatedSecretValue, null, null);

    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("envId", updatedEnvId);
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", updatedSecretId.toCharArray());

    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertThat(updatedAttribute.getEnvId()).isEqualTo(updatedEnvId);
    assertThat(updatedAttribute.getName()).isEqualTo(updatedName);
    assertThat(updatedAttribute.getValue()).isNull();
    encryptionService.decrypt(
        updatedAttribute, secretManager.getEncryptionDetails(updatedAttribute, workflowExecutionId, appId));
    assertThat(String.valueOf(updatedAttribute.getValue())).isEqualTo(updatedSecretValue);
    assertThat(wingsPersistence.createQuery(ServiceVariable.class).count()).isEqualTo(1);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 2);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryptionDeleteSettingAttribute() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    wingsPersistence.save(settingAttributes);

    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(numOfSettingAttributes);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count())
        .isEqualTo(numOfEncryptedValsForKms + numOfSettingAttributes);
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(settingAttributes.get(i));
      assertThat(wingsPersistence.createQuery(SettingAttribute.class).count())
          .isEqualTo(numOfSettingAttributes - (i + 1));
      assertThat(wingsPersistence.createQuery(EncryptedData.class).count())
          .isEqualTo(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1));
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryptionDeleteSettingAttributeQueryUuid() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    for (SettingAttribute settingAttribute : settingAttributes) {
      wingsPersistence.save(settingAttribute);
    }
    validateSettingAttributes(settingAttributes, numOfEncryptedValsForKms + numOfSettingAttributes);

    settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    wingsPersistence.save(settingAttributes);
    validateSettingAttributes(settingAttributes, numOfEncryptedValsForKms + numOfSettingAttributes);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsEncryptionDeleteSettingAttributeQuery() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    wingsPersistence.save(settingAttributes);

    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(numOfSettingAttributes);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count())
        .isEqualTo(numOfEncryptedValsForKms + numOfSettingAttributes);

    Set<String> idsToDelete = new HashSet<>();
    idsToDelete.add(settingAttributes.get(0).getUuid());
    idsToDelete.add(settingAttributes.get(1).getUuid());
    Query<SettingAttribute> query = wingsPersistence.createQuery(SettingAttribute.class)
                                        .field(Mapper.ID_KEY)
                                        .hasAnyOf(idsToDelete)
                                        .filter(SettingAttributeKeys.accountId, accountId);
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(query);
      assertThat(wingsPersistence.createQuery(SettingAttribute.class).count())
          .isEqualTo(numOfSettingAttributes - idsToDelete.size());
      assertThat(wingsPersistence.createQuery(EncryptedData.class).count())
          .isEqualTo(numOfEncryptedValsForKms + numOfSettingAttributes - idsToDelete.size());
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void kmsEncryptionSaveGlobalConfig() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveGlobalKmsConfig(GLOBAL_ACCOUNT_ID, KryoUtils.clone(kmsConfig));
    assertThat(wingsPersistence.createQuery(KmsConfig.class).count()).isEqualTo(1);

    KmsConfig savedKmsConfig = (KmsConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
    assertThat(savedKmsConfig).isNotNull();

    assertThat(savedKmsConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);
    assertThat(savedKmsConfig.getAccessKey()).isEqualTo(kmsConfig.getAccessKey());
    assertThat(savedKmsConfig.getSecretKey()).isEqualTo(kmsConfig.getSecretKey());
    assertThat(savedKmsConfig.getKmsArn()).isEqualTo(kmsConfig.getKmsArn());

    KmsConfig encryptedKms = wingsPersistence.getDatastore(KmsConfig.class).createQuery(KmsConfig.class).get();

    assertThat(encryptedKms.getAccessKey()).isNotEqualTo(savedKmsConfig.getAccessKey());
    assertThat(encryptedKms.getSecretKey()).isNotEqualTo(savedKmsConfig.getSecretKey());
    assertThat(encryptedKms.getKmsArn()).isNotEqualTo(savedKmsConfig.getKmsArn());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void listEncryptedValues() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = getSettingAttributes(accountId, numOfSettingAttributes);
    wingsPersistence.save(settingAttributes);

    Collection<SettingAttribute> encryptedValues =
        secretManagementResource.listEncryptedSettingAttributes(accountId, null).getResource();
    validateContainEncryptedValues(settingAttributes, encryptedValues);
    for (SettingAttribute encryptedValue : encryptedValues) {
      assertThat(encryptedValue.getEncryptionType()).isEqualTo(EncryptionType.KMS);
    }

    // Retrieving the setting attributes of category CONNECTOR.
    encryptedValues =
        secretManagementResource.listEncryptedSettingAttributes(accountId, SettingCategory.CONNECTOR.name())
            .getResource();
    validateContainEncryptedValues(settingAttributes, encryptedValues);
    for (SettingAttribute encryptedValue : encryptedValues) {
      assertThat(encryptedValue.getEncryptionType()).isEqualTo(EncryptionType.KMS);
    }
  }

  private void validateContainEncryptedValues(
      List<SettingAttribute> settingAttributes, Collection<SettingAttribute> encryptedValues) {
    assertThat(encryptedValues).hasSize(settingAttributes.size());
    Map<String, SettingAttribute> settingAttributeMap = new HashMap<>();
    for (SettingAttribute settingAttribute : settingAttributes) {
      settingAttributeMap.put(settingAttribute.getName(), settingAttribute);
    }

    for (SettingAttribute settingAttribute : encryptedValues) {
      assertThat(settingAttributeMap.containsKey(settingAttribute.getName())).isTrue();
      AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
      assertThat(appDynamicsConfig.getPassword()).isEqualTo(SecretManager.ENCRYPTED_FIELD_MASK.toCharArray());
    }
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(kmsConfigs).hasSize(2);

    int defaultConfig = 0;
    int nonDefaultConfig = 0;

    for (SecretManagerConfig config : kmsConfigs) {
      KmsConfig actualConfig = (KmsConfig) config;
      if (actualConfig.isDefault()) {
        defaultConfig++;
        assertThat(actualConfig.getName()).isEqualTo(kmsConfig1.getName());
        assertThat(actualConfig.getAccessKey()).isEqualTo(kmsConfig1.getAccessKey());
        assertThat(actualConfig.getKmsArn()).isEqualTo(SECRET_MASK);
        assertThat(actualConfig.getSecretKey()).isEqualTo(SECRET_MASK);
        assertThat(isEmpty(actualConfig.getUuid())).isFalse();
        assertThat(actualConfig.getAccountId()).isEqualTo(accountId);
      } else {
        nonDefaultConfig++;
        assertThat(actualConfig.getName()).isEqualTo(kmsConfig2.getName());
        assertThat(actualConfig.getAccessKey()).isEqualTo(kmsConfig2.getAccessKey());
        assertThat(actualConfig.getKmsArn()).isEqualTo(SECRET_MASK);
        assertThat(actualConfig.getSecretKey()).isEqualTo(SECRET_MASK);
        assertThat(isEmpty(actualConfig.getUuid())).isFalse();
        assertThat(actualConfig.getAccountId()).isEqualTo(accountId);
      }
    }

    assertThat(defaultConfig).isEqualTo(1);
    assertThat(nonDefaultConfig).isEqualTo(1);

    // Update to set the non-default to default secret manage
    kmsConfig2.setUuid(kms2Id);
    kmsConfig2.setName(UUID.randomUUID().toString());
    kmsConfig2.setDefault(true);

    kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig2));

    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertThat(kmsConfigs).hasSize(2);

    defaultConfig = 0;
    nonDefaultConfig = 0;
    for (SecretManagerConfig config : kmsConfigs) {
      KmsConfig actualConfig = (KmsConfig) config;
      if (actualConfig.isDefault()) {
        defaultConfig++;
        assertThat(actualConfig.getName()).isEqualTo(kmsConfig2.getName());
        assertThat(actualConfig.getAccessKey()).isEqualTo(kmsConfig2.getAccessKey());
        assertThat(actualConfig.getKmsArn()).isEqualTo(SECRET_MASK);
        assertThat(actualConfig.getSecretKey()).isEqualTo(SECRET_MASK);
        assertThat(isEmpty(actualConfig.getUuid())).isFalse();
        assertThat(actualConfig.getAccountId()).isEqualTo(accountId);
      } else {
        nonDefaultConfig++;
        assertThat(actualConfig.getName()).isEqualTo(kmsConfig1.getName());
        assertThat(actualConfig.getAccessKey()).isEqualTo(kmsConfig1.getAccessKey());
        assertThat(actualConfig.getKmsArn()).isEqualTo(SECRET_MASK);
        assertThat(actualConfig.getSecretKey()).isEqualTo(SECRET_MASK);
        assertThat(isEmpty(actualConfig.getUuid())).isFalse();
        assertThat(actualConfig.getAccountId()).isEqualTo(accountId);
      }
    }

    assertThat(defaultConfig).isEqualTo(1);
    assertThat(nonDefaultConfig).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void resetDefaultShouldNotAcrossAccount() {
    int numOfKmsConfigs = 3;
    Set<String> kmsIds = new HashSet<>();
    for (int i = 0; i < numOfKmsConfigs; i++) {
      kmsIds.add(saveKmsConfigWithAccount(getAccount(AccountType.PAID)));
    }

    for (String kmsId : kmsIds) {
      KmsConfig kmsConfig = wingsPersistence.get(KmsConfig.class, kmsId);
      // Each KMS config should be default of its own account and not affecting each other.
      assertThat(kmsConfig.isDefault()).isTrue();
    }
  }

  private String saveKmsConfigWithAccount(Account account) {
    String accountId = wingsPersistence.save(account);
    when(accountService.get(accountId)).thenReturn(account);
    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(true);

    User user = User.Builder.anUser()
                    .accounts(Collections.singletonList(account))
                    .email(UUID.randomUUID().toString())
                    .name(UUID.randomUUID().toString())
                    .build();
    wingsPersistence.save(user);

    UserThreadLocal.set(user);
    KmsConfig kmsConfig = getKmsConfig();
    return kmsService.saveKmsConfig(accountId, kmsConfig);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void deleteGlobalKmsNotAllowed() {
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setName("Global config");
    globalKmsConfig.setDefault(true);
    String kmsId = kmsService.saveGlobalKmsConfig(accountId, globalKmsConfig);

    try {
      kmsService.deleteKmsConfig(accountId, kmsId);
      fail("Exception expected when deleting global KMS secret manager");
    } catch (SecretManagementException e) {
      // Global kMS operation exception expected.
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void listKmsGlobalDefault() {
    KmsConfig globalKmsConfig = getKmsConfig();
    globalKmsConfig.setName("Global config");

    globalKmsConfig.setDefault(false);
    kmsResource.saveGlobalKmsConfig(accountId, globalKmsConfig);

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertThat(kmsConfigs).hasSize(1);
    assertThat(kmsConfigs.iterator().next().isDefault()).isTrue();

    int numOfKms = 10;
    for (int i = 1; i <= numOfKms; i++) {
      KmsConfig kmsConfig = getKmsConfig();
      kmsConfig.setDefault(true);
      kmsConfig.setName("kms" + i);
      kmsResource.saveKmsConfig(accountId, kmsConfig);
    }

    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertThat(kmsConfigs).hasSize(numOfKms + 1);

    int kmsNum = numOfKms;
    for (SecretManagerConfig kmsConfig : kmsConfigs) {
      if (kmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
        assertThat(kmsConfig.isDefault()).isFalse();
        assertThat(kmsConfig.getName()).isEqualTo("Global config");
      } else {
        assertThat(kmsConfig.getName()).isEqualTo("kms" + kmsNum);
      }
      if (kmsNum == numOfKms) {
        assertThat(kmsConfig.isDefault()).isTrue();
      } else {
        assertThat(kmsConfig.isDefault()).isFalse();
      }
      kmsNum--;
    }

    // delete the default and global should become default
    kmsResource.deleteKmsConfig(accountId, kmsConfigs.iterator().next().getUuid());
    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertThat(kmsConfigs).hasSize(numOfKms);

    int defaultSet = 0;
    kmsNum = numOfKms - 1;
    for (SecretManagerConfig kmsConfig : kmsConfigs) {
      if (kmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
        assertThat(kmsConfig.isDefault()).isTrue();
        assertThat(kmsConfig.getName()).isEqualTo("Global config");
        defaultSet++;
      } else {
        assertThat(kmsConfig.isDefault()).isFalse();
        assertThat(kmsConfig.getName()).isEqualTo("kms" + kmsNum);
      }
      kmsNum--;
    }

    assertThat(defaultSet).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(kmsConfigs).hasSize(numOfKms);

    int kmsNum = numOfKms;
    for (SecretManagerConfig kmsConfig : kmsConfigs) {
      if (kmsNum == numOfKms) {
        assertThat(kmsConfig.isDefault()).isTrue();
      } else {
        assertThat(kmsConfig.isDefault()).isFalse();
      }
      assertThat(kmsConfig.getName()).isEqualTo("kms" + kmsNum);
      kmsNum--;
    }
  }

  @Test
  @Owner(developers = RAGHU)
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
    assertThat(kmsConfigs).hasSize(2);

    int defaultConfig = 0;
    int accountConfig = 0;

    for (SecretManagerConfig secretManagerConfig : kmsConfigs) {
      KmsConfig actualConfig = (KmsConfig) secretManagerConfig;
      if (actualConfig.isDefault()) {
        accountConfig++;
        assertThat(actualConfig.getName()).isEqualTo(kmsConfig.getName());
        assertThat(actualConfig.getAccessKey()).isEqualTo(kmsConfig.getAccessKey());
        assertThat(actualConfig.getKmsArn()).isEqualTo(SECRET_MASK);
        assertThat(actualConfig.getSecretKey()).isEqualTo(SECRET_MASK);
        assertThat(isEmpty(actualConfig.getUuid())).isFalse();
      } else {
        defaultConfig++;
        assertThat(actualConfig.getName()).isEqualTo(globalKmsConfig.getName());
        assertThat(actualConfig.getAccessKey()).isEqualTo(globalKmsConfig.getAccessKey());
        assertThat(actualConfig.getKmsArn()).isEqualTo(SECRET_MASK);
        assertThat(actualConfig.getSecretKey()).isEqualTo(SECRET_MASK);
        assertThat(isEmpty(actualConfig.getUuid())).isFalse();
        assertThat(actualConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);
      }
    }

    assertThat(defaultConfig).isEqualTo(1);
    assertThat(accountConfig).isEqualTo(1);

    // test with unmasked
    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, false);
    assertThat(kmsConfigs).hasSize(2);

    defaultConfig = 0;
    accountConfig = 0;

    for (SecretManagerConfig secretManagerConfig : kmsConfigs) {
      KmsConfig actualConfig = (KmsConfig) secretManagerConfig;
      if (actualConfig.isDefault()) {
        accountConfig++;
        assertThat(actualConfig.getName()).isEqualTo(kmsConfig.getName());
        assertThat(actualConfig.getAccessKey()).isEqualTo(kmsConfig.getAccessKey());
        assertThat(actualConfig.getKmsArn()).isEqualTo(kmsConfig.getKmsArn());
        assertThat(actualConfig.getSecretKey()).isEqualTo(kmsConfig.getSecretKey());
        assertThat(isEmpty(actualConfig.getUuid())).isFalse();
      } else {
        defaultConfig++;
        assertThat(actualConfig.getName()).isEqualTo(globalKmsConfig.getName());
        assertThat(actualConfig.getAccessKey()).isEqualTo(globalKmsConfig.getAccessKey());
        assertThat(actualConfig.getKmsArn()).isEqualTo(globalKmsConfig.getKmsArn());
        assertThat(actualConfig.getSecretKey()).isEqualTo(globalKmsConfig.getSecretKey());
        assertThat(isEmpty(actualConfig.getUuid())).isFalse();
        assertThat(actualConfig.getAccountId()).isEqualTo(GLOBAL_ACCOUNT_ID);
      }
    }

    assertThat(defaultConfig).isEqualTo(1);
    assertThat(accountConfig).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void listKmsConfig() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));

    Collection<SecretManagerConfig> kmsConfigs =
        secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertThat(kmsConfigs).hasSize(1);
    KmsConfig actualConfig = (KmsConfig) kmsConfigs.iterator().next();
    assertThat(actualConfig.getName()).isEqualTo(kmsConfig.getName());
    assertThat(actualConfig.getAccessKey()).isEqualTo(kmsConfig.getAccessKey());
    assertThat(actualConfig.getKmsArn()).isEqualTo(SECRET_MASK);
    assertThat(actualConfig.getSecretKey()).isEqualTo(SECRET_MASK);
    assertThat(isEmpty(actualConfig.getUuid())).isFalse();
    assertThat(actualConfig.isDefault()).isTrue();

    // add another kms
    String name = UUID.randomUUID().toString();
    kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsConfig.setName(name);
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertThat(kmsConfigs).hasSize(2);

    int defaultPresent = 0;
    for (SecretManagerConfig config : kmsConfigs) {
      if (config.getName().equals(name)) {
        defaultPresent++;
        assertThat(config.isDefault()).isTrue();
      } else {
        assertThat(config.isDefault()).isFalse();
      }
    }

    assertThat(defaultPresent).isEqualTo(1);

    name = UUID.randomUUID().toString();
    kmsConfig = getKmsConfig();
    kmsConfig.setDefault(true);
    kmsConfig.setName(name);
    kmsResource.saveKmsConfig(accountId, kmsConfig);

    kmsConfigs = secretManagerConfigService.listSecretManagersByType(accountId, EncryptionType.KMS, true);
    assertThat(kmsConfigs).hasSize(3);

    defaultPresent = 0;
    for (SecretManagerConfig config : kmsConfigs) {
      if (config.getName().equals(name)) {
        defaultPresent++;
        assertThat(config.isDefault()).isTrue();
      } else {
        assertThat(config.isDefault()).isFalse();
      }
    }
    assertThat(defaultPresent).isEqualTo(1);
  }

  @Test
  @Owner(developers = RAGHU)
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
      assertThat(query.count()).isEqualTo(numOfEncryptedValsForKms + numOfSettingAttributes);
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null) {
          continue;
        }
        encryptedData.add(data);
        assertThat(data.getKmsId()).isEqualTo(fromConfig.getUuid());
        assertThat(data.getAccountId()).isEqualTo(accountId);
      }

      assertThat(encryptedData).hasSize(numOfSettingAttributes);

      KmsConfig toKmsConfig = getKmsConfig();
      toKmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/e1aebd89-277b-4ec7-a4e9-9a238f8b2594");
      kmsResource.saveKmsConfig(accountId, toKmsConfig);

      secretManagementResource.transitionSecrets(
          accountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.KMS, toKmsConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority);
      // 2 kms configs have been saved so far
      assertThat(query.count()).isEqualTo(2 * numOfEncryptedValsForKms + numOfSettingAttributes);
      encryptedData = new ArrayList<>();
      for (EncryptedData data : query.asList()) {
        if (data.getKmsId() == null) {
          continue;
        }
        encryptedData.add(data);
        assertThat(data.getKmsId()).isEqualTo(toKmsConfig.getUuid());
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
      assertThat(query.count()).isEqualTo(numOfEncryptedValsForKms + numOfSettingAttributes);

      KmsConfig toKmsConfig = getKmsConfig();
      toKmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/e1aebd89-277b-4ec7-a4e9-9a238f8b2594");
      kmsResource.saveKmsConfig(accountId, toKmsConfig);
      assertThat(wingsPersistence.createQuery(KmsConfig.class).count()).isEqualTo(2);

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
      assertThat(wingsPersistence.createQuery(KmsConfig.class).count()).isEqualTo(1);

      query = wingsPersistence.createQuery(EncryptedData.class);
      assertThat(query.count()).isEqualTo(numOfEncryptedValsForKms + numOfSettingAttributes);
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  @Owner(developers = ANKIT, intermittent = true)
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
      String secretFileId = secretManager.saveFile(randomAccountId, kmsId, secretName, fileToSave.length(), null,
          new BoundedInputStream(new FileInputStream(fileToSave)));
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
      assertThat(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()))
          .isEqualTo(FileUtils.readFileToString(download, Charset.defaultCharset()));

      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedUuid);
      assertThat(encryptedData).isNotNull();
      assertThat(encryptedData.getKmsId()).isEqualTo(fromConfig.getUuid());

      KmsConfig toKmsConfig = getKmsConfig();
      toKmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/e1aebd89-277b-4ec7-a4e9-9a238f8b2594");
      kmsResource.saveKmsConfig(randomAccountId, toKmsConfig);

      secretManagementResource.transitionSecrets(
          randomAccountId, EncryptionType.KMS, fromConfig.getUuid(), EncryptionType.KMS, toKmsConfig.getUuid());
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));

      download = configService.download(randomAppId, configFileId);
      assertThat(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()))
          .isEqualTo(FileUtils.readFileToString(download, Charset.defaultCharset()));
      encryptedData = wingsPersistence.get(EncryptedData.class, encryptedUuid);
      assertThat(encryptedData).isNotNull();
      assertThat(encryptedData.getKmsId()).isEqualTo(toKmsConfig.getUuid());
    } finally {
      stopTransitionListener(listenerThread);
    }
  }

  @Test
  @Owner(developers = RAGHU)
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

    Collection<SettingAttribute> uuidAwares =
        secretManagementResource.listEncryptedSettingAttributes(accountId, null).getResource();
    assertThat(uuidAwares).hasSize(encryptedEntities.size());
    for (SettingAttribute encryptedValue : uuidAwares) {
      assertThat(encryptedValue.getEncryptionType()).isEqualTo(EncryptionType.KMS);
    }
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()));
    assertThat(wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).count())
        .isEqualTo(0);
    ConfigFile savedConfigFile = configService.get(renameAppId, configFileId);
    assertThat(savedConfigFile.isEncrypted()).isFalse();
    assertThat(isEmpty(savedConfigFile.getEncryptedFileId())).isTrue();
    assertThat(wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).count())
        .isEqualTo(0);

    // now make the same file encrypted
    String secretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    String secretFileId = secretManager.saveFile(renameAccountId, kmsId, secretName, fileToUpdate.length(), null,
        new BoundedInputStream(new FileInputStream(fileToUpdate)));
    configFile.setEncrypted(true);
    configFile.setEncryptedFileId(secretFileId);
    configService.update(configFile, null);
    download = configService.download(renameAppId, configFileId);
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()));
    savedConfigFile = configService.get(renameAppId, configFileId);
    assertThat(savedConfigFile.isEncrypted()).isTrue();
    assertThat(isEmpty(savedConfigFile.getEncryptedFileId())).isFalse();

    assertThat(
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).count())
        .isEqualTo(1);
    EncryptedData encryptedData =
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).get();
    assertThat(encryptedData.getAccountId()).isEqualTo(renameAccountId);
    assertThat(encryptedData.getParentIds()).hasSize(1);
    assertThat(encryptedData.getParentIds().contains(configFileId)).isTrue();
    assertThat(encryptedData.getEncryptionKey()).isEqualTo(renameAccountId);
    assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.CONFIG_FILE);
    assertThat(encryptedData.isEnabled()).isTrue();
    assertThat(isNotEmpty(encryptedData.getKmsId())).isTrue();
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);

    // now make the same file not encrypted
    fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    configFile.setEncrypted(false);
    configService.update(configFile, new BoundedInputStream(new FileInputStream(fileToUpdate)));
    download = configService.download(renameAppId, configFileId);
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()));
    savedConfigFile = configService.get(renameAppId, configFileId);
    assertThat(savedConfigFile.isEncrypted()).isFalse();
    assertThat(isEmpty(savedConfigFile.getEncryptedFileId())).isTrue();

    assertThat(
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).count())
        .isEqualTo(1);
    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(renameAccountId, secretFileId, SettingVariableTypes.CONFIG_FILE);
    assertThat(changeLogs).hasSize(1);
    SecretChangeLog changeLog = changeLogs.get(0);
    assertThat(changeLog.getAccountId()).isEqualTo(renameAccountId);
    assertThat(changeLog.getEncryptedDataId()).isEqualTo(secretFileId);
    assertThat(changeLog.getUser().getName()).isEqualTo(userName);
    assertThat(changeLog.getUser().getEmail()).isEqualTo(userEmail);
    assertThat(changeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(changeLog.getDescription()).isEqualTo("File uploaded");
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()));
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms);
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    String secretFileId = secretManager.saveFile(randomAccountId, kmsId, secretName, fileToSave.length(), null,
        new BoundedInputStream(new FileInputStream(fileToSave)));
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
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()));
    assertThat(
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, randomAccountId).count())
        .isEqualTo(numOfEncryptedValsForKms + 1);

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                                .filter("type", SettingVariableTypes.CONFIG_FILE)
                                                .filter("accountId", randomAccountId)
                                                .asList();
    assertThat(encryptedFileData).hasSize(1);
    assertThat(encryptedFileData.get(0).getParentIds()).hasSize(1);
    assertThat(encryptedFileData.get(0).getParentIds().contains(configFileId)).isTrue();

    // test update
    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManager.updateFile(randomAccountId, newSecretName, encryptedUuid, fileToUpdate.length(), null,
        new BoundedInputStream(new FileInputStream(fileToUpdate)));

    download = configService.download(randomAppId, configFileId);
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()));
    assertThat(
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, randomAccountId).count())
        .isEqualTo(numOfEncryptedValsForKms + 1);

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                            .filter("type", SettingVariableTypes.CONFIG_FILE)
                            .filter("accountId", randomAccountId)
                            .asList();
    assertThat(encryptedFileData).hasSize(1);
    assertThat(encryptedFileData.get(0).getParentIds().isEmpty()).isFalse();

    int numOfAccess = 7;
    for (int i = 0; i < numOfAccess; i++) {
      configService.downloadForActivity(randomAppId, configFileId, activity.getUuid());
    }
    List<SecretUsageLog> usageLogs =
        (List<SecretUsageLog>) secretManagementResource
            .getUsageLogs(aPageRequest().build(), randomAccountId, encryptedUuid, SettingVariableTypes.CONFIG_FILE)
            .getResource();
    assertThat(usageLogs).hasSize(numOfAccess);

    for (SecretUsageLog usageLog : usageLogs) {
      assertThat(usageLog.getWorkflowExecutionName()).isEqualTo(workflowName);
      assertThat(usageLog.getAccountId()).isEqualTo(randomAccountId);
      assertThat(usageLog.getEnvId()).isEqualTo(envId);
      assertThat(usageLog.getAppId()).isEqualTo(randomAppId);
    }

    List<SecretChangeLog> changeLogs =
        secretManager.getChangeLogs(randomAccountId, secretFileId, SettingVariableTypes.CONFIG_FILE);
    assertThat(changeLogs).hasSize(2);
    SecretChangeLog changeLog = changeLogs.get(0);
    assertThat(changeLog.getAccountId()).isEqualTo(randomAccountId);
    assertThat(changeLog.getEncryptedDataId()).isEqualTo(secretFileId);
    assertThat(changeLog.getUser().getName()).isEqualTo(userName);
    assertThat(changeLog.getUser().getEmail()).isEqualTo(userEmail);
    assertThat(changeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(changeLog.getDescription()).isEqualTo("Changed Name and File");

    changeLog = changeLogs.get(1);
    assertThat(changeLog.getAccountId()).isEqualTo(randomAccountId);
    assertThat(changeLog.getEncryptedDataId()).isEqualTo(secretFileId);
    assertThat(changeLog.getUser().getName()).isEqualTo(userName);
    assertThat(changeLog.getUser().getEmail()).isEqualTo(userEmail);
    assertThat(changeLog.getUser().getUuid()).isEqualTo(user.getUuid());
    assertThat(changeLog.getDescription()).isEqualTo("File uploaded");
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    String secretFileId = secretManager.saveFile(renameAccountId, kmsId, secretName, fileToSave.length(), null,
        new BoundedInputStream(new FileInputStream(fileToSave)));
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
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()));
    assertThat(
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).count())
        .isEqualTo(numOfEncryptedValsForKms + 1);

    List<EncryptedData> encryptedFileData = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                                .filter("type", SettingVariableTypes.CONFIG_FILE)
                                                .filter("accountId", renameAccountId)
                                                .asList();
    assertThat(encryptedFileData).hasSize(1);
    assertThat(encryptedFileData.get(0).getParentIds().isEmpty()).isFalse();
    // test update
    String newSecretName = UUID.randomUUID().toString();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManager.updateFile(renameAccountId, newSecretName, encryptedUuid, fileToUpdate.length(), null,
        new BoundedInputStream(new FileInputStream(fileToUpdate)));

    download = configService.download(renameAppId, configFileId);
    assertThat(FileUtils.readFileToString(download, Charset.defaultCharset()))
        .isEqualTo(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()));
    assertThat(
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, renameAccountId).count())
        .isEqualTo(numOfEncryptedValsForKms + 1);

    encryptedFileData = wingsPersistence.createQuery(EncryptedData.class)
                            .filter(EncryptedDataKeys.accountId, renameAccountId)
                            .filter(EncryptedDataKeys.type, SettingVariableTypes.CONFIG_FILE)
                            .asList();
    assertThat(encryptedFileData).hasSize(1);
    assertThat(encryptedFileData.get(0).getParentIds().isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void kmsExceptionTest() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setKmsArn("invalid krn");
    String toEncrypt = UUID.randomUUID().toString();
    try {
      delegateService.encrypt(accountId, toEncrypt.toCharArray(), kmsConfig);
      fail("should have been failed");
    } catch (SecretManagementDelegateException e) {
      assertThat(true).isTrue();
    }

    kmsConfig = getKmsConfig();
    try {
      delegateService.decrypt(EncryptedData.builder()
                                  .encryptionKey(UUID.randomUUID().toString())
                                  .encryptedValue(toEncrypt.toCharArray())
                                  .build(),
          kmsConfig);
      fail("should have been failed");
    } catch (SecretManagementDelegateException e) {
      assertThat(true).isTrue();
    }
  }

  @Test
  @Owner(developers = RAGHU)
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

    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(numOfSettingAttributes);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(1);

    List<EncryptedData> encryptedDatas = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                             .field("encryptionType")
                                             .notEqual(EncryptionType.KMS)
                                             .asList();
    assertThat(encryptedDatas).hasSize(1);
    EncryptedData encryptedData = encryptedDatas.get(0);
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
    assertThat(encryptedData.getAccountId()).isEqualTo(accountId);
    assertThat(encryptedData.isEnabled()).isTrue();
    assertThat(encryptedData.getKmsId()).isNotNull();
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
      encryptedDatas =
          wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).asList();
      if (i == numOfSettingAttributes - 1) {
        assertThat(encryptedDatas.isEmpty()).isTrue();
      } else {
        assertThat(encryptedDatas).hasSize(1);
        encryptedData = encryptedDatas.get(0);
        assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
        assertThat(encryptedData.getAccountId()).isEqualTo(accountId);
        assertThat(encryptedData.isEnabled()).isTrue();
        assertThat(encryptedData.getKmsId()).isNotNull();
        assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.APP_DYNAMICS);
        assertThat(encryptedData.getParentIds()).hasSize(numOfSettingAttributes - (i + 1));

        assertThat(encryptedData.getParentIds().contains(attributeId)).isFalse();
        assertThat(encryptedData.getParentIds()).isEqualTo(remainingAttrs);
      }
      i++;
    }
  }

  @Test
  @Owner(developers = RAGHU)
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

    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(numOfSettingAttributes);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(numOfEncryptedValsForKms + 1);

    List<EncryptedData> encryptedDatas = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                             .filter("encryptionType", EncryptionType.KMS)
                                             .asList();
    assertThat(encryptedDatas).hasSize(1);
    EncryptedData encryptedData = encryptedDatas.get(0);
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.KMS);
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
      encryptedDatas = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                           .filter("encryptionType", EncryptionType.KMS)
                           .asList();
      if (i == numOfSettingAttributes - 1) {
        assertThat(encryptedDatas.isEmpty()).isTrue();
      } else {
        assertThat(encryptedDatas).hasSize(1);
        encryptedData = encryptedDatas.get(0);
        assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.KMS);
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void reuseYamlPasswordNewEntityKmsEncryption() throws IllegalAccessException {
    KmsConfig fromConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, fromConfig);

    String password = "password";
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(appDynamicsConfig);

    wingsPersistence.save(settingAttribute);
    List<EncryptedData> encryptedDataList = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                                                .filter("type", SettingVariableTypes.APP_DYNAMICS)
                                                .asList();
    assertThat(encryptedDataList).hasSize(1);

    String yamlRef = secretManager.getEncryptedYamlRef(appDynamicsConfig);

    String randomPassword = UUID.randomUUID().toString();
    appDynamicsConfig = getAppDynamicsConfig(accountId, randomPassword, yamlRef);
    settingAttribute = getSettingAttribute(appDynamicsConfig);

    SettingAttribute attributeCopy = settingsService.save(settingAttribute);
    encryptedDataList = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority)
                            .filter("type", SettingVariableTypes.APP_DYNAMICS)
                            .asList();
    assertThat(encryptedDataList).hasSize(1);

    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, attributeCopy.getUuid());
    AppDynamicsConfig savedConfig = (AppDynamicsConfig) savedAttribute.getValue();
    encryptionService.decrypt(savedConfig, secretManager.getEncryptionDetails(savedConfig, workflowExecutionId, appId));
    assertThat(String.valueOf(savedConfig.getPassword())).isEqualTo(password);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void getUsageLogs() throws IllegalAccessException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsResource.saveKmsConfig(accountId, kmsConfig);

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
    assertThat(usageLogs).hasSize(1);
    assertThat(usageLogs.get(0).getWorkflowExecutionName()).isEqualTo(workflowName);
    assertThat(usageLogs.get(0).getAccountId()).isEqualTo(accountId);
    assertThat(usageLogs.get(0).getEnvId()).isEqualTo(envId);
    assertThat(usageLogs.get(0).getAppId()).isEqualTo(appId);

    secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId);
    secretManager.getEncryptionDetails(savedAttribute, appId, workflowExecutionId);
    usageLogs =
        (List<SecretUsageLog>) secretManagementResource
            .getUsageLogs(aPageRequest().build(), accountId, savedAttributeId, SettingVariableTypes.SERVICE_VARIABLE)
            .getResource();
    assertThat(usageLogs).hasSize(3);

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
    assertThat(changeLogs).hasSize(numOfUpdates + 1);
    for (int i = 0; i <= numOfUpdates; i++) {
      SecretChangeLog secretChangeLog = changeLogs.get(i);
      assertThat(secretChangeLog.getUser().getUuid()).isEqualTo(user.getUuid());
      assertThat(secretChangeLog.getUser().getEmail()).isEqualTo(user.getEmail());
      assertThat(secretChangeLog.getUser().getName()).isEqualTo(user.getName());

      if (i == numOfUpdates) {
        assertThat(secretChangeLog.getDescription()).isEqualTo("Created");
      } else {
        assertThat(secretChangeLog.getDescription()).isEqualTo("Changed password");
      }
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void kms_Crud_shouldGenerate_Audit() {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);

    String secretManagerId = kmsService.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(eq(accountId), eq(null), any(KmsConfig.class), eq(Event.Type.CREATE));

    kmsConfig.setUuid(secretManagerId);
    kmsConfig.setDefault(false);
    kmsConfig.setName(kmsConfig.getName() + "_Updated");
    kmsService.saveKmsConfig(accountId, KryoUtils.clone(kmsConfig));
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(
            eq(accountId), any(KmsConfig.class), any(KmsConfig.class), eq(Event.Type.UPDATE));

    kmsService.deleteKmsConfig(accountId, secretManagerId);
    verify(auditServiceHelper).reportDeleteForAuditingUsingAccountId(eq(accountId), any(KmsConfig.class));
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
