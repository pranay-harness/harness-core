package software.wings;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.queue.QueueConsumer;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Account;
import software.wings.beans.Account.Builder;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureFlagKeys;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.VaultConfig;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;
import software.wings.dl.WingsPersistence;
import software.wings.resources.SecretManagementResource;
import software.wings.rules.WingsRule;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by anubhaw on 4/28/16.
 */
public abstract class WingsBaseTest extends CategoryTest implements MockableTestMixin {
  private static final String plainTextKey = "1234567890123456";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  // I am not absolutely sure why, but there is dependency between wings io.harness.rule and
  // MockitoJUnit io.harness.rule and they have to be listed in these order
  @Rule public WingsRule wingsRule = new WingsRule();

  @Inject protected SecretManagementResource secretManagementResource;
  @Inject protected SecretManager secretManager;
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected ConfigService configService;
  @Inject protected EncryptionService encryptionService;
  @Inject protected QueueConsumer<KmsTransitionEvent> transitionKmsQueue;
  @Inject protected SettingsService settingsService;
  @Inject protected FeatureFlagService featureFlagService;

  protected EncryptedData encrypt(String accountId, char[] value, KmsConfig kmsConfig) throws Exception {
    if (kmsConfig.getAccessKey().equals("invalidKey")) {
      throw new SecretManagementException(
          ErrorCode.SECRET_MANAGEMENT_ERROR, "Invalid credentials", WingsException.USER);
    }
    char[] encryptedValue = value == null
        ? null
        : KmsEncryptDecryptClient.encrypt(new String(value), new SecretKeySpec(plainTextKey.getBytes(), "AES"));

    return EncryptedData.builder()
        .encryptionKey(plainTextKey)
        .encryptedValue(encryptedValue)
        .encryptionType(EncryptionType.KMS)
        .kmsId(kmsConfig.getUuid())
        .enabled(true)
        .accountId(accountId)
        .build();
  }

  protected char[] decrypt(EncryptedRecord data, KmsConfig kmsConfig) throws Exception {
    return KmsEncryptDecryptClient.decrypt(data.getEncryptedValue(), new SecretKeySpec(plainTextKey.getBytes(), "AES"))
        .toCharArray();
  }

  protected boolean validateCyberArkConfig(CyberArkConfig cyberArkConfig) {
    if (Objects.equals(cyberArkConfig.getCyberArkUrl(), "invalidUrl")) {
      throw new SecretManagementException("Invalid Url");
    }
    if (Objects.equals(cyberArkConfig.getClientCertificate(), "invalidCertificate")) {
      throw new SecretManagementException("Invalid credentials");
    }
    return true;
  }

  protected EncryptedData encrypt(String value, CyberArkConfig cyberArkConfig) throws Exception {
    if (cyberArkConfig.getClientCertificate().equals("invalidCertificate")) {
      throw new SecretManagementException(
          ErrorCode.SECRET_MANAGEMENT_ERROR, "Invalid credentials", WingsException.USER);
    }
    char[] encryptedValue = value == null
        ? null
        : KmsEncryptDecryptClient.encrypt(value, new SecretKeySpec(plainTextKey.getBytes(), "AES"));

    return EncryptedData.builder()
        .encryptionKey(plainTextKey)
        .encryptedValue(encryptedValue)
        .encryptionType(EncryptionType.CYBERARK)
        .kmsId(cyberArkConfig.getUuid())
        .enabled(true)
        .accountId(cyberArkConfig.getAccountId())
        .build();
  }

  protected char[] decrypt(EncryptedRecord data, CyberArkConfig cyberArkConfig) throws Exception {
    return "Cyberark1".toCharArray();
  }

