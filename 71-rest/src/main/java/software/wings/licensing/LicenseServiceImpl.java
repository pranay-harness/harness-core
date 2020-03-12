package software.wings.licensing;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.configuration.DeployMode;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.security.EncryptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.DefaultSalesContacts;
import software.wings.beans.DefaultSalesContacts.AccountTypeDefault;
import software.wings.beans.License;
import software.wings.beans.LicenseInfo;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.impl.LicenseUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.instance.licensing.InstanceLimitProvider;

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
@Slf4j
public class LicenseServiceImpl implements LicenseService {
  private static final String EMAIL_SUBJECT_ACCOUNT_EXPIRED = "Harness License Expired!";
  private static final String EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE = "Harness License about to Expire!";

  private static final String EMAIL_BODY_ACCOUNT_EXPIRED = "Customer License has Expired";
  private static final String EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE = "Customer License is about to Expire";

  private AccountService accountService;
  private WingsPersistence wingsPersistence;
  private GenericDbCache dbCache;
  private ExecutorService executorService;
  private LicenseProvider licenseProvider;
  private EmailNotificationService emailNotificationService;
  private EventPublishHelper eventPublishHelper;
  private List<String> trialDefaultContacts;
  private List<String> paidDefaultContacts;

  @Inject
  public LicenseServiceImpl(AccountService accountService, WingsPersistence wingsPersistence, GenericDbCache dbCache,
      ExecutorService executorService, LicenseProvider licenseProvider,
      EmailNotificationService emailNotificationService, EventPublishHelper eventPublishHelper,
      MainConfiguration mainConfiguration) {
    this.accountService = accountService;
    this.wingsPersistence = wingsPersistence;
    this.dbCache = dbCache;
    this.executorService = executorService;
    this.licenseProvider = licenseProvider;
    this.emailNotificationService = emailNotificationService;
    this.eventPublishHelper = eventPublishHelper;

    DefaultSalesContacts defaultSalesContacts = mainConfiguration.getDefaultSalesContacts();
    if (defaultSalesContacts != null && defaultSalesContacts.isEnabled()) {
      List<AccountTypeDefault> accountTypeDefaults = defaultSalesContacts.getAccountTypeDefaults();

      if (isNotEmpty(accountTypeDefaults)) {
        for (AccountTypeDefault accountTypeDefault : accountTypeDefaults) {
          switch (accountTypeDefault.getAccountType()) {
            case AccountType.ESSENTIALS:
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
  }

  @Override
  public void checkForLicenseExpiry(Account account) {
    try {
      account = accountService.get(account.getUuid());
      LicenseInfo licenseInfo = account.getLicenseInfo();

      if (licenseInfo == null) {
        return;
      }

      String accountStatus = licenseInfo.getAccountStatus();
      String accountType = licenseInfo.getAccountType();
      if (isEmpty(accountType)) {
        return;
      }

      if (accountType.equals(AccountType.COMMUNITY)) {
        return;
      }

      long expiryTime = licenseInfo.getExpiryTime();
      long currentTime = System.currentTimeMillis();
      if (currentTime < expiryTime) {
        if (accountType.equals(AccountType.PAID) || accountType.equals(AccountType.ESSENTIALS)) {
          if (!account.isEmailSentToSales() && ((expiryTime - currentTime) <= Duration.ofDays(30).toMillis())) {
            sendEmailToSales(account, expiryTime, accountType, EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE,
                EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE, paidDefaultContacts);
          }
        } else if (accountType.equals(AccountType.TRIAL)) {
          boolean lessThan7DaysLeftForLicenseExpiry = (expiryTime - currentTime) <= Duration.ofDays(7).toMillis();
          if (!account.isEmailSentToSales() && lessThan7DaysLeftForLicenseExpiry) {
            sendEmailToSales(account, expiryTime, accountType, EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE,
                EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE, trialDefaultContacts);
          }
        }
      } else if (AccountStatus.ACTIVE.equals(accountStatus)) {
        expireLicense(account.getUuid(), licenseInfo);
        sendEmailToSales(account, expiryTime, accountType, EMAIL_SUBJECT_ACCOUNT_EXPIRED, EMAIL_BODY_ACCOUNT_EXPIRED,
            accountType.equals(AccountType.PAID) ? paidDefaultContacts : trialDefaultContacts);
      }
    } catch (Exception e) {
      logger.warn("Failed to check license info", e);
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

  private void sendEmailToSales(
      Account account, long expiryTime, String accountType, String subject, String body, List<String> defaultContacts) {
    if (isEmpty(account.getSalesContacts()) && isEmpty(defaultContacts)) {
      logger.info("Skipping the sending of email since no sales contacts were configured");
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
    String deployMode = System.getenv().get(DeployMode.DEPLOY_MODE);
    LicenseInfo licenseInfo = account.getLicenseInfo();
    if (licenseInfo == null) {
      if (!DeployMode.isOnPrem(deployMode)) {
        throw new InvalidRequestException("Invalid / Null license info", USER);
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
      throw new InvalidRequestException("Invalid / Null license info", USER);
    }

    if (!AccountStatus.isValid(licenseInfo.getAccountStatus())) {
      throw new InvalidRequestException("Invalid / Null license info account status", USER);
    }

    if (!AccountType.isValid(licenseInfo.getAccountType())) {
      throw new InvalidRequestException("Invalid / Null license info account type", USER);
    }

    if (licenseInfo.getAccountType().equals(AccountType.COMMUNITY)) {
      licenseInfo.setExpiryTime(-1L);
    } else {
      int expiryInDays = licenseInfo.getExpireAfterDays();
      if (expiryInDays > 0) {
        licenseInfo.setExpiryTime(getExpiryTime(expiryInDays));
      } else if (licenseInfo.getExpiryTime() <= System.currentTimeMillis()) {
        if (licenseInfo.getAccountType().equals(AccountType.TRIAL)) {
          licenseInfo.setExpiryTime(LicenseUtils.getDefaultTrialExpiryTime());
        } else if (licenseInfo.getAccountType().equals(AccountType.PAID)) {
          licenseInfo.setExpiryTime(LicenseUtils.getDefaultPaidExpiryTime());
        } else if (licenseInfo.getAccountType().equals(AccountType.ESSENTIALS)) {
          licenseInfo.setExpiryTime(LicenseUtils.getDefaultEssentialsExpiryTime());
        }
      }
    }

    if (licenseInfo.getExpiryTime() == 0L) {
      throw new InvalidRequestException("No expiry set. Cannot proceed.", USER);
    }

    if (licenseInfo.getAccountType().equals(AccountType.TRIAL)) {
      licenseInfo.setLicenseUnits(InstanceLimitProvider.defaults(AccountType.TRIAL));
    } else if (licenseInfo.getAccountType().equals(AccountType.COMMUNITY)) {
      licenseInfo.setLicenseUnits(InstanceLimitProvider.defaults(AccountType.COMMUNITY));
    }

    if (licenseInfo.getLicenseUnits() <= 0) {
      throw new InvalidRequestException("Invalid number of license units. Cannot proceed.", USER);
    }

    return EncryptionUtils.encrypt(LicenseUtils.convertToString(licenseInfo).getBytes(Charset.forName("UTF-8")), null);
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

  private byte[] getEncryptedLicenseInfoForUpdate(LicenseInfo currentLicenseInfo, LicenseInfo newLicenseInfo) {
    if (newLicenseInfo == null) {
      throw new InvalidRequestException("Invalid / Null license info for update", USER);
    }

    if (currentLicenseInfo == null) {
      return getEncryptedLicenseInfo(newLicenseInfo);
    }

    if (isNotEmpty(newLicenseInfo.getAccountStatus())) {
      if (!AccountStatus.isValid(newLicenseInfo.getAccountStatus())) {
        throw new InvalidRequestException("Invalid / Null license info account status", USER);
      }
      currentLicenseInfo.setAccountStatus(newLicenseInfo.getAccountStatus());
    }

    int resetLicenseUnitsCount = 0;
    long resetExpiryTime = 0;
    if (isNotEmpty(newLicenseInfo.getAccountType())) {
      if (!AccountType.isValid(newLicenseInfo.getAccountType())) {
        throw new InvalidRequestException("Invalid / Null license info account type", USER);
      }

      if (isNotEmpty(newLicenseInfo.getAccountType())
          && !currentLicenseInfo.getAccountType().equals(newLicenseInfo.getAccountType())) {
        if (AccountType.TRIAL.equals(newLicenseInfo.getAccountType())) {
          resetLicenseUnitsCount = InstanceLimitProvider.defaults(AccountType.TRIAL);
          resetExpiryTime = LicenseUtils.getDefaultTrialExpiryTime();
        } else if (AccountType.COMMUNITY.equals(newLicenseInfo.getAccountType())) {
          resetLicenseUnitsCount = InstanceLimitProvider.defaults(AccountType.COMMUNITY);
          resetExpiryTime = -1L;
        } else if (AccountType.PAID.equals(newLicenseInfo.getAccountType())) {
          resetExpiryTime = LicenseUtils.getDefaultPaidExpiryTime();
        } else if (AccountType.ESSENTIALS.equals(newLicenseInfo.getAccountType())) {
          resetExpiryTime = LicenseUtils.getDefaultEssentialsExpiryTime();
        }
      }
      currentLicenseInfo.setAccountType(newLicenseInfo.getAccountType());
    }

    if (isEmpty(currentLicenseInfo.getAccountStatus())) {
      throw new InvalidRequestException("Null license info account status. Cannot proceed with update", USER);
    }

    if (isEmpty(currentLicenseInfo.getAccountType())) {
      throw new InvalidRequestException("Null license info account type. Cannot proceed with update", USER);
    }

    if (currentLicenseInfo.getAccountType().equals(AccountType.COMMUNITY)) {
      currentLicenseInfo.setExpiryTime(-1L);
    } else {
      int expiryInDays = newLicenseInfo.getExpireAfterDays();
      if (expiryInDays > 0) {
        currentLicenseInfo.setExpiryTime(getExpiryTime(expiryInDays));
      } else if (newLicenseInfo.getExpiryTime() > 0
          && newLicenseInfo.getExpiryTime() != currentLicenseInfo.getExpiryTime()) {
        if (newLicenseInfo.getExpiryTime() <= System.currentTimeMillis()) {
          throw new InvalidRequestException("Expiry time less than current time. Cannot proceed with update", USER);
        }
        currentLicenseInfo.setExpiryTime(newLicenseInfo.getExpiryTime());
      } else {
        if (resetExpiryTime > 0) {
          currentLicenseInfo.setExpiryTime(resetExpiryTime);
        }
      }
    }

    if (currentLicenseInfo.getExpiryTime() == 0L) {
      throw new InvalidRequestException("No expiry set. Cannot proceed with update", USER);
    }

    if (newLicenseInfo.getLicenseUnits() > 0) {
      currentLicenseInfo.setLicenseUnits(newLicenseInfo.getLicenseUnits());
    } else if (resetLicenseUnitsCount > 0) {
      currentLicenseInfo.setLicenseUnits(resetLicenseUnitsCount);
    }

    if (currentLicenseInfo.getLicenseUnits() <= 0) {
      throw new InvalidRequestException("Invalid number of license units. Cannot proceed with update", USER);
    }

    return EncryptionUtils.encrypt(
        LicenseUtils.convertToString(currentLicenseInfo).getBytes(Charset.forName("UTF-8")), null);
  }

  @Override
  public boolean updateAccountLicense(@NotEmpty String accountId, LicenseInfo licenseInfo) {
    Account accountInDB = accountService.get(accountId);
    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB);

    LicenseInfo oldLicenseInfo = accountInDB.getLicenseInfo();
    String oldAccountType = null;
    if (oldLicenseInfo != null) {
      oldAccountType = oldLicenseInfo.getAccountType();
    }

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);

    byte[] encryptedLicenseInfo = getEncryptedLicenseInfoForUpdate(oldLicenseInfo, licenseInfo);

    updateOperations.set("encryptedLicenseInfo", encryptedLicenseInfo);
    updateOperations.set(AccountKeys.licenseInfo, licenseInfo);

    wingsPersistence.update(accountInDB, updateOperations);
    updateEmailSentToSales(accountId, false);
    dbCache.invalidate(Account.class, accountId);
    Account updatedAccount = wingsPersistence.get(Account.class, accountId);
    decryptLicenseInfo(updatedAccount, false);
    //    refreshUsersForAccountUpdate(updatedAccount);

    eventPublishHelper.publishLicenseChangeEvent(accountId, oldAccountType, licenseInfo.getAccountType());
    return true;
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
        LicenseInfo licenseInfo = LicenseUtils.convertToObject(decryptedBytes, setExpiry);
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
      throw new InvalidRequestException("Invalid license info", USER);
    }

    return encodeBase64(getEncryptedLicenseInfo(licenseInfo));
  }

  @Override
  public void updateAccountLicenseForOnPrem(String encryptedLicenseInfoBase64String) {
    try {
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        String msg = "Couldn't find license info";
        throw new InvalidRequestException(msg);
      }

      List<Account> accountList = accountService.listAllAccounts();
      if (accountList == null) {
        String msg = "Couldn't find any accounts in DB";
        throw new InvalidRequestException(msg);
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
              throw new InvalidRequestException("No license info could be extracted from the encrypted license info");
            }
          }
        }
      });
    } catch (Exception ex) {
      throw new InvalidRequestException("Error while updating account license for on-prem", ex);
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
    notNullCheck("Invalid account with id: " + accountId, account);

    LicenseInfo licenseInfo = account.getLicenseInfo();

    if (licenseInfo == null) {
      return false;
    }

    String accountType = licenseInfo.getAccountType();
    String accountStatus = licenseInfo.getAccountStatus();

    if (isEmpty(accountType)) {
      throw new InvalidRequestException("Account type is null for account :" + accountId);
    }

    if (isEmpty(accountStatus)) {
      throw new InvalidRequestException("Account status is null for account :" + accountId);
    }

    if (AccountType.COMMUNITY.equals(accountType)) {
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
  public void validateLicense(String accountId, String operation) {
    Account account = accountService.get(accountId);
    LicenseInfo licenseInfo = account.getLicenseInfo();
    if (licenseInfo == null) {
      throw new InvalidRequestException("license Info not present");
    }

    if (licenseInfo.getExpiryTime() > 0 && System.currentTimeMillis() > licenseInfo.getExpiryTime()) {
      licenseProvider.get(account.getLicenseId());
    } else {
      throw new InvalidRequestException("Invalid expiry time");
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
