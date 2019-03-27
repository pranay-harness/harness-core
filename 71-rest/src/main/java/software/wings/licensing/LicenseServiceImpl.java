package software.wings.licensing;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.LICENSE_EXPIRED;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.common.Constants.DEFAULT_FREE_LICENSE_UNITS;
import static software.wings.common.Constants.DEFAULT_TRIAL_LICENSE_UNITS;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.DefaultSalesContacts;
import software.wings.beans.DefaultSalesContacts.AccountTypeDefault;
import software.wings.beans.License;
import software.wings.beans.LicenseInfo;
import software.wings.common.Constants;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.LicenseUtil;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.utils.Validator;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author rktummala on 11/10/18
 */
@Singleton
public class LicenseServiceImpl implements LicenseService {
  private static final Logger logger = LoggerFactory.getLogger(LicenseServiceImpl.class);
  @Inject private AccountService accountService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private GenericDbCache dbCache;
  @Inject private ExecutorService executorService;
  @Inject private LicenseProvider licenseProvider;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public void checkForLicenseExpiry() {
    List<String> trialDefaultContacts = null;
    List<String> paidDefaultContacts = null;

    DefaultSalesContacts defaultSalesContacts = mainConfiguration.getDefaultSalesContacts();
    if (defaultSalesContacts != null && defaultSalesContacts.isEnabled()) {
      List<AccountTypeDefault> accountTypeDefaults = defaultSalesContacts.getAccountTypeDefaults();

      if (isNotEmpty(accountTypeDefaults)) {
        for (AccountTypeDefault accountTypeDefault : accountTypeDefaults) {
          switch (accountTypeDefault.getAccountType()) {
            case AccountType.PAID:
              paidDefaultContacts = getEmailIds(accountTypeDefault.getEmailIds());
              break;
            case AccountType.TRIAL:
              trialDefaultContacts = getEmailIds(accountTypeDefault.getEmailIds());
              break;
            default:
              break;
          }
        }
      }
    }

    Query<Account> query = wingsPersistence.createQuery(Account.class, excludeAuthority);
    query.project("_id", true);
    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        try {
          account = accountService.get(account.getUuid());
          LicenseInfo licenseInfo = account.getLicenseInfo();

          if (licenseInfo == null) {
            continue;
          }

          String accountType = licenseInfo.getAccountType();
          if (isEmpty(accountType)) {
            continue;
          }

          if (accountType.equals(AccountType.FREE)) {
            continue;
          }

          long expiryTime = licenseInfo.getExpiryTime();
          long currentTime = System.currentTimeMillis();
          if (currentTime < expiryTime) {
            if (accountType.equals(AccountType.PAID)) {
              if (!account.isEmailSentToSales() && ((expiryTime - currentTime) <= Duration.ofDays(30).toMillis())) {
                sendEmail(account, expiryTime, accountType, Constants.EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE,
                    Constants.EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE, paidDefaultContacts);
              }
            } else if (accountType.equals(AccountType.TRIAL)) {
              if (!account.isEmailSentToSales() && ((expiryTime - currentTime) <= Duration.ofDays(7).toMillis())) {
                sendEmail(account, expiryTime, accountType, Constants.EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE,
                    Constants.EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE, trialDefaultContacts);
              }
            }
          } else {
            if (AccountStatus.ACTIVE.equals(licenseInfo.getAccountStatus())) {
              expireLicense(account.getUuid(), licenseInfo);
              sendEmail(account, expiryTime, accountType, Constants.EMAIL_SUBJECT_ACCOUNT_EXPIRED,
                  Constants.EMAIL_BODY_ACCOUNT_EXPIRED,
                  accountType.equals(AccountType.PAID) ? paidDefaultContacts : trialDefaultContacts);
            }
          }
        } catch (Exception e) {
          logger.warn("Failed to check license info for account id {}", account.getUuid(), e);
        }
      }
    }
  }

  private List<String> getEmailIds(String emailIdsStr) {
    if (isEmpty(emailIdsStr)) {
      return null;
    }

    String[] emailIdArr = emailIdsStr.split(",");

    if (isEmpty(emailIdArr)) {
      return null;
    }

    List<String> emailIds = new ArrayList<>();

    for (String emailId : emailIdArr) {
      emailIds.add(emailId.trim());
    }

    return emailIds;
  }

  private void sendEmail(
      Account account, long expiryTime, String accountType, String subject, String body, List<String> defaultContacts) {
    if (isEmpty(account.getSalesContacts()) && isEmpty(defaultContacts)) {
      logger.info(
          "Skipping the sending of email for account {} since no sales contacts were configured", account.getUuid());
      return;
    }

    List<String> mailingList = isEmpty(account.getSalesContacts()) ? defaultContacts : account.getSalesContacts();

    Date expiryDate = new Date(expiryTime);
    Map<String, String> templateModel = new HashMap<>();
    templateModel.put("emailSubject", subject);
    templateModel.put("emailBody", body);
    templateModel.put("accountName", account.getAccountName());
    templateModel.put("companyName", account.getCompanyName());
    templateModel.put("accountType", accountType);
    templateModel.put("expiry", expiryDate.toString());

    EmailData emailData = EmailData.builder()
                              .system(true)
                              .to(mailingList)
                              .templateName("send_email_to_sales")
                              .templateModel(templateModel)
                              .accountId(account.getUuid())
                              .build();
    emailData.setCc(Collections.emptyList());
    emailData.setRetries(3);
    boolean sent = emailNotificationService.send(emailData);
    if (sent) {
      updateEmailSentToSales(account.getUuid(), true);
    } else {
      updateEmailSentToSales(account.getUuid(), false);
      logger.warn("Couldn't send email to sales for account {}", account.getUuid());
    }
  }

  @Override
  public Account addLicenseInfo(Account account) {
    String deployMode = System.getenv().get("DEPLOY_MODE");
    LicenseInfo licenseInfo = account.getLicenseInfo();
    if (licenseInfo == null) {
      if (!DeployMode.isOnPrem(deployMode)) {
        throw new WingsException("Invalid / Null license info", USER);
      } else {
        return account;
      }
    }

    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    byte[] encryptedLicenseInfo = getEncryptedLicenseInfo(licenseInfo);
    account.setEncryptedLicenseInfo(encryptedLicenseInfo);
    return account;
  }

  private byte[] getEncryptedLicenseInfo(LicenseInfo licenseInfo) {
    if (licenseInfo == null) {
      throw new WingsException("Invalid / Null license info", USER);
    }

    if (!AccountStatus.isValid(licenseInfo.getAccountStatus())) {
      throw new WingsException("Invalid / Null license info account status", USER);
    }

    if (!AccountType.isValid(licenseInfo.getAccountType())) {
      throw new WingsException("Invalid / Null license info account type", USER);
    }

    if (licenseInfo.getAccountType().equals(AccountType.FREE)) {
      licenseInfo.setExpiryTime(-1L);
    } else {
      int expiryInDays = licenseInfo.getExpireAfterDays();
      if (expiryInDays > 0) {
        licenseInfo.setExpiryTime(getExpiryTime(expiryInDays));
      } else if (licenseInfo.getExpiryTime() <= System.currentTimeMillis()) {
        if (licenseInfo.getAccountType().equals(AccountType.TRIAL)) {
          licenseInfo.setExpiryTime(LicenseUtil.getDefaultTrialExpiryTime());
        } else if (licenseInfo.getAccountType().equals(AccountType.PAID)) {
          licenseInfo.setExpiryTime(LicenseUtil.getDefaultPaidExpiryTime());
        }
      }
    }

    if (licenseInfo.getExpiryTime() == 0L) {
      throw new WingsException("No expiry set. Cannot proceed.", USER);
    }

    if (licenseInfo.getAccountType().equals(AccountType.TRIAL)) {
      licenseInfo.setLicenseUnits(DEFAULT_TRIAL_LICENSE_UNITS);
    } else if (licenseInfo.getAccountType().equals(AccountType.FREE)) {
      licenseInfo.setLicenseUnits(DEFAULT_FREE_LICENSE_UNITS);
    }

    if (licenseInfo.getLicenseUnits() <= 0) {
      throw new WingsException("Invalid number of license units. Cannot proceed.", USER);
    }

    return EncryptionUtils.encrypt(LicenseUtil.convertToString(licenseInfo).getBytes(Charset.forName("UTF-8")), null);
  }

  private long getExpiryTime(int numberOfDays) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, numberOfDays);
    calendar.set(Calendar.HOUR, 11);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.AM_PM, Calendar.PM);
    return calendar.getTimeInMillis();
  }

  private byte[] getEncryptedLicenseInfoForUpdate(
      String accountId, LicenseInfo currentLicenseInfo, LicenseInfo newLicenseInfo) {
    if (newLicenseInfo == null) {
      throw new WingsException("Invalid / Null license info for update", USER);
    }

    if (currentLicenseInfo == null) {
      return getEncryptedLicenseInfo(newLicenseInfo);
    }

    if (isNotEmpty(newLicenseInfo.getAccountStatus())) {
      if (!AccountStatus.isValid(newLicenseInfo.getAccountStatus())) {
        throw new WingsException("Invalid / Null license info account status", USER);
      }
      currentLicenseInfo.setAccountStatus(newLicenseInfo.getAccountStatus());
    }

    int resetLicenseUnitsCount = 0;
    long resetExpiryTime = 0;
    if (isNotEmpty(newLicenseInfo.getAccountType())) {
      if (!AccountType.isValid(newLicenseInfo.getAccountType())) {
        throw new WingsException("Invalid / Null license info account type", USER);
      }

      if (isNotEmpty(newLicenseInfo.getAccountType())
          && !currentLicenseInfo.getAccountType().equals(newLicenseInfo.getAccountType())) {
        if (AccountType.TRIAL.equals(newLicenseInfo.getAccountType())) {
          resetLicenseUnitsCount = Constants.DEFAULT_TRIAL_LICENSE_UNITS;
          resetExpiryTime = LicenseUtil.getDefaultTrialExpiryTime();
        } else if (AccountType.FREE.equals(newLicenseInfo.getAccountType())) {
          resetLicenseUnitsCount = Constants.DEFAULT_FREE_LICENSE_UNITS;
          resetExpiryTime = -1L;
        } else if (AccountType.PAID.equals(newLicenseInfo.getAccountType())) {
          resetExpiryTime = LicenseUtil.getDefaultPaidExpiryTime();
        }
      }
      currentLicenseInfo.setAccountType(newLicenseInfo.getAccountType());
    }

    if (isEmpty(currentLicenseInfo.getAccountStatus())) {
      throw new WingsException("Null license info account status. Cannot proceed with update", USER);
    }

    if (isEmpty(currentLicenseInfo.getAccountType())) {
      throw new WingsException("Null license info account type. Cannot proceed with update", USER);
    }

    if (currentLicenseInfo.getAccountType().equals(AccountType.FREE)) {
      currentLicenseInfo.setExpiryTime(-1L);
    } else {
      int expiryInDays = newLicenseInfo.getExpireAfterDays();
      if (expiryInDays > 0) {
        currentLicenseInfo.setExpiryTime(getExpiryTime(expiryInDays));
      } else if (newLicenseInfo.getExpiryTime() > 0
          && newLicenseInfo.getExpiryTime() != currentLicenseInfo.getExpiryTime()) {
        if (newLicenseInfo.getExpiryTime() <= System.currentTimeMillis()) {
          throw new WingsException("Expiry time less than current time. Cannot proceed with update", USER);
        }
        currentLicenseInfo.setExpiryTime(newLicenseInfo.getExpiryTime());
      } else {
        if (resetExpiryTime > 0) {
          currentLicenseInfo.setExpiryTime(resetExpiryTime);
        }
      }
    }

    if (currentLicenseInfo.getExpiryTime() == 0L) {
      throw new WingsException("No expiry set. Cannot proceed with update", USER);
    }

    if (newLicenseInfo.getLicenseUnits() > 0) {
      currentLicenseInfo.setLicenseUnits(newLicenseInfo.getLicenseUnits());
    } else if (resetLicenseUnitsCount > 0) {
      currentLicenseInfo.setLicenseUnits(resetLicenseUnitsCount);
    }

    if (currentLicenseInfo.getLicenseUnits() <= 0) {
      throw new WingsException("Invalid number of license units. Cannot proceed with update", USER);
    }

    return EncryptionUtils.encrypt(
        LicenseUtil.convertToString(currentLicenseInfo).getBytes(Charset.forName("UTF-8")), null);
  }

  @Override
  public Account updateAccountLicense(@NotEmpty String accountId, LicenseInfo licenseInfo) {
    Account accountInDB = accountService.get(accountId);
    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB);

    LicenseInfo oldLicenseInfo = accountInDB.getLicenseInfo();
    String oldAccountType = null;
    if (oldLicenseInfo != null) {
      oldAccountType = oldLicenseInfo.getAccountType();
    }

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);

    byte[] encryptedLicenseInfo = getEncryptedLicenseInfoForUpdate(accountId, oldLicenseInfo, licenseInfo);

    updateOperations.set("encryptedLicenseInfo", encryptedLicenseInfo);

    wingsPersistence.update(accountInDB, updateOperations);
    updateEmailSentToSales(accountId, false);
    dbCache.invalidate(Account.class, accountId);
    Account updatedAccount = wingsPersistence.get(Account.class, accountId);
    decryptLicenseInfo(updatedAccount, false);
    //    refreshUsersForAccountUpdate(updatedAccount);

    eventPublishHelper.publishLicenseChangeEvent(accountId, oldAccountType, licenseInfo.getAccountType());

    return updatedAccount;
  }

  @Override
  public Account updateAccountSalesContacts(@NotEmpty String accountId, List<String> salesContacts) {
    Account accountInDB = accountService.get(accountId);

    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB);

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);

    if (isNotEmpty(salesContacts)) {
      updateOperations.set("salesContacts", salesContacts);
    } else {
      updateOperations.unset("salesContacts");
    }

    wingsPersistence.update(accountInDB, updateOperations);
    updateEmailSentToSales(accountId, false);
    dbCache.invalidate(Account.class, accountId);
    Account updatedAccount = wingsPersistence.get(Account.class, accountId);
    decryptLicenseInfo(updatedAccount, false);
    //    refreshUsersForAccountUpdate(updatedAccount);
    return updatedAccount;
  }

  @Override
  public Account decryptLicenseInfo(Account account, boolean setExpiry) {
    if (account == null) {
      return null;
    }

    byte[] encryptedLicenseInfo = account.getEncryptedLicenseInfo();
    if (isNotEmpty(encryptedLicenseInfo)) {
      byte[] decryptedBytes = EncryptionUtils.decrypt(encryptedLicenseInfo, null);
      if (isNotEmpty(decryptedBytes)) {
        LicenseInfo licenseInfo = LicenseUtil.convertToObject(decryptedBytes, setExpiry);
        account.setLicenseInfo(licenseInfo);
      } else {
        logger.error("Error while decrypting license info. Deserialized object is not instance of LicenseInfo");
      }
    }

    return account;
  }

  @Override
  public String generateLicense(LicenseInfo licenseInfo) {
    if (licenseInfo == null) {
      throw new WingsException("Invalid license info", USER);
    }

    return encodeBase64(getEncryptedLicenseInfo(licenseInfo));
  }

  @Override
  public void updateAccountLicenseForOnPrem(String encryptedLicenseInfoBase64String) {
    try {
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        String msg = "Couldn't find license info";
        throw new WingsException(msg);
      }

      List<Account> accountList = accountService.listAllAccounts();
      if (accountList == null) {
        String msg = "Couldn't find any accounts in DB";
        throw new WingsException(msg);
      }

      accountList.forEach(account -> {
        if (!account.getAccountName().equalsIgnoreCase("Global")) {
          byte[] encryptedLicenseInfo = Base64.getDecoder().decode(encryptedLicenseInfoBase64String.getBytes());
          byte[] encryptedLicenseInfoFromDB = account.getEncryptedLicenseInfo();

          boolean noLicenseInfoInDB = isEmpty(encryptedLicenseInfoFromDB);

          if (noLicenseInfoInDB || !Arrays.equals(encryptedLicenseInfo, encryptedLicenseInfoFromDB)) {
            account.setEncryptedLicenseInfo(encryptedLicenseInfo);
            decryptLicenseInfo(account, true);
            LicenseInfo licenseInfo = account.getLicenseInfo();
            if (licenseInfo != null) {
              updateAccountLicense(account.getUuid(), licenseInfo);
            } else {
              throw new WingsException("No license info could be extracted from the encrypted license info");
            }
          }
        }
      });
    } catch (Exception ex) {
      throw new WingsException("Error while updating account license for on-prem", ex);
    }
  }

  @Override
  public boolean isAccountDeleted(String accountId) {
    // We get the account status from local cache even though its eventually consistent,
    // Since this is called by delegate service frequently, we are referring to cache.
    return AccountStatus.DELETED.equals(accountService.getAccountStatus(accountId));
  }

  @Override
  public boolean isAccountExpired(String accountId) {
    // TODO when we have distributed cache, account should be cached and referred.
    Account account = dbCache.get(Account.class, accountId);
    Validator.notNullCheck("Invalid account with id: " + accountId, account);

    LicenseInfo licenseInfo = account.getLicenseInfo();

    if (licenseInfo == null) {
      return false;
    }

    String accountType = licenseInfo.getAccountType();
    String accountStatus = licenseInfo.getAccountStatus();

    if (isEmpty(accountType)) {
      throw new WingsException("Account type is null for account :" + accountId);
    }

    if (isEmpty(accountStatus)) {
      throw new WingsException("Account status is null for account :" + accountId);
    }

    if (AccountType.FREE.equals(accountType)) {
      return false;
    }

    if (AccountStatus.EXPIRED.equals(accountStatus) || AccountStatus.DELETED.equals(accountStatus)) {
      return true;
    }

    if (System.currentTimeMillis() > licenseInfo.getExpiryTime()) {
      executorService.submit(() -> expireLicense(accountId, licenseInfo));
      return true;
    }

    return false;
  }

  private void expireLicense(String accountId, LicenseInfo licenseInfo) {
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    updateAccountLicense(accountId, licenseInfo);
  }

  private void updateEmailSentToSales(String accountId, boolean status) {
    wingsPersistence.updateField(Account.class, accountId, "emailSentToSales", status);
  }

  @Override
  public void validateLicense(String accountId, String operation) throws WingsException {
    Account account = accountService.get(accountId);
    LicenseInfo licenseInfo = account.getLicenseInfo();
    if (licenseInfo == null) {
      throw new WingsException(LICENSE_EXPIRED);
    }

    if (licenseInfo.getExpiryTime() > 0 && System.currentTimeMillis() > licenseInfo.getExpiryTime()) {
      licenseProvider.get(account.getLicenseId());
      // throw new WingsException(LICENSE_NOT_ALLOWED);
    } else {
      throw new WingsException(LICENSE_EXPIRED);
    }
  }

  @Override
  public void setLicense(Account account) {
    List<License> licenseList = licenseProvider.getActiveLicenses();
    account.setLicenseId(licenseList.get(0).getUuid());
    if (account.getLicenseInfo() == null) {
      account.setLicenseInfo(new LicenseInfo());
    }

    account.getLicenseInfo().setExpiryTime(System.currentTimeMillis() + licenseList.get(0).getExpiryDuration());
  }
}
