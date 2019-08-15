package software.wings;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.exception.KmsOperationException;
import io.harness.security.encryption.EncryptionType;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.Account;
import software.wings.beans.Account.Builder;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.VaultConfig;
import software.wings.rules.WingsRule;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.util.HashSet;
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

  protected char[] decrypt(EncryptedData data, KmsConfig kmsConfig) throws Exception {
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

  protected char[] decrypt(EncryptedData data, CyberArkConfig cyberArkConfig) throws Exception {
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

  protected char[] decrypt(EncryptedData data, VaultConfig vaultConfig) throws IOException {
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
}
