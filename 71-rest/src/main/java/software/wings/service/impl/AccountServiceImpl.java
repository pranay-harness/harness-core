package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Account.ACCOUNT_NAME_KEY;
import static software.wings.beans.Account.COMPANY_NAME_KEY;
import static software.wings.beans.AppContainer.Builder.anAppContainer;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.RoleType.ACCOUNT_ADMIN;
import static software.wings.beans.RoleType.APPLICATION_ADMIN;
import static software.wings.beans.RoleType.NON_PROD_SUPPORT;
import static software.wings.beans.RoleType.PROD_SUPPORT;
import static software.wings.beans.SystemCatalog.CatalogType.APPSTACK;
import static software.wings.common.Constants.SUPPORT_EMAIL;
import static software.wings.utils.Misc.generateSecretKey;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.DeployMode;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.AppContainer;
import software.wings.beans.DelegateConfiguration;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.LicenseInfo;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SystemCatalog;
import software.wings.beans.User;
import software.wings.common.Constants;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseManager;
import software.wings.scheduler.AlertCheckJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.utils.Validator;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
/**
 * Created by peeyushaggarwal on 10/11/16.
 */
@Singleton
@ValidateOnExecution
public class AccountServiceImpl implements AccountService {
  private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private RoleService roleService;
  @Inject private UserGroupService userGroupService;
  // DO NOT DELETE THIS, PRUNE logic needs it
  @SuppressWarnings("unused") @Inject private InstanceService instanceService;
  @Inject private AuthHandler authHandler;
  @Inject private LicenseManager licenseManager;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private SettingsService settingsService;
  @Inject private ExecutorService executorService;
  @Inject private AppService appService;
  @Inject private AppContainerService appContainerService;
  @Inject private SystemCatalogService systemCatalogService;
  @Inject private AlertService alertService;
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private GenericDbCache dbCache;
  @Inject private FeatureFlagService featureFlagService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Override
  public Account save(@Valid Account account) {
    account.setAccountKey(generateSecretKey());
    if (isEmpty(account.getUuid())) {
      account.setUuid(UUIDGenerator.generateUuid());
    }

    if (featureFlagService.isEnabled(FeatureName.TRIAL_SUPPORT, account.getUuid())) {
      byte[] encryptedLicenseInfo = getEncryptedLicenseInfo(account.getLicenseInfo(), true);
      account.setEncryptedLicenseInfo(encryptedLicenseInfo);

      if (isEmpty(account.getSalesContacts())) {
        account.setSalesContacts(asList(SUPPORT_EMAIL));
      } else {
        if (!account.getSalesContacts().contains(SUPPORT_EMAIL)) {
          account.getSalesContacts().add(SUPPORT_EMAIL);
        }
      }
    }

    wingsPersistence.save(account);
    createDefaultAccountEntities(account);
    AlertCheckJob.add(jobScheduler, account);
    return account;
  }

