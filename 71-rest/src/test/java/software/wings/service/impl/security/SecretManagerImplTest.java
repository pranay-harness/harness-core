package software.wings.service.impl.security;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_NAME;

import com.google.common.collect.Sets;
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
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.ServiceVariableKeys;
import software.wings.beans.User;
import software.wings.security.GenericEntityFilter;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.GcpKmsService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SecretManagerImplTest extends WingsBaseTest {
  private Account account;
  private GcpKmsConfig gcpKmsConfig;
  @Mock private GcpKmsService gcpKmsService;
  @Mock private AccountService accountService;
  @Mock private HarnessUserGroupService harnessUserGroupService;
  @Mock private FileService fileService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Inject @InjectMocks private KmsService kmsService;
  @Inject @InjectMocks private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject @InjectMocks private SecretManager secretManager;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  private String secretName = "secretName";
  private String secretValue = "secretValue";

  @Before
  public void setup() {
    account = getAccount(AccountType.PAID);
    account.setLocalEncryptionEnabled(false);
    wingsPersistence.save(account);
    List<Account> accounts = new ArrayList<>();
    accounts.add(account);
    User user = User.Builder.anUser()
                    .uuid("uuid")
                    .name("Hello")
                    .uuid(generateUuid())
                    .email("hello@harness.io")
                    .accounts(accounts)
                    .build();
    UserThreadLocal.set(user);

    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    gcpKmsConfig = new GcpKmsConfig("name", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(account.getUuid());
    gcpKmsConfig.setDefault(true);

    when(accountService.get(account.getUuid())).thenReturn(account);
    when(accountService.get(GLOBAL_ACCOUNT_ID)).thenReturn(account);
    when(harnessUserGroupService.isHarnessSupportUser(user.getUuid())).thenReturn(true);

    when(gcpKmsService.encrypt(any(), eq(account.getUuid()), eq(gcpKmsConfig), eq(null))).thenReturn(null);
    String result = gcpSecretsManagerService.saveGcpKmsConfig(account.getUuid(), gcpKmsConfig);
    assertThat(result).isNotNull();
    gcpKmsConfig.setUuid(result);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncrypt_GCPKMS() {
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
        account.getUuid(), SettingVariableTypes.GCP_KMS, secretValue.toCharArray(), null, null, null, secretName, null);
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

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testTransitionSecrets_FromGlobalAccount() {
    char[] credentials = "{\"credentials\":\"abc\"}".toCharArray();

    GcpKmsConfig gcpKmsConfig = new GcpKmsConfig("name1", "projectId", "region", "keyRing", "keyName", credentials);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setAccountId(GLOBAL_ACCOUNT_ID);
    gcpKmsConfig.setDefault(true);

    String result = gcpSecretsManagerService.saveGcpKmsConfig(GLOBAL_ACCOUNT_ID, gcpKmsConfig);
    assertThat(result).isNotNull();
    gcpKmsConfig.setUuid(result);

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

    boolean transitionEventsCreated = secretManager.transitionSecrets(
        account.getUuid(), EncryptionType.GCP_KMS, gcpKmsConfig.getUuid(), EncryptionType.KMS, "kmsConfigId");

    assertThat(transitionEventsCreated).isTrue();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_saveSecret() {
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
    SecretText secretText =
        SecretText.builder().name(secretName).kmsId(gcpKmsConfig.getUuid()).value(secretValue).build();
    String secretId = secretManager.saveSecret(account.getUuid(), secretText);
    assertThat(secretId).isNotNull();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_saveSecretLocal() {
    Account newAccount = getAccount(AccountType.PAID);
    newAccount.setLocalEncryptionEnabled(true);
    String accountId = wingsPersistence.save(newAccount);
    SecretText secretText = SecretText.builder().name(secretName).value(secretValue).build();
    String secretId = secretManager.saveSecretUsingLocalMode(accountId, secretText);
    assertThat(secretId).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_canUseSecretsInAppAndEnv() {
    String appId1 = "appId1";
    String envId1 = "envId1";
    String appId2 = "appId2";
    String envId2 = "envId2";
    String appId3 = "appId3";
    String envId3 = "envId3";

    AppEnvRestriction appEnvRestriction1 =
        usageRestrictionsService.getDefaultUsageRestrictions(account.getUuid(), appId1, envId1)
            .getAppEnvRestrictions()
            .iterator()
            .next();
    AppEnvRestriction appEnvRestriction2 =
        usageRestrictionsService.getDefaultUsageRestrictions(account.getUuid(), appId2, envId2)
            .getAppEnvRestrictions()
            .iterator()
            .next();
    AppEnvRestriction appEnvRestriction3 =
        usageRestrictionsService.getDefaultUsageRestrictions(account.getUuid(), appId3, envId3)
            .getAppEnvRestrictions()
            .iterator()
            .next();

    UsageRestrictions usageRestrictions1 =
        UsageRestrictions.builder().appEnvRestrictions(Sets.newHashSet(appEnvRestriction1, appEnvRestriction2)).build();
    UsageRestrictions usageRestrictions2 =
        UsageRestrictions.builder().appEnvRestrictions(Sets.newHashSet(appEnvRestriction2, appEnvRestriction3)).build();

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.AWS)
                                      .usageRestrictions(usageRestrictions1)
                                      .build();

    String encryptedDataId1 = wingsPersistence.save(encryptedData);
    encryptedData.setUuid(null);
    encryptedData.setName(UUIDGenerator.generateUuid());
    encryptedData.setUsageRestrictions(usageRestrictions2);
    String encryptedDataId2 = wingsPersistence.save(encryptedData);

    Environment environment1 = mock(Environment.class);
    when(environment1.getUuid()).thenReturn(envId1);
    Environment environment2 = mock(Environment.class);
    when(environment2.getUuid()).thenReturn(envId2);
    Environment environment3 = mock(Environment.class);
    when(environment3.getUuid()).thenReturn(envId3);

    UsageRestrictions userUsageRestrictions = mock(UsageRestrictions.class);
    Map<String, Set<String>> userAppEnvMap = new HashMap<>();
    Map<String, List<Base>> appIdEnvMapForAccount = new HashMap<>();
    appIdEnvMapForAccount.put(appId1, Collections.singletonList(environment1));
    appIdEnvMapForAccount.put(appId2, Collections.singletonList(environment1));
    appIdEnvMapForAccount.put(appId3, Collections.singletonList(environment1));

    boolean canUseSecrets = secretManager.canUseSecretsInAppAndEnv(
        Sets.newHashSet(encryptedDataId1, encryptedDataId2, UUIDGenerator.generateUuid()), account.getUuid(), appId2,
        envId2, false, userUsageRestrictions, userAppEnvMap, appIdEnvMapForAccount);
    assertThat(canUseSecrets).isTrue();

    canUseSecrets = secretManager.canUseSecretsInAppAndEnv(Sets.newHashSet(encryptedDataId1, encryptedDataId2),
        account.getUuid(), appId1, envId1, false, userUsageRestrictions, userAppEnvMap, appIdEnvMapForAccount);
    assertThat(canUseSecrets).isFalse();

    canUseSecrets = secretManager.canUseSecretsInAppAndEnv(Sets.newHashSet(encryptedDataId1, encryptedDataId2),
        account.getUuid(), appId3, envId3, false, userUsageRestrictions, userAppEnvMap, appIdEnvMapForAccount);
    assertThat(canUseSecrets).isFalse();

    canUseSecrets = secretManager.canUseSecretsInAppAndEnv(Sets.newHashSet(encryptedDataId1), account.getUuid(), appId1,
        envId1, false, userUsageRestrictions, userAppEnvMap, appIdEnvMapForAccount);
    assertThat(canUseSecrets).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_canUseSecretsInAppAndEnv_OnlyAppIdandEnvId() {
    String appId1 = "appId1";
    String envId1 = "envId1";
    String appId2 = "appId2";
    String envId2 = "envId2";
    String appId3 = "appId3";
    String envId3 = "envId3";

    AppEnvRestriction appEnvRestriction1 =
        usageRestrictionsService.getDefaultUsageRestrictions(account.getUuid(), appId1, envId1)
            .getAppEnvRestrictions()
            .iterator()
            .next();
    AppEnvRestriction appEnvRestriction2 =
        usageRestrictionsService.getDefaultUsageRestrictions(account.getUuid(), appId2, envId2)
            .getAppEnvRestrictions()
            .iterator()
            .next();
    AppEnvRestriction appEnvRestriction3 =
        usageRestrictionsService.getDefaultUsageRestrictions(account.getUuid(), appId3, envId3)
            .getAppEnvRestrictions()
            .iterator()
            .next();

    UsageRestrictions usageRestrictions1 =
        UsageRestrictions.builder().appEnvRestrictions(Sets.newHashSet(appEnvRestriction1, appEnvRestriction2)).build();
    UsageRestrictions usageRestrictions2 =
        UsageRestrictions.builder().appEnvRestrictions(Sets.newHashSet(appEnvRestriction2, appEnvRestriction3)).build();

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(account.getUuid())
                                      .enabled(true)
                                      .kmsId(gcpKmsConfig.getUuid())
                                      .encryptionType(EncryptionType.GCP_KMS)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.AWS)
                                      .usageRestrictions(usageRestrictions1)
                                      .build();

    String encryptedDataId1 = wingsPersistence.save(encryptedData);
    encryptedData.setUuid(null);
    encryptedData.setName(UUIDGenerator.generateUuid());
    encryptedData.setUsageRestrictions(usageRestrictions2);
    String encryptedDataId2 = wingsPersistence.save(encryptedData);

    Environment environment1 = mock(Environment.class);
    when(environment1.getUuid()).thenReturn(envId1);
    Environment environment2 = mock(Environment.class);
    when(environment2.getUuid()).thenReturn(envId2);
    Environment environment3 = mock(Environment.class);
    when(environment3.getUuid()).thenReturn(envId3);

    Map<String, List<Base>> appIdEnvMapForAccount = new HashMap<>();
    appIdEnvMapForAccount.put(appId1, Collections.singletonList(environment1));
    appIdEnvMapForAccount.put(appId2, Collections.singletonList(environment1));
    appIdEnvMapForAccount.put(appId3, Collections.singletonList(environment1));

    when(appService.getAppIdsByAccountId(account.getUuid())).thenReturn(Arrays.asList(appId1, appId2, appId3));
    when(environmentService.getAppIdEnvMap(any())).thenReturn(appIdEnvMapForAccount);

    boolean canUseSecrets = secretManager.canUseSecretsInAppAndEnv(
        Sets.newHashSet(encryptedDataId1, encryptedDataId2, UUIDGenerator.generateUuid()), account.getUuid(), appId2,
        envId2);
    assertThat(canUseSecrets).isTrue();

    canUseSecrets = secretManager.canUseSecretsInAppAndEnv(
        Sets.newHashSet(encryptedDataId1, encryptedDataId2), account.getUuid(), appId1, envId1);
    assertThat(canUseSecrets).isFalse();

    canUseSecrets = secretManager.canUseSecretsInAppAndEnv(
        Sets.newHashSet(encryptedDataId1, encryptedDataId2), account.getUuid(), appId3, envId3);
    assertThat(canUseSecrets).isFalse();

    canUseSecrets =
        secretManager.canUseSecretsInAppAndEnv(Sets.newHashSet(encryptedDataId1), account.getUuid(), appId1, envId1);
    assertThat(canUseSecrets).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_canUseSecretsInAppAndEnv_OnlyAppIdandEnvId_shouldReturnTrue() {
    String encryptedDataId1 = UUIDGenerator.generateUuid();
    String encryptedDataId2 = UUIDGenerator.generateUuid();

    boolean canUseSecrets = secretManager.canUseSecretsInAppAndEnv(
        Sets.newHashSet(encryptedDataId1, encryptedDataId2), account.getUuid(), "appId2", "envId2");
    assertThat(canUseSecrets).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_getSecretUsageForEncryptedText() {
    Account newAccount = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(newAccount);
    newAccount.setUuid(accountId);
    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(accountId)
                                      .enabled(true)
                                      .kmsId(accountId)
                                      .encryptionType(EncryptionType.LOCAL)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.SECRET_TEXT)
                                      .build();
    String secretId = wingsPersistence.save(encryptedData);
    encryptedData.setUuid(secretId);

    EncryptedData encryptedData1 = EncryptedData.builder()
                                       .accountId(accountId)
                                       .enabled(true)
                                       .kmsId(accountId)
                                       .encryptionType(EncryptionType.LOCAL)
                                       .encryptionKey("Dummy Key")
                                       .encryptedValue("Dummy Value".toCharArray())
                                       .base64Encoded(false)
                                       .name("Dummy record 1")
                                       .type(SettingVariableTypes.SECRET_TEXT)
                                       .build();
    String secretId1 = wingsPersistence.save(encryptedData1);
    encryptedData1.setUuid(secretId1);

    ServiceTemplate serviceTemplate = aServiceTemplate().build();
    String entityId = wingsPersistence.save(serviceTemplate);

    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(accountId)
                                          .envId(ENV_ID)
                                          .entityType(EntityType.SERVICE_TEMPLATE)
                                          .entityId(entityId)
                                          .templateId(entityId)
                                          .name(SERVICE_VARIABLE_NAME + "2")
                                          .type(ENCRYPTED_TEXT)
                                          .encryptedValue(secretId)
                                          .build();

    ServiceVariable serviceVariable1 = ServiceVariable.builder()
                                           .accountId(accountId)
                                           .envId(ENV_ID)
                                           .entityType(EntityType.SERVICE_TEMPLATE)
                                           .entityId(entityId)
                                           .entityId(entityId)
                                           .name(SERVICE_VARIABLE_NAME + "3")
                                           .type(ENCRYPTED_TEXT)
                                           .encryptedValue(secretId)
                                           .build();

    String serviceVariableId = wingsPersistence.save(serviceVariable);
    serviceVariable.setUuid(serviceVariableId);
    String serviceVariableId1 = wingsPersistence.save(serviceVariable1);
    serviceVariable1.setUuid(serviceVariableId1);

    encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    assertThat(encryptedData.getParents()).hasSize(2);
    encryptedData.addParent(new EncryptedDataParent(
        generateUuid(), SettingVariableTypes.SERVICE_VARIABLE, SettingVariableTypes.SERVICE_VARIABLE.toString()));
    wingsPersistence.save(encryptedData);

    encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    assertThat(encryptedData.getParents()).hasSize(3);

    wingsPersistence.updateField(ServiceVariable.class, serviceVariable1.getUuid(), ServiceVariableKeys.encryptedValue,
        encryptedData1.getUuid());

    encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    assertThat(encryptedData.getParents()).hasSize(2);

    encryptedData1 = wingsPersistence.get(EncryptedData.class, secretId1);
    assertThat(encryptedData1.getParents()).hasSize(1);

    Set<SecretSetupUsage> usages = secretManager.getSecretUsage(accountId, encryptedData.getUuid());
    assertThat(usages).isNotNull();
    assertThat(usages.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSaveEncryptedData_whenUsageRestrictionIsNotPresent() {
    Account newAccount = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(newAccount);
    newAccount.setUuid(accountId);
    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(accountId)
                                      .enabled(true)
                                      .kmsId(accountId)
                                      .encryptionType(EncryptionType.LOCAL)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.SECRET_TEXT)
                                      .build();
    String secretId = ((SecretManagerImpl) secretManager).saveEncryptedData(encryptedData);

    EncryptedData encryptedDataInDB = wingsPersistence.get(EncryptedData.class, secretId);
    assertThat(encryptedDataInDB).isNotNull();
    assertThat(encryptedDataInDB.isScopedToAccount()).isTrue();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSaveEncryptedData_whenUsageRestrictionIsPresent() {
    Account newAccount = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(newAccount);
    newAccount.setUuid(accountId);

    Set<AppEnvRestriction> appEnvRestrictions = new HashSet();
    appEnvRestrictions.add(
        AppEnvRestriction.builder()
            .appFilter(GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build())
            .build());
    UsageRestrictions usageRestrictions = UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictions).build();

    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(accountId)
                                      .enabled(true)
                                      .kmsId(accountId)
                                      .encryptionType(EncryptionType.LOCAL)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .type(SettingVariableTypes.SECRET_TEXT)
                                      .usageRestrictions(usageRestrictions)
                                      .build();
    String secretId = ((SecretManagerImpl) secretManager).saveEncryptedData(encryptedData);

    EncryptedData encryptedDataInDB = wingsPersistence.get(EncryptedData.class, secretId);
    assertThat(encryptedDataInDB).isNotNull();
    assertThat(encryptedDataInDB.isScopedToAccount()).isFalse();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testSaveEncryptedData_whenUsageRestrictionIsEdited() {
    Account newAccount = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(newAccount);
    newAccount.setUuid(accountId);
    EncryptedData encryptedData = EncryptedData.builder()
                                      .accountId(accountId)
                                      .enabled(true)
                                      .kmsId(accountId)
                                      .encryptionType(EncryptionType.LOCAL)
                                      .encryptionKey("Dummy Key")
                                      .encryptedValue("Dummy Value".toCharArray())
                                      .base64Encoded(false)
                                      .name("Dummy record")
                                      .scopedToAccount(true)
                                      .type(SettingVariableTypes.SECRET_TEXT)
                                      .build();

    String secretId = ((SecretManagerImpl) secretManager).saveEncryptedData(encryptedData);

    EncryptedData encryptedDataInDB = wingsPersistence.get(EncryptedData.class, secretId);
    assertThat(encryptedDataInDB).isNotNull();
    assertThat(encryptedDataInDB.isScopedToAccount()).isTrue();

    Set<AppEnvRestriction> appEnvRestrictions = new HashSet();
    appEnvRestrictions.add(
        AppEnvRestriction.builder()
            .appFilter(GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build())
            .build());
    UsageRestrictions usageRestrictions = UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictions).build();
    encryptedDataInDB.setUsageRestrictions(usageRestrictions);

    String editedSecretId = ((SecretManagerImpl) secretManager).saveEncryptedData(encryptedDataInDB);

    EncryptedData editedEncryptedDataInDB = wingsPersistence.get(EncryptedData.class, editedSecretId);
    assertThat(editedEncryptedDataInDB).isNotNull();
    assertThat(editedEncryptedDataInDB.isScopedToAccount()).isFalse();
    assertThat(editedSecretId).isEqualTo(secretId);
  }
}