  protected EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData savedEncryptedData) throws IOException {
    if (vaultConfig.getAuthToken().equals("invalidKey")) {
      throw new SecretManagementException("invalidKey");
    }
    String keyUrl = settingType + "/" + name;
    if (savedEncryptedData != null) {
      savedEncryptedData.setEncryptionKey(keyUrl);
      savedEncryptedData.setEncryptedValue(value == null ? keyUrl.toCharArray() : value.toCharArray());
      return savedEncryptedData;
    }

    return EncryptedData.builder()
        .encryptionKey(keyUrl)
        .encryptedValue(value == null ? null : value.toCharArray())
        .encryptionType(EncryptionType.VAULT)
        .enabled(true)
        .accountId(accountId)
        .kmsId(vaultConfig.getUuid())
        .build();
  }

  protected char[] decrypt(EncryptedRecord data, VaultConfig vaultConfig) throws IOException {
    if (data.getEncryptedValue() == null) {
      return null;
    }
    return data.getEncryptedValue();
  }

  private VaultConfig getCommonVaultConfig() {
    VaultConfig vaultConfig =
        VaultConfig.builder().vaultUrl("http://127.0.0.1:8200").name("myVault").secretEngineVersion(1).build();
    vaultConfig.setDefault(true);
    vaultConfig.setReadOnly(false);
    vaultConfig.setEncryptionType(EncryptionType.VAULT);
    return vaultConfig;
  }

  protected VaultConfig getVaultConfigWithAuthToken() {
    return getVaultConfigWithAuthToken(generateUuid());
  }

  protected VaultConfig getVaultConfigWithAuthToken(String authToken) {
    VaultConfig vaultConfig = getCommonVaultConfig();
    vaultConfig.setAuthToken(authToken);
    return vaultConfig;
  }

  protected VaultConfig getVaultConfigWithAppRole(String appRoleId, String secretId) {
    VaultConfig vaultConfig = getCommonVaultConfig();
    vaultConfig.setAppRoleId(appRoleId);
    vaultConfig.setSecretId(secretId);
    return vaultConfig;
  }

  protected KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setDefault(true);
    kmsConfig.setKmsArn(generateUuid());
    kmsConfig.setAccessKey(generateUuid());
    kmsConfig.setSecretKey(generateUuid());
    kmsConfig.setEncryptionType(EncryptionType.KMS);
    return kmsConfig;
  }

  protected GcpKmsConfig getGcpKmsConfig() {
    GcpKmsConfig gcpKmsConfig =
        new GcpKmsConfig("gcpKms", "projectId", "region", "keyRing", "keyName", "{\"abc\": \"value\"}".toCharArray());
    gcpKmsConfig.setDefault(true);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    return gcpKmsConfig;
  }

  protected CyberArkConfig getCyberArkConfig() {
    return getCyberArkConfig(null);
  }

  protected CyberArkConfig getCyberArkConfig(String clientCertificate) {
    final CyberArkConfig cyberArkConfig = new CyberArkConfig();
    cyberArkConfig.setName("myCyberArk");
    cyberArkConfig.setDefault(true);
    cyberArkConfig.setCyberArkUrl("https://app.harness.io"); // Just a valid URL.
    cyberArkConfig.setAppId(generateUuid());
    cyberArkConfig.setClientCertificate(clientCertificate);
    cyberArkConfig.setEncryptionType(EncryptionType.CYBERARK);
    return cyberArkConfig;
  }

  protected AzureVaultConfig getAzureVaultConfig() {
    AzureVaultConfig azureVaultConfig = new AzureVaultConfig();
    azureVaultConfig.setName("myAzureVault");
    azureVaultConfig.setSecretKey(generateUuid());
    azureVaultConfig.setDefault(true);
    azureVaultConfig.setVaultName(generateUuid());
    azureVaultConfig.setClientId(generateUuid());
    azureVaultConfig.setSubscription(generateUuid());
    azureVaultConfig.setTenantId(generateUuid());
    azureVaultConfig.setEncryptionType(EncryptionType.AZURE_VAULT);
    return azureVaultConfig;
  }

  protected AwsSecretsManagerConfig getAwsSecretManagerConfig() {
    AwsSecretsManagerConfig secretsManagerConfig = AwsSecretsManagerConfig.builder()
                                                       .name("myAwsSecretManager")
                                                       .accessKey(generateUuid())
                                                       .secretKey(generateUuid())
                                                       .region("us-east-1")
                                                       .secretNamePrefix(generateUuid())
                                                       .build();
    secretsManagerConfig.setDefault(true);
    secretsManagerConfig.setEncryptionType(EncryptionType.AWS_SECRETS_MANAGER);

    return secretsManagerConfig;
  }

  protected Account getAccount(String accountType) {
    Builder accountBuilder = Builder.anAccount().withUuid(generateUuid());
    LicenseInfo license = getLicenseInfo();
    license.setAccountType(accountType);
    accountBuilder.withLicenseInfo(license);

    return accountBuilder.build();
  }

  protected LicenseInfo getLicenseInfo() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setLicenseUnits(100);
    licenseInfo.setExpireAfterDays(15);
    return licenseInfo;
  }

  protected AppDynamicsConfig getAppDynamicsConfig(String accountId, String password) {
    return getAppDynamicsConfig(accountId, password, null);
  }

  protected AppDynamicsConfig getAppDynamicsConfig(String accountId, String password, String encryptedPassword) {
    char[] passwordChars = password == null ? null : password.toCharArray();
    return AppDynamicsConfig.builder()
        .accountId(accountId)
        .controllerUrl(UUID.randomUUID().toString())
        .username(UUID.randomUUID().toString())
        .password(passwordChars)
        .encryptedPassword(encryptedPassword)
        .accountname(UUID.randomUUID().toString())
        .build();
  }

  protected void validateSettingAttributes(
      List<SettingAttribute> settingAttributes, int expectedNumOfEncryptedRecords) {
    int numOfSettingAttributes = settingAttributes.size();
    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(numOfSettingAttributes);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(expectedNumOfEncryptedRecords);

    for (int i = 0; i < numOfSettingAttributes; i++) {
      SettingAttribute settingAttribute = settingAttributes.get(i);
      String id = settingAttribute.getUuid();
      String appId = settingAttribute.getAppId();
      SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, id);
      assertThat(savedAttribute).isEqualTo(settingAttributes.get(i));
      AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttributes.get(i).getValue();
      assertThat(appDynamicsConfig.getPassword()).isNull();

      encryptionService.decrypt(appDynamicsConfig, secretManager.getEncryptionDetails(appDynamicsConfig, null, appId));
      assertThat(new String(appDynamicsConfig.getPassword())).isEqualTo("password_" + i);

      wingsPersistence.delete(SettingAttribute.class, settingAttribute.getAppId(), settingAttribute.getUuid());
    }

    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(0);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count())
        .isEqualTo(expectedNumOfEncryptedRecords - numOfSettingAttributes);
  }

  protected List<SettingAttribute> getSettingAttributes(String accountId, int numOfSettingAttributes) {
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      String password = "password_" + i;
      final AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, password);
      settingAttributes.add(getSettingAttribute(appDynamicsConfig));
    }
    return settingAttributes;
  }

  protected SettingAttribute getSettingAttribute(AppDynamicsConfig appDynamicsConfig) {
    return SettingAttribute.Builder.aSettingAttribute()
        .withAccountId(appDynamicsConfig.getAccountId())
        .withValue(appDynamicsConfig)
        .withAppId(UUID.randomUUID().toString())
        .withCategory(SettingCategory.CONNECTOR)
        .withEnvId(UUID.randomUUID().toString())
        .withName(UUID.randomUUID().toString())
        .build();
  }

  protected JenkinsConfig getJenkinsConfig(String accountId, String password) {
    return JenkinsConfig.builder()
        .accountId(accountId)
        .jenkinsUrl(UUID.randomUUID().toString())
        .username(UUID.randomUUID().toString())
        .password(password.toCharArray())
        .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
        .build();
  }

  protected SettingAttribute getSettingAttribute(JenkinsConfig jenkinsConfig) {
    return SettingAttribute.Builder.aSettingAttribute()
        .withAccountId(jenkinsConfig.getAccountId())
        .withValue(jenkinsConfig)
        .withAppId(UUID.randomUUID().toString())
        .withCategory(SettingCategory.CONNECTOR)
        .withEnvId(UUID.randomUUID().toString())
        .withName(UUID.randomUUID().toString())
        .build();
  }

  protected WinRmConnectionAttributes getWinRmConnectionAttribute(String accountId, String password) {
    return WinRmConnectionAttributes.builder()
        .accountId(accountId)
        .password(password.toCharArray())
        .authenticationScheme(AuthenticationScheme.NTLM)
        .port(5164)
        .skipCertChecks(true)
        .useSSL(true)
        .username("mark.lu")
        .build();
  }

  protected SettingAttribute getSettingAttribute(WinRmConnectionAttributes winRmConnectionAttributes) {
    return SettingAttribute.Builder.aSettingAttribute()
        .withAccountId(winRmConnectionAttributes.getAccountId())
        .withValue(winRmConnectionAttributes)
        .withAppId(UUID.randomUUID().toString())
        .withCategory(SettingCategory.SETTING)
        .withEnvId(UUID.randomUUID().toString())
        .withName(UUID.randomUUID().toString())
        .build();
  }

  protected void enableFeatureFlag(FeatureName featureName) {
    featureFlagService.initializeFeatureFlags();
    wingsPersistence.update(
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter(FeatureFlagKeys.name, featureName),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.enabled, true));
    assertThat(featureFlagService.isEnabledReloadCache(featureName, generateUuid())).isTrue();
  }

  protected void disableFeatureFlag(FeatureName featureName) {
    featureFlagService.initializeFeatureFlags();
    wingsPersistence.update(
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter(FeatureFlagKeys.name, featureName),
        wingsPersistence.createUpdateOperations(FeatureFlag.class)
            .set(FeatureFlagKeys.enabled, false)
            .set(FeatureFlagKeys.accountIds, Collections.emptyList()));
    assertThat(featureFlagService.isEnabledReloadCache(featureName, generateUuid()));
  }
}
