package software.wings.service.impl.security;

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
import static software.wings.service.impl.security.SecretManagerImpl.ENCRYPTED_FIELD_MASK;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.QueryImpl;
import software.wings.beans.AwsConfig;
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
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.UsageRestrictions;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
  @Mock private FeatureFlagService featureFlagService;
  @Mock private KmsEncryptDecryptClient kmsEncryptDecryptClient;
  @Inject @InjectMocks private SecretManagerImpl secretManager;

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
  @Category(UnitTests.class)
  public void testMaskEncryptedFields() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).accessKey(ACCESS_KEY).secretKey(SECRET_KEY).build();
    secretManager.maskEncryptedFields(awsConfig);
    assertThat(awsConfig.getSecretKey()).isEqualTo(ENCRYPTED_FIELD_MASK.toCharArray());
  }

  @Test
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
  @Category(UnitTests.class)
  public void getGetEncryptedDataDetails() {
    when(featureFlagService.isEnabled(FeatureName.GLOBAL_KMS_PRE_PROCESSING, ACCOUNT_ID)).thenReturn(true);
    JenkinsConfig jenkinsConfig = getJenkinsConfig(ACCOUNT_ID);
    EncryptedRecordData mockEncryptedRecordData = mock(EncryptedRecordData.class);
    EncryptedData mockEncryptedData = mock(EncryptedData.class);
    when(mockEncryptedData.getEncryptionType()).thenReturn(EncryptionType.KMS);
    String kmsId = UUIDGenerator.generateUuid();
    when(mockEncryptedData.getKmsId()).thenReturn(kmsId);
    KmsConfig kmsConfig = mock(KmsConfig.class);
    when(kmsConfig.isGlobalKms()).thenReturn(true);
    when(secretManagerConfigService.getSecretManager(ACCOUNT_ID, kmsId)).thenReturn(kmsConfig);
    when(wingsPersistence.get(EncryptedData.class, jenkinsConfig.getEncryptedPassword())).thenReturn(mockEncryptedData);
    when(kmsEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(
             any(EncryptedData.class), any(KmsConfig.class)))
        .thenReturn(mockEncryptedRecordData);
    when(localEncryptionService.getEncryptionConfig(ACCOUNT_ID))
        .thenReturn(LocalEncryptionConfig.builder().uuid(ACCOUNT_ID).build());
    when(secretManagerConfigService.getDefaultSecretManager(ACCOUNT_ID)).thenReturn(mock(KmsConfig.class));

    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(jenkinsConfig);
    assertThat(encryptedDataDetails.size()).isEqualTo(1);
    assertThat(encryptedDataDetails.get(0).getEncryptedData()).isEqualTo(mockEncryptedRecordData);
    assertThat(encryptedDataDetails.get(0).getEncryptionConfig().getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
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
}