  private byte[] getEncryptedLicenseInfo(LicenseInfo licenseInfo, boolean checkAndSetDefaultExpiry) {
    if (licenseInfo == null) {
      licenseInfo = new LicenseInfo();
    }

    if (isEmpty(licenseInfo.getAccountType())) {
      licenseInfo.setAccountType(AccountType.TRIAL);
    }

    if (isEmpty(licenseInfo.getAccountStatus())) {
      licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    }

    if (checkAndSetDefaultExpiry) {
      // TODO when licensing is implemented, we need to set the expiry time for paid accounts too
      if (licenseInfo.getAccountType().equals(AccountType.TRIAL)
          && licenseInfo.getExpiryTime() <= System.currentTimeMillis()) {
        licenseInfo.setExpiryTime(getDefaultTrialExpiryTime());
      }
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

  @Override
  public long getDefaultTrialExpiryTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, Constants.TRIAL_PERIOD);
    calendar.set(Calendar.HOUR, 11);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.AM_PM, Calendar.PM);
    return calendar.getTimeInMillis();
  }

  private void createDefaultAccountEntities(Account account) {
    settingsService.createDefaultAccountSettings(account.getUuid());
    createDefaultRoles(account)
        .stream()
        .filter(role -> RoleType.ACCOUNT_ADMIN.equals(role.getRoleType()))
        .forEach(role -> createDefaultNotificationGroup(account, role));
    createSystemAppContainers(account);
    authHandler.createDefaultUserGroups(account, null);

    executorService.submit(
        () -> templateGalleryService.copyHarnessTemplatesToAccount(account.getUuid(), account.getAccountName()));
  }

  List<Role> createDefaultRoles(Account account) {
    return Lists.newArrayList(roleService.save(aRole()
                                                   .withAppId(GLOBAL_APP_ID)
                                                   .withAccountId(account.getUuid())
                                                   .withName(ACCOUNT_ADMIN.getDisplayName())
                                                   .withRoleType(ACCOUNT_ADMIN)
                                                   .build()),

        roleService.save(aRole()
                             .withAppId(GLOBAL_APP_ID)
                             .withAccountId(account.getUuid())
                             .withName(APPLICATION_ADMIN.getDisplayName())
                             .withRoleType(APPLICATION_ADMIN)
                             .withAllApps(true)
                             .build()),
        roleService.save(aRole()
                             .withAppId(GLOBAL_APP_ID)
                             .withAccountId(account.getUuid())
                             .withName(PROD_SUPPORT.getDisplayName())
                             .withRoleType(PROD_SUPPORT)
                             .withAllApps(true)
                             .build()),
        roleService.save(aRole()
                             .withAppId(GLOBAL_APP_ID)
                             .withAccountId(account.getUuid())
                             .withName(NON_PROD_SUPPORT.getDisplayName())
                             .withRoleType(NON_PROD_SUPPORT)
                             .withAllApps(true)
                             .build()));
  }

  @Override
  public Account get(String accountId) {
    Account account = wingsPersistence.get(Account.class, accountId);
    notNullCheck("Account is null for the given id:" + accountId, account, USER);

    decryptLicenseInfo(singletonList(account));
    return account;
  }

  @Override
  public String getAccountStatus(String accountId) {
    LicenseInfo licenseInfo = dbCache.get(Account.class, accountId).getLicenseInfo();

    if (licenseInfo == null) {
      return AccountStatus.ACTIVE;
    }

    return licenseInfo.getAccountStatus();
  }

  private void decryptLicenseInfo(List<Account> accounts) {
    if (isEmpty(accounts)) {
      return;
    }

    accounts.forEach(account -> decryptLicenseInfo(account, false));
  }

  @Override
  public Account decryptLicenseInfo(Account account, boolean setExpiry) {
    if (account == null) {
      return null;
    }

    if (!featureFlagService.isEnabled(FeatureName.TRIAL_SUPPORT, account.getUuid())) {
      return account;
    }

    byte[] encryptedLicenseInfo = account.getEncryptedLicenseInfo();
    if (isNotEmpty(encryptedLicenseInfo)) {
      byte[] decryptedBytes = EncryptionUtils.decrypt(encryptedLicenseInfo, null);
      if (isNotEmpty(decryptedBytes)) {
        LicenseInfo licenseInfo = LicenseUtil.convertToObject(decryptedBytes, getDefaultTrialExpiryTime(), setExpiry);
        account.setLicenseInfo(licenseInfo);
      } else {
        logger.error("Error while decrypting license info. Deserialized object is not instance of LicenseInfo");
      }
    }

    return account;
  }

  public <T> List<T> descendingServices(Class<T> cls) {
    List<T> descendings = new ArrayList<>();

    for (Field field : AccountServiceImpl.class.getDeclaredFields()) {
      Object obj;
      try {
        obj = field.get(this);
        if (cls.isInstance(obj)) {
          T descending = (T) obj;
          descendings.add(descending);
        }
      } catch (IllegalAccessException e) {
        logger.error("", e);
      }
    }

    return descendings;
  }

  @Override
  public void delete(String accountId) {
    if (wingsPersistence.delete(Account.class, accountId)) {
      dbCache.invalidate(Account.class, accountId);
      executorService.submit(() -> {
        List<OwnedByAccount> services = descendingServices(OwnedByAccount.class);
        services.forEach(service -> service.deleteByAccountId(accountId));
      });
    }
  }

  @Override
  public boolean getTwoFactorEnforceInfo(String accountId) {
    Query<Account> getQuery = wingsPersistence.createQuery(Account.class).filter(ID_KEY, accountId);
    return getQuery.get().isTwoFactorAdminEnforced();
  }

  @Override
  public void updateTwoFactorEnforceInfo(String accountId, User user, boolean enabled) {
    Account account = get(accountId);
    account.setTwoFactorAdminEnforced(enabled);
    update(account);
  }

  @Override
  public String suggestAccountName(String accountName) {
    String suggestedAccountName = accountName;
    Random rand = new Random();
    do {
      Account res = wingsPersistence.get(
          Account.class, aPageRequest().addFilter("accountName", Operator.EQ, suggestedAccountName).build());
      if (res == null) {
        return suggestedAccountName;
      }
      suggestedAccountName = accountName + rand.nextInt(1000);
    } while (true);
  }

  @Override
  public boolean exists(String accountName) {
    return wingsPersistence.createQuery(Account.class).field(ACCOUNT_NAME_KEY).equal(accountName).getKey() != null;
  }

  @Override
  public boolean isAccountDeleted(String accountId) {
    return AccountStatus.DELETED.equals(getAccountStatus(accountId));
  }

  @Override
  public boolean isAccountExpired(String accountId) {
    Account account = dbCache.get(Account.class, accountId);
    Validator.notNullCheck("Invalid account with id: " + accountId, account);

    if (!featureFlagService.isEnabled(FeatureName.TRIAL_SUPPORT, accountId)) {
      return false;
    }

    String deployMode = System.getenv().get("DEPLOY_MODE");

    LicenseInfo licenseInfo = account.getLicenseInfo();

    if (licenseInfo == null) {
      return DeployMode.isOnPrem(deployMode);
    }

    String accountType = licenseInfo.getAccountType();
    String accountStatus = licenseInfo.getAccountStatus();

    if (isEmpty(accountType)) {
      throw new WingsException("Account type is null for account :" + accountId);
    }

    if (isEmpty(accountStatus)) {
      throw new WingsException("Account status is null for account :" + accountId);
    }

    // TODO this check would extend to PAID accounts as well
    if (AccountType.TRIAL.equals(accountType) && (System.currentTimeMillis() > licenseInfo.getExpiryTime())) {
      account.getLicenseInfo().setAccountStatus(AccountStatus.EXPIRED);
      updateAccountLicense(accountId, accountType, AccountStatus.EXPIRED, null, null, false);
      return true;
    }

    return false;
  }

  @Override
  public void updateAccountLicenseForOnPrem(String encryptedLicenseInfoBase64String) {
    try {
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        String msg = "Couldn't find license info";
        throw new WingsException(msg);
      }

      List<Account> accountList = listAllAccounts();
      if (accountList == null) {
        String msg = "Couldn't find any accounts in DB";
        throw new WingsException(msg);
      }

      accountList.forEach(account -> {
        if (!featureFlagService.isEnabled(FeatureName.TRIAL_SUPPORT, account.getUuid())) {
          return;
        }

        if (!account.getAccountName().equalsIgnoreCase("Global")) {
          byte[] encryptedLicenseInfo = Base64.getDecoder().decode(encryptedLicenseInfoBase64String.getBytes());
          byte[] encryptedLicenseInfoFromDB = account.getEncryptedLicenseInfo();

          boolean noLicenseInfoInDB = isEmpty(encryptedLicenseInfoFromDB);

          if (noLicenseInfoInDB || !Arrays.equals(encryptedLicenseInfo, encryptedLicenseInfoFromDB)) {
            account.setEncryptedLicenseInfo(encryptedLicenseInfo);
            decryptLicenseInfo(account, true);
            LicenseInfo licenseInfo = account.getLicenseInfo();
            if (licenseInfo != null) {
              updateAccountLicense(account.getUuid(), licenseInfo, null, false);
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
  public Account update(@Valid Account account) {
    wingsPersistence.update(account,
        wingsPersistence.createUpdateOperations(Account.class)
            .set("companyName", account.getCompanyName())
            .set("twoFactorAdminEnforced", account.isTwoFactorAdminEnforced())
            .set("authenticationMechanism", account.getAuthenticationMechanism()));
    dbCache.invalidate(Account.class, account.getUuid());
    return wingsPersistence.get(Account.class, account.getUuid());
  }

  @Override
  public Account updateAccountLicense(
      @NotEmpty String accountId, LicenseInfo licenseInfo, String salesContacts, boolean checkAndSetDefaultExpiry) {
    Account accountInDB = get(accountId);
    if (!featureFlagService.isEnabled(FeatureName.TRIAL_SUPPORT, accountId)) {
      logger.error("Feature is not enabled for account {}", accountId);
      return accountInDB;
    }

    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB);

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);

    byte[] encryptedLicenseInfo = getEncryptedLicenseInfo(licenseInfo, checkAndSetDefaultExpiry);

    updateOperations.set("encryptedLicenseInfo", encryptedLicenseInfo);

    if (isNotEmpty(salesContacts)) {
      updateOperations.set("salesContacts", salesContacts);
    }

    wingsPersistence.update(accountInDB, updateOperations);
    dbCache.invalidate(Account.class, accountId);
    return wingsPersistence.get(Account.class, accountId);
  }

  @Override
  public Account updateAccountLicense(@NotEmpty String accountId, String accountType, String accountStatus,
      String expiryInDaysString, String salesContacts, boolean checkAndSetDefaultExpiry) {
    Account accountInDB = get(accountId);
    if (!featureFlagService.isEnabled(FeatureName.TRIAL_SUPPORT, accountId)) {
      logger.error("Feature is not enabled for account {}", accountId);
      return accountInDB;
    }

    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB);

    LicenseInfo licenseInfo = accountInDB.getLicenseInfo();

    if (licenseInfo == null) {
      licenseInfo = new LicenseInfo();
    }

    if (isNotEmpty(expiryInDaysString)) {
      try {
        int expiryInDays = Integer.parseInt(expiryInDaysString);
        if (expiryInDays > 0) {
          licenseInfo.setExpiryTime(getExpiryTime(expiryInDays));
        }
      } catch (NumberFormatException ex) {
        throw new WingsException(GENERAL_ERROR, USER).addParam("message", "Invalid expiry time " + expiryInDaysString);
      }
    } else {
      if (checkAndSetDefaultExpiry) {
        licenseInfo.setExpiryTime(getDefaultTrialExpiryTime());
      }
    }

    if (AccountStatus.isValid(accountStatus)) {
      licenseInfo.setAccountStatus(accountStatus);
    }

    if (AccountType.isValid(accountType)) {
      licenseInfo.setAccountType(accountType);
    }

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);

    byte[] encryptedLicenseInfo = getEncryptedLicenseInfo(licenseInfo, checkAndSetDefaultExpiry);

    updateOperations.set("encryptedLicenseInfo", encryptedLicenseInfo);

    if (isNotEmpty(salesContacts)) {
      updateOperations.set("salesContacts", salesContacts);
    }

    wingsPersistence.update(accountInDB, updateOperations);
    dbCache.invalidate(Account.class, accountId);
    return wingsPersistence.get(Account.class, accountId);
  }

  @Override
  public String generateLicense(String accountType, String accountStatus, String expiryInDaysString) {
    LicenseInfo licenseInfo = new LicenseInfo();

    if (isNotEmpty(expiryInDaysString)) {
      try {
        int expiryInDays = Integer.parseInt(expiryInDaysString);
        if (expiryInDays > 0) {
          licenseInfo.setExpiryTime(getExpiryTime(expiryInDays));
        }
      } catch (NumberFormatException ex) {
        throw new WingsException(GENERAL_ERROR, USER).addParam("message", "Invalid expiry time " + expiryInDaysString);
      }
    }

    if (AccountStatus.isValid(accountStatus)) {
      licenseInfo.setAccountStatus(accountStatus);
    }

    if (AccountType.isValid(accountType)) {
      licenseInfo.setAccountType(accountType);
    }

    return encodeBase64(getEncryptedLicenseInfo(licenseInfo, true));
  }

  @Override
  public Account getByName(String companyName) {
    return wingsPersistence.createQuery(Account.class).filter("companyName", companyName).get();
  }

  @Override
  public List<Account> list(PageRequest<Account> pageRequest) {
    List<Account> accountList = wingsPersistence.query(Account.class, pageRequest, excludeAuthority).getResponse();
    decryptLicenseInfo(accountList);
    return accountList;
  }

  @Override
  public DelegateConfiguration getDelegateConfiguration(String accountId) {
    if (isAccountDeleted(accountId)) {
      throw new InvalidRequestException("Deleted AccountId: " + accountId);
    }

    List<Account> accounts = wingsPersistence.createQuery(Account.class, excludeAuthority)
                                 .field(Mapper.ID_KEY)
                                 .in(asList(accountId, GLOBAL_ACCOUNT_ID))
                                 .project("delegateConfiguration", true)
                                 .asList();

    Optional<Account> specificAccount =
        accounts.stream().filter(account -> StringUtils.equals(accountId, account.getUuid())).findFirst();

    if (!specificAccount.isPresent()) {
      throw new InvalidRequestException("Invalid AccountId: " + accountId);
    }

    if (specificAccount.get().getDelegateConfiguration() != null
        && !isBlank(specificAccount.get().getDelegateConfiguration().getWatcherVersion())) {
      return specificAccount.get().getDelegateConfiguration();
    }

    Optional<Account> fallbackAccount =
        accounts.stream().filter(account -> StringUtils.equals(GLOBAL_ACCOUNT_ID, account.getUuid())).findFirst();

    if (!fallbackAccount.isPresent()) {
      throw new InvalidRequestException("Global account ID is missing");
    }

    return fallbackAccount.get().getDelegateConfiguration();
  }

  @Override
  public List<Account> listAllAccounts() {
    List<Account> accountList = wingsPersistence.createQuery(Account.class).filter(APP_ID_KEY, GLOBAL_APP_ID).asList();
    decryptLicenseInfo(accountList);
    return accountList;
  }

  @Override
  public Account getByAccountName(String accountName) {
    return wingsPersistence.createQuery(Account.class).filter(ACCOUNT_NAME_KEY, accountName).get();
  }

  @Override
  public Account getAccountWithDefaults(String accountId) {
    Account account = wingsPersistence.createQuery(Account.class)
                          .project(ACCOUNT_NAME_KEY, true)
                          .project(COMPANY_NAME_KEY, true)
                          .filter(ID_KEY, accountId)
                          .get();
    if (account != null) {
      account.setDefaults(settingsService.listAccountDefaults(accountId));
    }
    return account;
  }

  @Override
  public Collection<FeatureFlag> getFeatureFlags(String accountId) {
    return Arrays.stream(FeatureName.values())
        .map(featureName
            -> FeatureFlag.builder()
                   .name(featureName.toString())
                   .enabled(featureFlagService.isEnabled(featureName, accountId))
                   .build())
        .collect(Collectors.toList());
  }

  private void createDefaultNotificationGroup(Account account, Role role) {
    String name = role.getRoleType().getDisplayName();
    // check if the notification group name exists
    List<NotificationGroup> existingGroups =
        notificationSetupService.listNotificationGroups(account.getUuid(), role, name);
    if (isEmpty(existingGroups)) {
      logger.info("Creating default {} notification group {} for account {}", ACCOUNT_ADMIN.getDisplayName(), name,
          account.getAccountName());
      NotificationGroup notificationGroup = aNotificationGroup()
                                                .withAppId(account.getAppId())
                                                .withAccountId(account.getUuid())
                                                .withRole(role)
                                                .withName(name)
                                                .withEditable(false)
                                                .withDefaultNotificationGroupForAccount(false)
                                                .build();

      // Reason we are setting withDefaultNotificationGroupForAccount(false), is We have also added a concept of default
      // group, where user can mark any editable notificationGroup as default (1 per account). This default group will
      // be selected for sending notifications in case of workflow execution. If no default group is set, then
      // automatically,  "ACCOUNT_ADMIN" notification group is selected. So for "ACCOUNT_ADMIN" isDefault = false, as we
      // want to first check for any explicitly set default notification group
      notificationSetupService.createNotificationGroup(notificationGroup);
    } else {
      logger.info("Default notification group already exists for role {} and account {}",
          ACCOUNT_ADMIN.getDisplayName(), account.getAccountName());
    }
  }

  private void createSystemAppContainers(Account account) {
    List<SystemCatalog> systemCatalogs =
        systemCatalogService.list(aPageRequest()
                                      .addFilter(SystemCatalog.APP_ID_KEY, EQ, GLOBAL_APP_ID)
                                      .addFilter("catalogType", EQ, APPSTACK)
                                      .build());
    logger.debug("Creating default system app containers  ");
    for (SystemCatalog systemCatalog : systemCatalogs) {
      AppContainer appContainer = anAppContainer()
                                      .withAccountId(account.getUuid())
                                      .withAppId(systemCatalog.getAppId())
                                      .withChecksum(systemCatalog.getChecksum())
                                      .withChecksumType(systemCatalog.getChecksumType())
                                      .withFamily(systemCatalog.getFamily())
                                      .withStackRootDirectory(systemCatalog.getStackRootDirectory())
                                      .withFileName(systemCatalog.getFileName())
                                      .withFileUuid(systemCatalog.getFileUuid())
                                      .withFileType(systemCatalog.getFileType())
                                      .withSize(systemCatalog.getSize())
                                      .withName(systemCatalog.getName())
                                      .withSystemCreated(true)
                                      .withDescription(systemCatalog.getNotes())
                                      .withHardened(systemCatalog.isHardened())
                                      .withVersion(systemCatalog.getVersion())
                                      .build();
      try {
        appContainerService.save(appContainer);
      } catch (Exception e) {
        logger.warn("Error while creating system app container " + appContainer, e);
      }
    }
  }

  public boolean isFeatureFlagEnabled(FeatureName featureName, String accountId) {
    return featureFlagService.isEnabled(featureName, accountId);
  }
}
