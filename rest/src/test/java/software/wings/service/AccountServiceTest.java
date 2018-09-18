package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.common.Constants.HARNESS_NAME;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.DelegateConfiguration;
import software.wings.beans.LicenseInfo;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.beans.StringValue.Builder;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseManager;
import software.wings.scheduler.JobScheduler;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.template.TemplateGalleryService;

import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public class AccountServiceTest extends WingsBaseTest {
  @Mock private LicenseManager licenseManager;

  @Mock private AppService appService;
  @Mock private SettingsService settingsService;
  @Mock private JobScheduler jobScheduler;
  @Mock private TemplateGalleryService templateGalleryService;
  @Mock private FeatureFlagService featureFlagService;

  @InjectMocks @Inject private AccountService accountService;

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void shouldSaveAccount() {
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
    verify(settingsService).createDefaultAccountSettings(account.getUuid());
    verify(jobScheduler).deleteJob(eq(account.getUuid()), anyString());
  }

  @Test
  public void shouldSaveTrialAccountWithDefaultValuesAndFeatureDisabled() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNull();
  }

  @Test
  public void shouldSaveTrialAccountWithDefaultValues() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    long expiryTime = accountService.getDefaultTrialExpiryTime();
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldSaveAccountWithSpecificType() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(0);
  }

  @Test
  public void shouldSaveAccountWithSpecificTypeAndExpiryTime() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    long expiryTime = accountService.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    licenseInfo.setExpiryTime(expiryTime);
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldUpdateTrialAccountWithDefaultValuesAndFeatureDisabled() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    accountService.updateAccountLicense(accountFromDB.getUuid(), accountFromDB.getLicenseInfo(), null, false);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNull();
  }

  @Test
  public void shouldUpdateTrialAccountWithDefaultValues() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    long expiryTime = accountService.getDefaultTrialExpiryTime();
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    accountService.updateAccountLicense(accountFromDB.getUuid(), accountFromDB.getLicenseInfo(), null, true);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldUpdateTrialAccount2WithDefaultValues() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    long expiryTime = accountService.getDefaultTrialExpiryTime();
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);

    accountService.updateAccountLicense(accountFromDB.getUuid(), null, null, true);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldUpdateTrialAccount3WithDefaultValues() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    long expiryTime = accountService.getDefaultTrialExpiryTime();
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);

    accountService.updateAccountLicense(accountFromDB.getUuid(), null, null, null, null, true);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldUpdateAccountWithSpecificType() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    accountFromDB.setLicenseInfo(licenseInfo);
    accountService.updateAccountLicense(accountFromDB.getUuid(), licenseInfo, null, false);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(0);
  }

  @Test
  public void shouldUpdateAccountWithSpecificTypeAndExpiryTime() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    long expiryTime = accountService.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    licenseInfo.setExpiryTime(expiryTime);

    accountFromDB.setLicenseInfo(licenseInfo);
    accountService.updateAccountLicense(accountFromDB.getUuid(), licenseInfo, null, false);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldUpdateOnPremTrialAccountWithDefaultValuesAndFeatureDisabled() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    accountService.updateAccountLicenseForOnPrem("any");
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNull();
  }

  @Test
  public void shouldUpdateOnPremTrialAccountWithDefaultValues() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    long expiryTime = accountService.getDefaultTrialExpiryTime();
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    String encryptedString = getEncryptedString(null, null, 0L);
    accountService.updateAccountLicenseForOnPrem(encryptedString);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldUpdateOnPremTrialAccountWithSpecificValues() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    long expiryTime = accountService.getDefaultTrialExpiryTime();
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    Account accountFromDB = accountService.get(account.getUuid());
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    String encryptedString = getEncryptedString("PAID", "ACTIVE", expiryTime);
    accountService.updateAccountLicenseForOnPrem(encryptedString);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldGetNewLicense() {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, 1);
    calendar.set(Calendar.HOUR, 11);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.AM_PM, Calendar.PM);
    long expiryTime = calendar.getTimeInMillis();

    String generatedLicense = accountService.generateLicense(AccountType.TRIAL, AccountStatus.ACTIVE, "1");
    byte[] decodedBytes = Base64.getDecoder().decode(generatedLicense);

    Account account = new Account();
    account.setEncryptedLicenseInfo(decodedBytes);
    Account accountWithDecryptedInfo = accountService.decryptLicenseInfo(account, false);
    assertThat(accountWithDecryptedInfo).isNotNull();
    assertThat(accountWithDecryptedInfo.getLicenseInfo()).isNotNull();
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
  }

  private String getEncryptedString(String accountType, String accountStatus, long expiryTime) {
    String text = accountType + "_" + accountStatus + "_" + expiryTime;
    byte[] encrypt = EncryptionUtils.encrypt(text.getBytes(), null);
    return Base64.getEncoder().encodeToString(encrypt);
  }

  @Test
  public void shouldDeleteAccount() {
    String accountId = wingsPersistence.save(anAccount().withCompanyName(HARNESS_NAME).build());
    accountService.delete(accountId);
    assertThat(wingsPersistence.get(Account.class, accountId)).isNull();
    verify(appService).deleteByAccountId(accountId);
    verify(settingsService).deleteByAccountId(accountId);
    verify(templateGalleryService).deleteByAccountId(accountId);
  }

  @Test
  public void shouldUpdateCompanyName() {
    Account account = wingsPersistence.saveAndGet(
        Account.class, anAccount().withCompanyName("Wings").withAccountName("Wings").build());
    account.setCompanyName(HARNESS_NAME);
    accountService.update(account);
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
  }

  @Test
  public void shouldGetAccountByCompanyName() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.getByName(HARNESS_NAME)).isEqualTo(account);
  }

  @Test
  public void shouldGetAccountByAccountName() {
    Account account = wingsPersistence.saveAndGet(
        Account.class, anAccount().withAccountName(HARNESS_NAME).withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.getByAccountName(HARNESS_NAME)).isEqualTo(account);
  }

  @Test
  public void shouldGetAccount() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.get(account.getUuid())).isEqualTo(account);
  }

  @Test
  public void shouldGetDelegateConfiguration() {
    String accountId =
        wingsPersistence.save(anAccount()
                                  .withCompanyName(HARNESS_NAME)
                                  .withDelegateConfiguration(DelegateConfiguration.builder()
                                                                 .watcherVersion("1.0.1")
                                                                 .delegateVersions(asList("1.0.0", "1.0.1"))
                                                                 .build())
                                  .build());
    assertThat(accountService.getDelegateConfiguration(accountId))
        .hasFieldOrPropertyWithValue("watcherVersion", "1.0.1")
        .hasFieldOrPropertyWithValue("delegateVersions", asList("1.0.0", "1.0.1"));
  }

  @Test
  public void shouldGetDelegateConfigurationFromGlobalAccount() {
    wingsPersistence.save(anAccount()
                              .withUuid(GLOBAL_ACCOUNT_ID)
                              .withCompanyName(HARNESS_NAME)
                              .withDelegateConfiguration(DelegateConfiguration.builder()
                                                             .watcherVersion("globalVersion")
                                                             .delegateVersions(asList("globalVersion"))
                                                             .build())
                              .build());

    String accountId = wingsPersistence.save(anAccount().withCompanyName(HARNESS_NAME).build());

    assertThat(accountService.getDelegateConfiguration(accountId))
        .hasFieldOrPropertyWithValue("watcherVersion", "globalVersion")
        .hasFieldOrPropertyWithValue("delegateVersions", asList("globalVersion"));
  }

  @Test
  public void shouldListAllAccounts() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.get(account.getUuid())).isEqualTo(account);
    assertThat(accountService.listAllAccounts()).isNotEmpty();
    assertThat(accountService.listAllAccounts().get(0)).isNotNull();
  }

  @Test
  public void shouldGetAccountWithDefaults() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(account).isNotNull();

    List<SettingAttribute> settingAttributes = asList(aSettingAttribute()
                                                          .withName("NAME")
                                                          .withAccountId(account.getUuid())
                                                          .withValue(Builder.aStringValue().build())
                                                          .build(),
        aSettingAttribute()
            .withName("NAME2")
            .withAccountId(account.getUuid())
            .withValue(Builder.aStringValue().withValue("VALUE").build())
            .build());

    when(settingsService.listAccountDefaults(account.getUuid()))
        .thenReturn(settingAttributes.stream().collect(Collectors.toMap(SettingAttribute::getName,
            settingAttribute
            -> Optional.ofNullable(((StringValue) settingAttribute.getValue()).getValue()).orElse(""),
            (a, b) -> b)));

    account = accountService.getAccountWithDefaults(account.getUuid());
    assertThat(account).isNotNull();
    assertThat(account.getDefaults()).isNotEmpty().containsKeys("NAME", "NAME2");
    assertThat(account.getDefaults()).isNotEmpty().containsValues("", "VALUE");
    verify(settingsService).listAccountDefaults(account.getUuid());
  }
}
