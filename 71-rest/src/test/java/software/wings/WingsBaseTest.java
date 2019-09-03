package software.wings;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.exception.KmsOperationException;
import io.harness.queue.Queue;
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
import software.wings.beans.CyberArkConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.resources.SecretManagementResource;
import software.wings.rules.WingsRule;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  @Inject protected Queue<KmsTransitionEvent> transitionKmsQueue;

  protected EncryptedData encrypt(String accountId, char[] value, KmsConfig kmsConfig) throws Exception {
    if (kmsConfig.getAccessKey().equals("invalidKey")) {
      throw new KmsOperationException("Invalid credentials");
    }
    char[] encryptedValue = value == null ? null
                                          : SecretManagementDelegateServiceImpl.encrypt(
                                                new String(value), new SecretKeySpec(plainTextKey.getBytes(), "AES"));

    return EncryptedData.builder()
        .encryptionKey(plainTextKey)
        .encryptedValue(encryptedValue)
        .encryptionType(EncryptionType.KMS)
        .kmsId(kmsConfig.getUuid())
        .enabled(true)
        .parentIds(new HashSet<>())
        .accountId(accountId)
        .build();
  }

  protected char[] decrypt(EncryptedRecord data, KmsConfig kmsConfig) throws Exception {
    return SecretManagementDelegateServiceImpl
        .decrypt(data.getEncryptedValue(), new SecretKeySpec(plainTextKey.getBytes(), "AES"))
        .toCharArray();
  }

  protected EncryptedData encrypt(String value, CyberArkConfig cyberArkConfig) throws Exception {
    if (cyberArkConfig.getClientCertificate().equals("invalidCertificate")) {
      throw new KmsOperationException("Invalid credentials");
    }
    char[] encryptedValue = value == null
        ? null
        : SecretManagementDelegateServiceImpl.encrypt(value, new SecretKeySpec(plainTextKey.getBytes(), "AES"));

    return EncryptedData.builder()
        .encryptionKey(plainTextKey)
        .encryptedValue(encryptedValue)
        .encryptionType(EncryptionType.CYBERARK)
        .kmsId(cyberArkConfig.getUuid())
        .enabled(true)
        .parentIds(new HashSet<>())
        .accountId(cyberArkConfig.getAccountId())
        .build();
  }

  protected char[] decrypt(EncryptedRecord data, CyberArkConfig cyberArkConfig) throws Exception {
    return "Cyberark1".toCharArray();
  }

  protected EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData savedEncryptedData) throws IOException {
    if (vaultConfig.getAuthToken().equals("invalidKey")) {
      throw new KmsOperationException("invalidKey");
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
        .parentIds(new HashSet<>())
        .kmsId(vaultConfig.getUuid())
        .build();
  }

  protected char[] decrypt(EncryptedRecord data, VaultConfig vaultConfig) throws IOException {
    if (data.getEncryptedValue() == null) {
      return null;
    }
    return data.getEncryptedValue();
  }

  protected VaultConfig getVaultConfig() {
    return getVaultConfig(generateUuid());
  }

  protected VaultConfig getVaultConfig(String authToken) {
    VaultConfig vaultConfig = VaultConfig.builder()
                                  .vaultUrl("http://127.0.0.1:8200")
                                  .authToken(authToken)
                                  .name("myVault")
                                  .secretEngineVersion(1)
                                  .build();
    vaultConfig.setDefault(true);
    return vaultConfig;
  }

  protected KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setDefault(true);
    kmsConfig.setKmsArn(generateUuid());
    kmsConfig.setAccessKey(generateUuid());
    kmsConfig.setSecretKey(generateUuid());
    return kmsConfig;
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
    return cyberArkConfig;
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
        .authMechanism(JenkinsConfig.USERNAME_PASSWORD_FIELD)
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
}
