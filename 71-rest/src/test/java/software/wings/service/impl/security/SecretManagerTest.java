package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ServiceVariable.ENCRYPTED_VALUE_KEY;
import static software.wings.service.impl.security.SecretManagerImpl.ENCRYPTED_FIELD_MASK;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.SECRET_TEXT;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import software.wings.beans.AwsConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.security.encryption.setupusage.SecretSetupUsageService;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.GcpKmsService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.UsageRestrictions;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SecretManagerTest extends CategoryTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private UsageRestrictionsService usageRestrictionsService;
  @Mock private AppService appService;
  @Mock private EnvironmentService envService;
  @Mock private UserService userService;
  @Mock private VaultService vaultService;
  @Mock private AuditServiceHelper auditServiceHelper;
  @Mock private SecretManagerConfigService secretManagerConfigService;
  @Mock private LocalEncryptionService localEncryptionService;
  @Mock private KmsService kmsService;
  @Mock private GcpSecretsManagerService gcpSecretsManagerService;
  @Mock private GcpKmsService gcpKmsService;
  @Mock private GlobalEncryptDecryptClient globalEncryptDecryptClient;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private SecretSetupUsageService secretSetupUsageService;
  @Inject @InjectMocks private SecretManagerImpl secretManager;

  @Rule public ExpectedException expectedEx = ExpectedException.none();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    when(usageRestrictionsService.getRestrictionsAndAppEnvMapFromCache(anyString(), eq(Action.READ)))
        .thenReturn(mock(RestrictionsAndAppEnvMap.class));
    when(userService.isAccountAdmin(ACCOUNT_ID)).thenReturn(true);
    when(secretManagerConfigService.listSecretManagers(anyString(), anyBoolean()))
        .thenReturn(Lists.newArrayList(mock(SecretManagerConfig.class)));
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testMaskEncryptedFields() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build();
    secretManager.maskEncryptedFields(awsConfig);
    assertThat(awsConfig.getSecretKey()).isEqualTo(ENCRYPTED_FIELD_MASK.toCharArray());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testResetUnchangedEncryptedFields() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build();
    AwsConfig maskedAwsConfig = AwsConfig.builder()
                                    .accountId(ACCOUNT_ID)
                                    .accessKey(ACCESS_KEY)
                                    .secretKey(ENCRYPTED_FIELD_MASK.toCharArray())
                                    .build();
    secretManager.resetUnchangedEncryptedFields(awsConfig, maskedAwsConfig);
    assertThat(maskedAwsConfig.getSecretKey()).isEqualTo(SECRET_KEY);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testValidateThatSecretManagerSupportsText() {
    expectedEx.expect(InvalidRequestException.class);
    expectedEx.expectMessage(
        "Secret values is not supported for the secretManager with id secretManagerId, type CYBERARK");
    CyberArkConfig cyberArkSecret = CyberArkConfig.builder().name("cyberArk").build();
    when(secretManagerConfigService.getSecretManager("accountId", "secretManagerId")).thenReturn(cyberArkSecret);
    secretManager.validateThatSecretManagerSupportsText("accountId", "secretManagerId");
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testListSecrets_withEmptyResponse() throws Exception {
    String accountId = UUIDGenerator.generateUuid();
    int pageSize = 40;
    int offset = 30;
    PageRequest<EncryptedData> pageRequest = getPageRequest(offset, pageSize);

    PageResponse<EncryptedData> pageResponse = mock(PageResponse.class);
    when(pageResponse.getResponse()).thenReturn(getSecretList(pageSize));
    when(wingsPersistence.query(eq(EncryptedData.class), any(PageRequest.class))).thenReturn(pageResponse);

    PageResponse<EncryptedData> finalPageResponse =
        secretManager.listSecrets(accountId, pageRequest, null, null, false);
    assertThat(finalPageResponse.getStart()).isEqualTo(offset + pageSize);
    assertThat(finalPageResponse.getPageSize()).isEqualTo(pageSize);
    assertThat(finalPageResponse.getTotal().longValue()).isEqualTo(0);

    verify(wingsPersistence, times(1)).query(eq(EncryptedData.class), any(PageRequest.class));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testListSecrets_withLargePageSize_multipleBatches() throws Exception {
    String accountId = UUIDGenerator.generateUuid();
    int pageSize = 10000;
    int offset = 0;
    PageRequest<EncryptedData> pageRequest = getPageRequest(offset, pageSize);

    PageResponse<EncryptedData> pageResponse = mock(PageResponse.class);
    when(pageResponse.getResponse())
        .thenReturn(getSecretList(PageRequest.DEFAULT_UNLIMITED), getSecretList(PageRequest.DEFAULT_UNLIMITED),
            getSecretList(3));
    when(wingsPersistence.query(eq(EncryptedData.class), any(PageRequest.class))).thenReturn(pageResponse);
    when(usageRestrictionsService.hasAccess(anyString(), anyBoolean(), anyString(), anyString(),
             any(UsageRestrictions.class), any(UsageRestrictions.class), anyMap(), anyMap()))
        .thenReturn(true);

    PageResponse<EncryptedData> finalPageResponse =
        secretManager.listSecrets(accountId, pageRequest, null, null, false);
    assertThat(finalPageResponse.getStart()).isEqualTo(2 * PageRequest.DEFAULT_UNLIMITED + 3);
    assertThat(finalPageResponse.getPageSize()).isEqualTo(pageSize);
    assertThat(finalPageResponse.getTotal().longValue()).isEqualTo(2 * PageRequest.DEFAULT_UNLIMITED + 3);

    verify(wingsPersistence, times(3)).query(eq(EncryptedData.class), any(PageRequest.class));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testListSecrets_withFullResponse_singleBatch() throws Exception {
    String accountId = UUIDGenerator.generateUuid();
    int pageSize = 40;
    int offset = 30;
    PageRequest<EncryptedData> pageRequest = getPageRequest(offset, pageSize);

    PageResponse<EncryptedData> pageResponse = mock(PageResponse.class);
    when(pageResponse.getResponse()).thenReturn(getSecretList(pageSize * 2));
    when(wingsPersistence.query(eq(EncryptedData.class), any(PageRequest.class))).thenReturn(pageResponse);
    when(usageRestrictionsService.hasAccess(anyString(), anyBoolean(), anyString(), anyString(),
             any(UsageRestrictions.class), any(UsageRestrictions.class), anyMap(), anyMap()))
        .thenReturn(false, false, false, true);

    PageResponse<EncryptedData> finalPageResponse =
        secretManager.listSecrets(accountId, pageRequest, null, null, false);
    assertThat(finalPageResponse.getStart()).isEqualTo(offset + pageSize + 3);
    assertThat(finalPageResponse.getPageSize()).isEqualTo(pageSize);
    assertThat(finalPageResponse.getTotal().longValue()).isEqualTo(pageSize);

    verify(wingsPersistence, times(1)).query(eq(EncryptedData.class), any(PageRequest.class));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testListSecrets_withFullResponse_multiBatches() throws Exception {
    String accountId = UUIDGenerator.generateUuid();
    int pageSize = 40;
    int offset = 30;
    PageRequest<EncryptedData> pageRequest = getPageRequest(offset, pageSize);

    PageResponse<EncryptedData> pageResponse = mock(PageResponse.class);
    when(pageResponse.getResponse())
        .thenReturn(getSecretList(pageSize * 2), getSecretList(pageSize * 2), getSecretList(pageSize));
    when(wingsPersistence.query(eq(EncryptedData.class), any(PageRequest.class))).thenReturn(pageResponse);

    // Filter out the first 3 records based on usage restriction.
    when(usageRestrictionsService.hasAccess(anyString(), anyBoolean(), anyString(), anyString(),
             any(UsageRestrictions.class), any(UsageRestrictions.class), anyMap(), anyMap()))
        .thenReturn(true, true, true, false);

    PageResponse<EncryptedData> finalPageResponse =
        secretManager.listSecrets(accountId, pageRequest, null, null, false);
    verify(wingsPersistence, times(3)).query(eq(EncryptedData.class), any(PageRequest.class));
    assertThat(finalPageResponse.getPageSize()).isEqualTo(pageSize);
    assertThat(finalPageResponse.getTotal().longValue()).isEqualTo(3);
    assertThat(finalPageResponse.getStart()).isEqualTo(offset + 5 * pageSize);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_Yaml_VaultPath_conversion() throws Exception {
    String encryptedDataId = UUIDGenerator.generateUuid();
    String accountId = UUIDGenerator.generateUuid();
    String entityId = UUIDGenerator.generateUuid();
    String kmsId = UUIDGenerator.generateUuid();
    String vaultConfigName = "TestVault";
    String vaultPath = "/foo/bar/MySecret#MyKey";
    String secretValue = "MySecretValue";

    EncryptedData encryptedData = mock(EncryptedData.class);
    VaultConfig vaultConfig = mock(VaultConfig.class);

    when(encryptedData.getEncryptionType()).thenReturn(EncryptionType.VAULT);
    when(encryptedData.getAccountId()).thenReturn(accountId);
    when(encryptedData.getKmsId()).thenReturn(kmsId);
    when(encryptedData.getPath()).thenReturn(vaultPath);

    when(vaultService.getVaultConfig(eq(accountId), eq(kmsId))).thenReturn(vaultConfig);
    when(vaultService.getVaultConfigByName(eq(accountId), eq(vaultConfigName))).thenReturn(vaultConfig);
    when(vaultService.decrypt(any(EncryptedData.class), eq(accountId), eq(vaultConfig)))
        .thenReturn(secretValue.toCharArray());
    when(vaultConfig.getName()).thenReturn(vaultConfigName);

    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(accountId)
                                          .entityType(EntityType.SERVICE)
                                          .name("SecretTextFromVaultPath")
                                          .encryptedValue(encryptedDataId)
                                          .value(secretValue.toCharArray())
                                          .entityId(entityId)
                                          .type(Type.ENCRYPTED_TEXT)
                                          .overrideType(OverrideType.ALL)
                                          .build();

    when(wingsPersistence.save(any(EncryptedData.class))).thenReturn(encryptedDataId);
    when(wingsPersistence.get(eq(EncryptedData.class), eq(encryptedDataId))).thenReturn(encryptedData);

    String yamlRef = secretManager.getEncryptedYamlRef(serviceVariable);
    assertThat(yamlRef.startsWith(EncryptionType.VAULT.getYamlName())).isTrue();
    assertThat(yamlRef.contains(vaultConfigName)).isTrue();
    assertThat(yamlRef.contains(vaultPath)).isTrue();

    QueryImpl<EncryptedData> query = mock(QueryImpl.class);
    FieldEnd fieldEnd = mock(FieldEnd.class);
    when(wingsPersistence.createQuery(eq(EncryptedData.class))).thenReturn(query);
    when(query.criteria(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.equal(anyObject())).thenReturn(query);
    when(query.get()).thenReturn(null);

    doNothing().when(auditServiceHelper).reportForAuditingUsingAccountId(anyString(), any(), any(), any());
    EncryptedData encryptedDataFromYaml = secretManager.getEncryptedDataFromYamlRef(yamlRef, accountId);
    assertThat(encryptedDataFromYaml).isNotNull();
    verify(wingsPersistence, times(1)).save(any(EncryptedData.class));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetSecretManager() {
    when(localEncryptionService.getEncryptionConfig(anyString()))
        .thenReturn(LocalEncryptionConfig.builder().uuid(ACCOUNT_ID).build());

    // The following 2 cases getSecretManager() call should return LOCAL encryption config
    SecretManagerConfig secretManagerConfig = secretManager.getSecretManager(ACCOUNT_ID, null, EncryptionType.LOCAL);
    assertThat(secretManagerConfig).isNotNull();
    assertThat(secretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);

    secretManagerConfig = secretManager.getSecretManager(ACCOUNT_ID, ACCOUNT_ID, EncryptionType.LOCAL);
    assertThat(secretManagerConfig).isNotNull();
    assertThat(secretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);

    // When default secret manager is configured...
    String kmsId = UUIDGenerator.generateUuid();
    when(secretManagerConfigService.getDefaultSecretManager(eq(ACCOUNT_ID))).thenReturn(KmsConfig.builder().build());
    when(secretManagerConfigService.getSecretManager(eq(ACCOUNT_ID), eq(kmsId)))
        .thenReturn(KmsConfig.builder().build());

    // The following 2 cases getSecretManager() call should return KMS encryption config
    secretManagerConfig = secretManager.getSecretManager(ACCOUNT_ID, null, EncryptionType.KMS);
    assertThat(secretManagerConfig).isNotNull();
    assertThat(secretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.KMS);

    secretManagerConfig = secretManager.getSecretManager(ACCOUNT_ID, kmsId, EncryptionType.KMS);
    assertThat(secretManagerConfig).isNotNull();
    assertThat(secretManagerConfig.getEncryptionType()).isEqualTo(EncryptionType.KMS);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void encryptedDataDetails() {
    EncryptedData mockEncryptedData = mock(EncryptedData.class);
    when(mockEncryptedData.getEncryptionType()).thenReturn(EncryptionType.KMS);
    String kmsId = UUIDGenerator.generateUuid();
    when(mockEncryptedData.getKmsId()).thenReturn(kmsId);
    KmsConfig kmsConfig = mock(KmsConfig.class);
    when(kmsConfig.isGlobalKms()).thenReturn(true);
    when(secretManagerConfigService.getSecretManager(ACCOUNT_ID, kmsId)).thenReturn(kmsConfig);
    String refId = UUIDGenerator.generateUuid();
    when(wingsPersistence.get(EncryptedData.class, refId)).thenReturn(mockEncryptedData);

    Optional<EncryptedDataDetail> encryptedDataDetails =
        secretManager.encryptedDataDetails(ACCOUNT_ID, "password", refId);
    assertThat(encryptedDataDetails.get()).isNotNull();
    assertThat(encryptedDataDetails.get().getEncryptionConfig()).isEqualTo(kmsConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void getEncryptionDetails() {
    JenkinsConfig jenkinsConfig = getJenkinsConfig(ACCOUNT_ID);
    EncryptedRecordData encryptedRecordData = EncryptedRecordData.builder().encryptionType(EncryptionType.KMS).build();
    String kmsId = UUIDGenerator.generateUuid();
    EncryptedData mockEncryptedData = EncryptedData.builder().kmsId(kmsId).encryptionType(EncryptionType.KMS).build();
    KmsConfig kmsConfig = mock(KmsConfig.class);
    when(kmsConfig.isGlobalKms()).thenReturn(true);
    when(kmsConfig.getEncryptionType()).thenReturn(EncryptionType.KMS);
    when(secretManagerConfigService.getSecretManager(ACCOUNT_ID, kmsId)).thenReturn(kmsConfig);
    when(wingsPersistence.get(EncryptedData.class, jenkinsConfig.getEncryptedPassword())).thenReturn(mockEncryptedData);
    when(globalEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(
             any(EncryptedData.class), any(String.class), any(EncryptionConfig.class)))
        .thenReturn(encryptedRecordData);
    when(localEncryptionService.getEncryptionConfig(ACCOUNT_ID))
        .thenReturn(LocalEncryptionConfig.builder().uuid(ACCOUNT_ID).build());
    when(secretManagerConfigService.getDefaultSecretManager(ACCOUNT_ID)).thenReturn(mock(KmsConfig.class));

    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(jenkinsConfig);
    assertThat(encryptedDataDetails.size()).isEqualTo(1);
    assertThat(encryptedDataDetails.get(0).getEncryptedData()).isEqualTo(encryptedRecordData);
    assertThat(encryptedDataDetails.get(0).getEncryptionConfig().getEncryptionType()).isEqualTo(EncryptionType.KMS);
  }

  private JenkinsConfig getJenkinsConfig(String accountId) {
    return JenkinsConfig.builder()
        .accountId(accountId)
        .jenkinsUrl(UUID.randomUUID().toString())
        .username(UUID.randomUUID().toString())
        .encryptedPassword(UUID.randomUUID().toString())
        .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
        .build();
  }

  private List<EncryptedData> getSecretList(int num) {
    List<EncryptedData> secretList = new ArrayList<>();
    for (int i = 0; i < num; i++) {
      secretList.add(new EncryptedData());
    }
    return secretList;
  }

  private PageRequest<EncryptedData> getPageRequest(int offset, int pageSize) {
    return PageRequestBuilder.aPageRequest()
        .withLimit(String.valueOf(pageSize))
        .withOffset(String.valueOf(offset))
        .build();
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testImportSecretsViaFile() {
    try {
      InputStream inputStream =
          new FileInputStream(getClass().getClassLoader().getResource("testProxyconfig.properties").getFile());
      String file = getClass().getClassLoader().getResource("encryption/secrets.csv").getFile();
      inputStream = new FileInputStream(file);

      EncryptedData encryptedData = mock(EncryptedData.class);
      when(secretManagerConfigService.getEncryptionType(anyString())).thenReturn(EncryptionType.LOCAL);

      when(localEncryptionService.encrypt(any(char[].class), anyString(), any(LocalEncryptionConfig.class)))
          .thenReturn(encryptedData);

      when(localEncryptionService.getEncryptionConfig(anyString())).thenReturn(mock(LocalEncryptionConfig.class));
      when(wingsPersistence.save(any(EncryptedData.class))).thenReturn("TEST");
      when(wingsPersistence.get(any(Class.class), anyString())).thenReturn(mock(EncryptedData.class));

      try {
        List<String> secrets = secretManager.importSecretsViaFile(ACCOUNT_ID, inputStream);
        assertThat(secrets.size()).isEqualTo(3);
        assertThat(secrets.get(0)).isEqualTo("TEST");
        assertThat(secrets.get(1)).isEqualTo("TEST");
        assertThat(secrets.get(2)).isEqualTo("TEST");
      } catch (SecretManagementException e) {
        fail(e.getMessage());
      }

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_deleteSecret_shouldFail() {
    String accountId = "accountId";
    String secretId = "secretId";
    EncryptedData encryptedData = mock(EncryptedData.class);
    UsageRestrictions usageRestrictions = mock(UsageRestrictions.class);
    when(encryptedData.getUuid()).thenReturn(secretId);
    when(encryptedData.getEncryptionType()).thenReturn(EncryptionType.AZURE_VAULT);
    when(encryptedData.getType()).thenReturn(SECRET_TEXT);
    when(encryptedData.getPath()).thenReturn("path");
    when(encryptedData.getUsageRestrictions()).thenReturn(usageRestrictions);

    when(wingsPersistence.get(EncryptedData.class, secretId)).thenReturn(encryptedData);
    when(usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, usageRestrictions)).thenReturn(true);
    when(featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)).thenReturn(true);
    when(secretSetupUsageService.getSecretUsage(accountId, secretId)).thenReturn(new HashSet<>());
    when(wingsPersistence.delete(EncryptedData.class, secretId)).thenReturn(false);

    boolean isDeleted = secretManager.deleteSecret(accountId, secretId);
    assertThat(isDeleted).isFalse();

    verify(wingsPersistence, times(1)).get(EncryptedData.class, secretId);
    verify(usageRestrictionsService, times(1)).userHasPermissionsToChangeEntity(accountId, usageRestrictions);
    verify(featureFlagService, times(1)).isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId);
    verify(secretSetupUsageService, times(1)).getSecretUsage(accountId, secretId);
    verify(wingsPersistence, times(1)).delete(EncryptedData.class, secretId);
    verify(auditServiceHelper, times(0)).reportDeleteForAuditingUsingAccountId(accountId, encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_deleteSecret_shouldFail_userRestrictions() {
    String accountId = "accountId";
    String secretId = "secretId";
    EncryptedData encryptedData = mock(EncryptedData.class);
    UsageRestrictions usageRestrictions = mock(UsageRestrictions.class);
    when(encryptedData.getUuid()).thenReturn(secretId);
    when(encryptedData.getEncryptionType()).thenReturn(EncryptionType.AZURE_VAULT);
    when(encryptedData.getType()).thenReturn(SECRET_TEXT);
    when(encryptedData.getPath()).thenReturn("path");
    when(encryptedData.getUsageRestrictions()).thenReturn(usageRestrictions);

    when(wingsPersistence.get(EncryptedData.class, secretId)).thenReturn(encryptedData);
    when(usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, usageRestrictions)).thenReturn(false);

    try {
      secretManager.deleteSecret(accountId, secretId);
      fail("Should not have succeeded");
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }

    verify(wingsPersistence, times(1)).get(EncryptedData.class, secretId);
    verify(usageRestrictionsService, times(1)).userHasPermissionsToChangeEntity(accountId, usageRestrictions);
    verify(featureFlagService, times(0)).isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId);
    verify(secretSetupUsageService, times(0)).getSecretUsage(accountId, secretId);
    verify(wingsPersistence, times(0)).delete(EncryptedData.class, secretId);
    verify(auditServiceHelper, times(0)).reportDeleteForAuditingUsingAccountId(accountId, encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_deleteSecret_shouldPass_featureFlagEnabled() {
    String accountId = "accountId";
    String secretId = "secretId";
    EncryptedData encryptedData = mock(EncryptedData.class);
    UsageRestrictions usageRestrictions = mock(UsageRestrictions.class);
    when(encryptedData.getUuid()).thenReturn(secretId);
    when(encryptedData.getEncryptionType()).thenReturn(EncryptionType.AZURE_VAULT);
    when(encryptedData.getType()).thenReturn(SECRET_TEXT);
    when(encryptedData.getPath()).thenReturn("path");
    when(encryptedData.getUsageRestrictions()).thenReturn(usageRestrictions);

    when(wingsPersistence.get(EncryptedData.class, secretId)).thenReturn(encryptedData);
    when(usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, usageRestrictions)).thenReturn(true);
    when(featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)).thenReturn(true);
    when(secretSetupUsageService.getSecretUsage(accountId, secretId)).thenReturn(new HashSet<>());
    when(wingsPersistence.delete(EncryptedData.class, secretId)).thenReturn(true);

    boolean isDeleted = secretManager.deleteSecret(accountId, secretId);
    assertThat(isDeleted).isTrue();

    verify(wingsPersistence, times(1)).get(EncryptedData.class, secretId);
    verify(usageRestrictionsService, times(1)).userHasPermissionsToChangeEntity(accountId, usageRestrictions);
    verify(featureFlagService, times(1)).isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId);
    verify(secretSetupUsageService, times(1)).getSecretUsage(accountId, secretId);
    verify(wingsPersistence, times(1)).delete(EncryptedData.class, secretId);
    verify(auditServiceHelper, times(1)).reportDeleteForAuditingUsingAccountId(accountId, encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_deleteSecret_shouldFail_featureFlagEnabled() {
    String accountId = "accountId";
    String secretId = "secretId";
    EncryptedData encryptedData = mock(EncryptedData.class);
    UsageRestrictions usageRestrictions = mock(UsageRestrictions.class);
    when(encryptedData.getUuid()).thenReturn(secretId);
    when(encryptedData.getEncryptionType()).thenReturn(EncryptionType.AZURE_VAULT);
    when(encryptedData.getType()).thenReturn(SECRET_TEXT);
    when(encryptedData.getPath()).thenReturn("path");
    when(encryptedData.getUsageRestrictions()).thenReturn(usageRestrictions);

    SecretSetupUsage secretSetupUsage = SecretSetupUsage.builder().entityId("uuid").type(AWS).build();
    when(wingsPersistence.get(EncryptedData.class, secretId)).thenReturn(encryptedData);
    when(usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, usageRestrictions)).thenReturn(true);
    when(featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)).thenReturn(true);
    when(secretSetupUsageService.getSecretUsage(accountId, secretId)).thenReturn(Sets.newHashSet(secretSetupUsage));

    try {
      secretManager.deleteSecret(accountId, secretId);
      fail("Should not have succeeded");
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }

    verify(wingsPersistence, times(1)).get(EncryptedData.class, secretId);
    verify(usageRestrictionsService, times(1)).userHasPermissionsToChangeEntity(accountId, usageRestrictions);
    verify(featureFlagService, times(1)).isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId);
    verify(secretSetupUsageService, times(1)).getSecretUsage(accountId, secretId);
    verify(wingsPersistence, times(0)).delete(EncryptedData.class, secretId);
    verify(auditServiceHelper, times(0)).reportDeleteForAuditingUsingAccountId(accountId, encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_deleteSecret_shouldPass_featureFlagDisabled() {
    String accountId = "accountId";
    String secretId = "secretId";
    EncryptedData encryptedData = mock(EncryptedData.class);
    UsageRestrictions usageRestrictions = mock(UsageRestrictions.class);
    when(encryptedData.getUuid()).thenReturn(secretId);
    when(encryptedData.getEncryptionType()).thenReturn(EncryptionType.AZURE_VAULT);
    when(encryptedData.getType()).thenReturn(SECRET_TEXT);
    when(encryptedData.getPath()).thenReturn("path");
    when(encryptedData.getUsageRestrictions()).thenReturn(usageRestrictions);

    Query<ServiceVariable> mockQuery = mock(Query.class);
    when(wingsPersistence.get(EncryptedData.class, secretId)).thenReturn(encryptedData);
    when(usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, usageRestrictions)).thenReturn(true);
    when(featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)).thenReturn(false);
    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(mockQuery);
    when(mockQuery.filter(ACCOUNT_ID_KEY, accountId)).thenReturn(mockQuery);
    when(mockQuery.filter(ENCRYPTED_VALUE_KEY, secretId)).thenReturn(mockQuery);
    when(mockQuery.asList()).thenReturn(new ArrayList<>());
    when(wingsPersistence.delete(EncryptedData.class, secretId)).thenReturn(true);

    boolean isDeleted = secretManager.deleteSecret(accountId, secretId);
    assertThat(isDeleted).isTrue();

    verify(wingsPersistence, times(1)).get(EncryptedData.class, secretId);
    verify(usageRestrictionsService, times(1)).userHasPermissionsToChangeEntity(accountId, usageRestrictions);
    verify(featureFlagService, times(1)).isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId);
    verify(wingsPersistence, times(1)).delete(EncryptedData.class, secretId);
    verify(auditServiceHelper, times(1)).reportDeleteForAuditingUsingAccountId(accountId, encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_deleteSecret_shouldFail_featureFlagDisabled() {
    String accountId = "accountId";
    String secretId = "secretId";
    EncryptedData encryptedData = mock(EncryptedData.class);
    UsageRestrictions usageRestrictions = mock(UsageRestrictions.class);
    when(encryptedData.getUuid()).thenReturn(secretId);
    when(encryptedData.getEncryptionType()).thenReturn(EncryptionType.AZURE_VAULT);
    when(encryptedData.getType()).thenReturn(SECRET_TEXT);
    when(encryptedData.getPath()).thenReturn("path");
    when(encryptedData.getUsageRestrictions()).thenReturn(usageRestrictions);

    Query<ServiceVariable> mockQuery = mock(Query.class);
    ServiceVariable mockServiceVariable = mock(ServiceVariable.class);
    when(wingsPersistence.get(EncryptedData.class, secretId)).thenReturn(encryptedData);
    when(usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, usageRestrictions)).thenReturn(true);
    when(featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)).thenReturn(false);
    when(wingsPersistence.createQuery(ServiceVariable.class)).thenReturn(mockQuery);
    when(mockQuery.filter(ACCOUNT_ID_KEY, accountId)).thenReturn(mockQuery);
    when(mockQuery.filter(ENCRYPTED_VALUE_KEY, secretId)).thenReturn(mockQuery);
    when(mockQuery.asList()).thenReturn(Collections.singletonList(mockServiceVariable));

    try {
      secretManager.deleteSecret(accountId, secretId);
      fail("Should not have succeeded");
    } catch (SecretManagementException e) {
      assertThat(e).isNotNull();
    }

    verify(wingsPersistence, times(1)).get(EncryptedData.class, secretId);
    verify(usageRestrictionsService, times(1)).userHasPermissionsToChangeEntity(accountId, usageRestrictions);
    verify(featureFlagService, times(1)).isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId);
    verify(wingsPersistence, times(0)).delete(EncryptedData.class, secretId);
    verify(auditServiceHelper, times(0)).reportDeleteForAuditingUsingAccountId(accountId, encryptedData);
  }
}
