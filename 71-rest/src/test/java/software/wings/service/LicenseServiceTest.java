package software.wings.service;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.common.Constants.HARNESS_NAME;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.common.Constants;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.LicenseUtil;
import software.wings.service.intfc.AccountService;

import java.util.Calendar;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public class LicenseServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private AccountService accountService;
  @InjectMocks @Inject private LicenseService licenseService;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    setInternalState(licenseService, "accountService", accountService);
    setInternalState(accountService, "licenseService", licenseService);
  }

  @Test
  public void shouldFailSavingAccountWithoutLicense() {
    thrown.expect(WingsException.class);
    thrown.expectMessage("Invalid / Null license info");
    accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
  }

  @Test
  public void shouldSaveTrialAccountWithDefaultValues() {
    long expiryTime = LicenseUtil.getDefaultTrialExpiryTime();
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
    assertThat(accountFromDB.getLicenseInfo().getLicenseUnits()).isEqualTo(Constants.DEFAULT_TRIAL_LICENSE_UNITS);
  }

  @Test
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
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
    assertThat(accountFromDB.getLicenseInfo().getAccountType()).isEqualTo(AccountType.PAID);
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(timestamp);
  }

  @Test
  public void shouldSaveAccountWithSpecificTypeAndExpiryTime() {
    long expiryTime = LicenseUtil.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
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
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountFromDB.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
  }

  @Test
  public void shouldUpdatePaidAccountWithDefaultValues() {
    long expiryTime = LicenseUtil.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);
    licenseInfo.setLicenseUnits(20);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
    Account accountFromDB = accountService.get(account.getUuid());
    licenseService.updateAccountLicense(accountFromDB.getUuid(), accountFromDB.getLicenseInfo());
    accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo()).isNotNull();
  }

  @Test
  public void shouldUpdateTrialAccountWithDefaultValues() {
    long expiryTime = LicenseUtil.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
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
  public void shouldFailToUpdateTrialAccountWithNullLicenseInfo() {
    long expiryTime = LicenseUtil.getDefaultTrialExpiryTime();
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
    Account accountFromDB = accountService.get(account.getUuid());
    thrown.expect(WingsException.class);
    thrown.expectMessage("Invalid / Null license info for update");
    licenseService.updateAccountLicense(accountFromDB.getUuid(), null);
  }

  @Test
  public void shouldUpdateTrialAccount3WithDefaultValues() {
    long expiryTime = LicenseUtil.getDefaultTrialExpiryTime();
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpireAfterDays(1);
    licenseInfo.setLicenseUnits(20);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
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
  public void shouldUpdateAccountWithSpecificType() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(20);
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
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
  public void shouldUpdateAccountWithSpecificTypeAndExpiryTime() {
    long expiryTime = LicenseUtil.getDefaultTrialExpiryTime() + 100000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(10);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());
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
  public void shouldUpdateOnPremTrialAccountWithDefaultValues() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);

    long expiryTime = LicenseUtil.getDefaultTrialExpiryTime();
    Account accountFromDB = accountService.save(anAccount()
                                                    .withCompanyName(HARNESS_NAME)
                                                    .withAccountName(HARNESS_NAME)
                                                    .withAccountKey("ACCOUNT_KEY")
                                                    .withLicenseInfo(licenseInfo)
                                                    .build());
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
    assertThat(accountFromDB.getLicenseInfo().getLicenseUnits()).isEqualTo(100);
  }

  @Test
  public void shouldUpdateOnPremTrialAccountWithSpecificValues() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(10);

    long expiryTime = LicenseUtil.getDefaultPaidExpiryTime();
    Account accountFromDB = accountService.save(anAccount()
                                                    .withCompanyName(HARNESS_NAME)
                                                    .withAccountName(HARNESS_NAME)
                                                    .withAccountKey("ACCOUNT_KEY")
                                                    .withLicenseInfo(licenseInfo)
                                                    .build());

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

    String generatedLicense = licenseService.generateLicense(licenseInfo);

    Account account = new Account();
    account.setEncryptedLicenseInfo(decodeBase64(generatedLicense));
    Account accountWithDecryptedInfo = licenseService.decryptLicenseInfo(account, false);
    assertThat(accountWithDecryptedInfo).isNotNull();
    assertThat(accountWithDecryptedInfo.getLicenseInfo()).isNotNull();
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getExpiryTime()).isEqualTo(expiryTime);
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(accountWithDecryptedInfo.getLicenseInfo().getAccountType()).isEqualTo(AccountType.TRIAL);
  }

  @Test
  public void shouldCheckForLicenseExpiry() throws InterruptedException {
    long expiryTime = System.currentTimeMillis() + 1000;
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(expiryTime);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());

    Thread.sleep(2000);
    licenseService.checkForLicenseExpiry();
    Account accountFromDB = accountService.get(account.getUuid());
    assertThat(accountFromDB.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
  }

  private String getEncryptedString(LicenseInfo licenseInfo) {
    return licenseService.generateLicense(licenseInfo);
  }
}
