package software.wings;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.beans.EncryptedData;
import io.harness.beans.MigrateSecretTask;
import io.harness.ff.FeatureFlagService;
import io.harness.queue.QueueConsumer;

import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Account.Builder;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;
import software.wings.dl.WingsPersistence;
import software.wings.resources.secretsmanagement.SecretManagementResource;
import software.wings.rules.WingsRule;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class WingsBaseTest extends CategoryTest implements MockableTestMixin {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  // I am not absolutely sure why, but there is dependency between wings io.harness.rule and
  // MockitoJUnit io.harness.rule and they have to be listed in these order
  @Rule public WingsRule wingsRule = new WingsRule();

  @Inject protected SecretManagementResource secretManagementResource;
  @Inject protected SecretManager secretManager;
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected ConfigService configService;
  @Inject protected EncryptionService encryptionService;
  @Inject protected QueueConsumer<MigrateSecretTask> transitionKmsQueue;
  @Inject protected SettingsService settingsService;
  @Inject protected FeatureFlagService featureFlagService;
  @Inject protected MainConfiguration mainConfiguration;

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

      encryptionService.decrypt(
          appDynamicsConfig, secretManager.getEncryptionDetails(appDynamicsConfig, null, appId), false);
      assertThat(new String(appDynamicsConfig.getPassword())).isEqualTo("password_" + i);

      wingsPersistence.delete(SettingAttribute.class, settingAttribute.getAppId(), settingAttribute.getUuid());
    }

    assertThat(wingsPersistence.createQuery(SettingAttribute.class).count()).isEqualTo(0);
    assertThat(wingsPersistence.createQuery(EncryptedData.class).count()).isEqualTo(expectedNumOfEncryptedRecords);
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
}
