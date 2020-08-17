package software.wings.licensing;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.rule.OwnerRule.MEHUL;
import static io.harness.rule.OwnerRule.RAMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.service.intfc.instance.licensing.InstanceLimitProvider.DEFAULT_SI_USAGE_LIMITS;
import static software.wings.service.intfc.instance.licensing.InstanceLimitProvider.defaults;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.service.impl.LicenseUtils;
import software.wings.service.intfc.AccountService;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public class LicenseServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private AccountService accountService;
  @InjectMocks @Inject private LicenseServiceImpl licenseService;

  @Rule public ExpectedException thrown = ExpectedException.none();

  private static final long oneDayTimeDiff = 86400000L;
  private static final String ACCOUNT_KEY = "ACCOUNT_KEY";
  private static final String TRIAL_EXPIRATION_DAY_0_TEMPLATE = "trial_expiration_day0";
  private static final String TRIAL_EXPIRATION_DAY_29_TEMPLATE = "trial_expiration_day29";
  private static final String TRIAL_EXPIRATION_DAY_30_TEMPLATE = "trial_expiration_day30";

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(licenseService, "accountService", accountService, true);
    FieldUtils.writeField(accountService, "licenseService", licenseService, true);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldFailSavingAccountWithoutLicense() {
    thrown.expect(WingsException.class);
    thrown.expectMessage("Invalid / Null license info");
    accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey(ACCOUNT_KEY).build(),
        false);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldSaveTrialAccountWithDefaultValues() {
    long expiryTime = LicenseUtils.getDefaultTrialExpiryTime();
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
    assertThat(accountFromDB.getLicenseInfo().getLicenseUnits()).isEqualTo(defaults(AccountType.TRIAL));
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldSaveAccountWithSpecificType() {
    long timestamp = System.currentTimeMillis() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(100);
    licenseInfo.setExpiryTime(timestamp);
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(timestamp);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldSaveAccountWithSpecificTypeAndExpiryTime() {
    long expiryTime = LicenseUtils.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setExpiryTime(expiryTime);
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldUpdatePaidAccountWithDefaultValues() {
    long expiryTime = LicenseUtils.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);
    licenseInfo.setLicenseUnits(20);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());
    licenseService.updateAccountLicense(accountFromDB.getUuid(), accountFromDB.getLicenseInfo());
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldUpdateTrialAccountWithDefaultValues() {
    long expiryTime = LicenseUtils.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());

    long newExpiryTime = System.currentTimeMillis() + 400000;
    LicenseInfo updatedLicenseInfo = new LicenseInfo();
    updatedLicenseInfo.setExpiryTime(newExpiryTime);

    licenseService.updateAccountLicense(accountFromDB.getUuid(), updatedLicenseInfo);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(newExpiryTime);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldFailToUpdateTrialAccountWithNullLicenseInfo() {
    long expiryTime = LicenseUtils.getDefaultTrialExpiryTime();
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());
    thrown.expect(WingsException.class);
    thrown.expectMessage("Invalid / Null license info for update");
    licenseService.updateAccountLicense(accountFromDB.getUuid(), null);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldUpdateTrialAccount3WithDefaultValues() {
    long expiryTime = LicenseUtils.getDefaultTrialExpiryTime();
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpireAfterDays(1);
    licenseInfo.setLicenseUnits(20);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());

    LicenseInfo updatedLicenseInfo = new LicenseInfo();
    updatedLicenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    updatedLicenseInfo.setExpiryTime(expiryTime);

    licenseService.updateAccountLicense(accountFromDB.getUuid(), updatedLicenseInfo);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldUpdateAccountWithSpecificType() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(20);
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());
    LicenseInfo updatedLicenseInfo = new LicenseInfo();
    updatedLicenseInfo.setAccountType(AccountType.PAID);
    accountFromDB.setLicenseInfo(updatedLicenseInfo);
    licenseService.updateAccountLicense(accountFromDB.getUuid(), updatedLicenseInfo);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isNotEqualTo(0);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldUpdateAccountWithSpecificTypeAndExpiryTime() {
    long expiryTime = LicenseUtils.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(10);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    Account accountFromDB = accountService.get(account.getUuid());

    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);

    accountFromDB.setLicenseInfo(licenseInfo);
    licenseService.updateAccountLicense(accountFromDB.getUuid(), licenseInfo);
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldUpdateOnPremTrialAccountWithDefaultValues() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);

    long expiryTime = LicenseUtils.getDefaultTrialExpiryTime();
    Account accountFromDB = accountService.save(anAccount()
                                                    .withCompanyName(HARNESS_NAME)
                                                    .withAccountName(HARNESS_NAME)
                                                    .withAccountKey(ACCOUNT_KEY)
                                                    .withLicenseInfo(licenseInfo)
                                                    .build(),
        false);
    LicenseInfo updatedLicenseInfo = new LicenseInfo();
    updatedLicenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    updatedLicenseInfo.setAccountType(AccountType.TRIAL);
    String encryptedString = getEncryptedString(updatedLicenseInfo);
    licenseService.updateAccountLicenseForOnPrem(encryptedString);
    accountFromDB = accountService.get(accountFromDB.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
    assertThat(accountFromDB.getLicenseInfo().getLicenseUnits())
        .isEqualTo(DEFAULT_SI_USAGE_LIMITS.get(AccountType.TRIAL));
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldUpdateOnPremTrialAccountWithSpecificValues() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(10);

    long expiryTime = LicenseUtils.getDefaultPaidExpiryTime();
    Account accountFromDB = accountService.save(anAccount()
                                                    .withCompanyName(HARNESS_NAME)
                                                    .withAccountName(HARNESS_NAME)
                                                    .withAccountKey(ACCOUNT_KEY)
                                                    .withLicenseInfo(licenseInfo)
                                                    .build(),
        false);

    LicenseInfo updatedLicenseInfo = new LicenseInfo();
    updatedLicenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    updatedLicenseInfo.setAccountType(AccountType.PAID);
    updatedLicenseInfo.setLicenseUnits(20);
    String encryptedString = getEncryptedString(updatedLicenseInfo);
    licenseService.updateAccountLicenseForOnPrem(encryptedString);
    accountFromDB = accountService.get(accountFromDB.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
    assertThat(accountFromDB.getLicenseInfo().getLicenseUnits()).isEqualTo(20);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldGetNewLicense() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, 1);
    calendar.set(Calendar.HOUR, 11);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.AM_PM, Calendar.PM);
    long expiryTime = calendar.getTimeInMillis();

    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpireAfterDays(1);

    String generatedLicense = LicenseUtils.generateLicense(licenseInfo);

    Account account = new Account();
    account.setEncryptedLicenseInfo(decodeBase64(generatedLicense));
    Account accountWithDecryptedInfo = LicenseUtils.decryptLicenseInfo(account, false);
    assertThat(accountWithDecryptedInfo).isNotNull();
    assertThat(accountWithDecryptedInfo.getLicenseInfo()).isNotNull();
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldCheckForLicenseExpiry() throws InterruptedException {
    long expiryTime = System.currentTimeMillis() + 1000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);

    TimeUnit.SECONDS.sleep(2);
    licenseService.checkForLicenseExpiry(account);
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getEmailTemplateNameForTrialAccountExpiration() {
    long currentTime = System.currentTimeMillis();
    long expiryTime = currentTime + 1000;
    long oneDayAfterExpiry = expiryTime + oneDayTimeDiff;
    long twoDaysAfterExpiry = expiryTime + (2 * oneDayTimeDiff);
    long twentyNineDaysAfterExpiry = expiryTime + (29 * oneDayTimeDiff);
    long thirtyDaysAfterExpiry = expiryTime + (30 * oneDayTimeDiff);
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .withLastLicenseExpiryReminderSentAt(expiryTime - oneDayTimeDiff)
                                              .build(),
        false);
    assertThat(licenseService.getEmailTemplateName(account, oneDayAfterExpiry, expiryTime))
        .isEqualTo(TRIAL_EXPIRATION_DAY_0_TEMPLATE);
    licenseService.updateLastLicenseExpiryReminderSentAt(account.getUuid(), oneDayAfterExpiry);
    account = accountService.get(account.getUuid());
    assertThat(licenseService.getEmailTemplateName(account, oneDayAfterExpiry + 10000, expiryTime)).isNull();
    assertThat(licenseService.getEmailTemplateName(account, twoDaysAfterExpiry, expiryTime)).isNull();
    assertThat(licenseService.getEmailTemplateName(account, twentyNineDaysAfterExpiry, expiryTime))
        .isEqualTo(TRIAL_EXPIRATION_DAY_29_TEMPLATE);
    licenseService.updateLastLicenseExpiryReminderSentAt(account.getUuid(), twentyNineDaysAfterExpiry);
    account = accountService.get(account.getUuid());
    assertThat(licenseService.getEmailTemplateName(account, twentyNineDaysAfterExpiry + 10000, expiryTime)).isNull();
    assertThat(licenseService.getEmailTemplateName(account, thirtyDaysAfterExpiry, expiryTime))
        .isEqualTo(TRIAL_EXPIRATION_DAY_30_TEMPLATE);
    licenseService.updateLastLicenseExpiryReminderSentAt(account.getUuid(), thirtyDaysAfterExpiry);
    account = accountService.get(account.getUuid());
    assertThat(licenseService.getEmailTemplateName(account, thirtyDaysAfterExpiry + 10000, expiryTime)).isNull();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void shouldHandleTrialAccountExpiration() throws InterruptedException {
    long currentTime = System.currentTimeMillis();
    long expiryTime = currentTime + 1000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    TimeUnit.SECONDS.sleep(2);
    LicenseServiceImpl licenseServiceSpy = spy(licenseService);
    doReturn(true).when(licenseServiceSpy).sendEmailToAccountAdmin(any(), anyString());
    licenseServiceSpy.checkForLicenseExpiry(account);
    account = accountService.get(account.getUuid());
    long lastLicenseExpiryReminderSentAtUpdatedValue = account.getLastLicenseExpiryReminderSentAt();
    assertThat(lastLicenseExpiryReminderSentAtUpdatedValue)
        .isBetween(System.currentTimeMillis() - 10000, System.currentTimeMillis() + 10000);
    assertThat(account.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void shouldUpdateAccountStatusAfterThirtyDaysOfExpiry() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey(ACCOUNT_KEY)
                                              .withLicenseInfo(licenseInfo)
                                              .build(),
        false);
    LicenseServiceImpl licenseServiceSpy = spy(licenseService);
    doReturn(true).when(licenseServiceSpy).sendEmailToAccountAdmin(any(), anyString());
    long currentTime = System.currentTimeMillis();
    licenseServiceSpy.handleTrialAccountExpiration(account, currentTime - 30 * oneDayTimeDiff);
    Account accountFromDB = accountService.get(account.getUuid());
    long lastLicenseExpiryReminderSentAtUpdatedValue = accountFromDB.getLastLicenseExpiryReminderSentAt();
    assertThat(lastLicenseExpiryReminderSentAtUpdatedValue)
        .isBetween(System.currentTimeMillis() - 10000, System.currentTimeMillis() + 10000);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.MARKED_FOR_DELETION);
  }

  private String getEncryptedString(LicenseInfo licenseInfo) {
    return LicenseUtils.generateLicense(licenseInfo);
  }
}
