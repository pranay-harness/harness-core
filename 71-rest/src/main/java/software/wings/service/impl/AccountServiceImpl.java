package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
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
import static software.wings.utils.Misc.generateSecretKey;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.scheduler.PersistentScheduler;
import io.harness.seeddata.SampleDataProviderService;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.LicenseInfo;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.Service;
import software.wings.beans.SystemCatalog;
import software.wings.beans.User;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.scheduler.AlertCheckJob;
import software.wings.scheduler.InstanceStatsCollectorJob;
import software.wings.scheduler.LimitVicinityCheckerJob;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertNotificationRuleService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.utils.CacheHelper;
import software.wings.verification.CVConfiguration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
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
  private static final int SIZE_PER_SERVICES_REQUEST = 25;
  private static final String UNLIMITED_PAGE_SIZE = "UNLIMITED";
  private static final String ILLEGAL_ACCOUNT_NAME_CHARACTERS = "[~!@#$%^*\\[\\]{}<>'\"/:;\\\\]";
  private static final int MAX_ACCOUNT_NAME_LENGTH = 50;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private RoleService roleService;
  // DO NOT DELETE THIS, PRUNE logic needs it
  @Inject private UserGroupService userGroupService;
  // DO NOT DELETE THIS, PRUNE logic needs it
  @SuppressWarnings("unused") @Inject private InstanceService instanceService;
  @Inject private AuthHandler authHandler;
  @Inject private LicenseService licenseService;
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
  @Inject protected AuthService authService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject protected CacheHelper cacheHelper;
  @Inject private SampleDataProviderService sampleDataProviderService;
  @Inject private AlertNotificationRuleService notificationRuleService;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public Account save(@Valid Account account) {
    // Validate if account/company name is valid.
    validateAccount(account);

    if (isEmpty(account.getUuid())) {
      logger.info("Creating a new account '{}'.", account.getAccountName());
      account.setUuid(UUIDGenerator.generateUuid());
    } else {
      logger.info("Creating a new account '{}' with specified id '{}'.", account.getAccountName(), account.getUuid());
    }

    account.setAppId(GLOBAL_APP_ID);
    account.setAccountKey(generateSecretKey());
    licenseService.addLicenseInfo(account);

    wingsPersistence.save(account);

    // When an account is just created for import, no need to create default account entities.
    // As the import process will do all these instead.
    if (account.isForImport()) {
      logger.info("Creating the account '{}' for import only, no default account entities will be created",
          account.getAccountName());
    } else {
      createDefaultAccountEntities(account);
      // Schedule default account level jobs.
      AlertCheckJob.add(jobScheduler, account.getUuid());
      InstanceStatsCollectorJob.add(jobScheduler, account.getUuid());
      LimitVicinityCheckerJob.add(jobScheduler, account.getUuid());
    }

    logger.info("Successfully created account '{}' with id '{}'.", account.getAccountName(), account.getUuid());
    return account;
  }

  private void validateAccount(Account account) {
    String companyName = account.getCompanyName();
    String accountName = account.getAccountName();
    if (isBlank(companyName)) {
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", "Company Name can't be empty.");
    } else if (companyName.length() > MAX_ACCOUNT_NAME_LENGTH) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Company Name exceeds " + MAX_ACCOUNT_NAME_LENGTH + " max-allowed characters.");
    } else {
      String[] parts = companyName.split(ILLEGAL_ACCOUNT_NAME_CHARACTERS, 2);
      if (parts.length > 1) {
        throw new WingsException(GENERAL_ERROR, USER)
            .addParam("message", "Company Name '" + companyName + "' contains illegal characters.");
      }
    }

    if (isBlank(accountName)) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "Account Name can't be empty.");
    } else if (accountName.length() > MAX_ACCOUNT_NAME_LENGTH) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Account Name exceeds " + MAX_ACCOUNT_NAME_LENGTH + " max-allowed characters.");
    } else {
      String[] parts = accountName.split(ILLEGAL_ACCOUNT_NAME_CHARACTERS, 2);
      if (parts.length > 1) {
        throw new WingsException(GENERAL_ERROR, USER)
            .addParam("message", "Account Name '" + accountName + "' contains illegal characters");
      }
    }

    if (exists(account.getAccountName())) {
      throw new WingsException(GENERAL_ERROR)
          .addParam("message", "Account Name is already taken, please try a different one.");
    }
  }

  private void createDefaultAccountEntities(Account account) {
    createDefaultRoles(account)
        .stream()
        .filter(role -> RoleType.ACCOUNT_ADMIN.equals(role.getRoleType()))
        .forEach(role -> createDefaultNotificationGroup(account, role));
    createSystemAppContainers(account);
    authHandler.createDefaultUserGroups(account);
    notificationRuleService.createDefaultRule(account.getUuid());

    executorService.submit(
        () -> templateGalleryService.copyHarnessTemplatesToAccountV2(account.getUuid(), account.getAccountName()));

    sampleDataProviderService.createHarnessSampleApp(account);
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
    licenseService.decryptLicenseInfo(account, false);
    return account;
  }

  @Override
  public Account getFromCache(String accountId) {
    return dbCache.get(Account.class, accountId);
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

    accounts.forEach(account -> licenseService.decryptLicenseInfo(account, false));
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
  public boolean delete(String accountId) {
    logger.info("Started deleting account '{}'", accountId);
    boolean deleted = wingsPersistence.delete(Account.class, accountId);
    if (deleted) {
      dbCache.invalidate(Account.class, accountId);
      InstanceStatsCollectorJob.delete(jobScheduler, accountId);
      AlertCheckJob.delete(jobScheduler, accountId);
      LimitVicinityCheckerJob.delete(jobScheduler, accountId);
      executorService.submit(() -> {
        List<OwnedByAccount> services = descendingServices(OwnedByAccount.class);
        services.forEach(service -> service.deleteByAccountId(accountId));
      });
      //      refreshUsersForAccountDelete(accountId);
    }

    logger.info("Successfully deleting account '{}': {}", accountId, deleted);
    return deleted;
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
      Account res = wingsPersistence.createQuery(Account.class).filter(ACCOUNT_NAME_KEY, suggestedAccountName).get();
      if (res == null) {
        return suggestedAccountName;
      }
      suggestedAccountName = accountName + rand.nextInt(1000);
    } while (true);
  }

  @Override
  public boolean exists(String accountName) {
    return wingsPersistence.createQuery(Account.class, excludeAuthority)
               .field(ACCOUNT_NAME_KEY)
               .equal(accountName)
               .getKey()
        != null;
  }

  @Override
  public Account update(@Valid Account account) {
    // Need to update the account status if the account status is not null.
    if (account.getLicenseInfo() != null && account.getLicenseInfo().getAccountStatus() != null) {
      setAccountStatus(account.getUuid(), account.getLicenseInfo().getAccountStatus());
    }

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class)
                                                     .set("companyName", account.getCompanyName())
                                                     .set("twoFactorAdminEnforced", account.isTwoFactorAdminEnforced());
    if (account.getAuthenticationMechanism() != null) {
      updateOperations.set("authenticationMechanism", account.getAuthenticationMechanism());
    }
    if (isNotEmpty(account.getNewClusterUrl())) {
      updateOperations.set("newClusterUrl", account.getNewClusterUrl());
    }
    wingsPersistence.update(account, updateOperations);
    dbCache.invalidate(Account.class, account.getUuid());
    Account updatedAccount = wingsPersistence.get(Account.class, account.getUuid());
    decryptLicenseInfo(singletonList(updatedAccount));
    return updatedAccount;
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
    if (licenseService.isAccountDeleted(accountId)) {
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
  public List<Account> listAllAccountWithDefaultsWithoutLicenseInfo() {
    return wingsPersistence.createQuery(Account.class, excludeAuthority)
        .project(ID_KEY, true)
        .project(ACCOUNT_NAME_KEY, true)
        .project(COMPANY_NAME_KEY, true)
        .filter(APP_ID_KEY, GLOBAL_APP_ID)
        .asList();
  }

  @Override
  public PageResponse<Account> getAccounts(PageRequest pageRequest) {
    PageResponse<Account> responses = wingsPersistence.query(Account.class, pageRequest, excludeAuthority);
    List<Account> accounts = responses.getResponse();
    decryptLicenseInfo(accounts);
    return responses;
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

  @Override
  public boolean startAccountMigration(String accountId) {
    return setAccountStatus(accountId, AccountStatus.MIGRATING);
  }

  @Override
  public boolean completeAccountMigration(String accountId, String newClusterUrl) {
    Account account = get(accountId);
    account.setNewClusterUrl(newClusterUrl);
    update(account);

    return setAccountStatus(accountId, AccountStatus.MIGRATED);
  }

  private boolean setAccountStatus(String accountId, String accountStatus) {
    if (!AccountStatus.isValid(accountStatus)) {
      throw new WingsException("Invalid account status: " + accountStatus, USER);
    }

    Account account = get(accountId);

    LicenseInfo newLicenseInfo = account.getLicenseInfo();
    newLicenseInfo.setAccountStatus(accountStatus);
    licenseService.updateAccountLicense(accountId, newLicenseInfo);

    return true;
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

  public List<Service> getServicesBreadCrumb(String accountId, User user) {
    PageRequest<String> request = aPageRequest().withOffset("0").withLimit(UNLIMITED_PAGE_SIZE).build();
    PageResponse<CVEnabledService> response = getServices(accountId, user, request, null);
    if (response != null && isNotEmpty(response.getResponse())) {
      List<Service> serviceList = new ArrayList<>();
      for (CVEnabledService cvEnabledService : response.getResponse()) {
        serviceList.add(Service.builder()
                            .name(cvEnabledService.getService().getName())
                            .uuid(cvEnabledService.getService().getUuid())
                            .build());
      }
      return serviceList;
    }
    return new ArrayList<>();
  }

  public PageResponse<CVEnabledService> getServices(
      String accountId, User user, PageRequest<String> request, String serviceId) {
    if (user == null) {
      logger.info("User is null when requesting for Services info. Returning null");
    }

    int offset = Integer.parseInt(request.getOffset());
    if (isNotEmpty(request.getLimit()) && request.getLimit().equals(UNLIMITED_PAGE_SIZE)) {
      request.setLimit(String.valueOf(Integer.MAX_VALUE));
    }
    int limit = Integer.parseInt(request.getLimit() != null ? request.getLimit() : "0");
    limit = limit == 0 ? SIZE_PER_SERVICES_REQUEST : limit;

    Map<String, AppPermissionSummary> userAppPermissions =
        authService.getUserPermissionInfo(accountId, user).getAppPermissionMapInternal();

    List<String> services = new ArrayList<>();
    Set<EnvInfo> envInfoSet = new HashSet<>();
    for (AppPermissionSummary summary : userAppPermissions.values()) {
      if (isNotEmpty(summary.getServicePermissions())) {
        services.addAll(summary.getServicePermissions().get(Action.READ));
      }
      if (isNotEmpty(summary.getEnvPermissions())) {
        envInfoSet.addAll(summary.getEnvPermissions().get(Action.READ));
      }
    }

    Set<String> allowedEnvs = new HashSet<>();
    for (EnvInfo envInfo : envInfoSet) {
      allowedEnvs.add(envInfo.getEnvId());
    }

    List<CVEnabledService> cvEnabledServices = new ArrayList<>();

    List<Service> serviceList = wingsPersistence.createQuery(Service.class)
                                    .field("appId")
                                    .in(userAppPermissions.keySet())
                                    .field("_id")
                                    .in(services)
                                    .asList();

    List<Environment> envList =
        wingsPersistence.createQuery(Environment.class).field("appId").in(userAppPermissions.keySet()).asList();

    List<String> envIds = envList.stream().map(Environment::getUuid).collect(Collectors.toList());

    // keep only the environments that actually exist
    allowedEnvs.retainAll(envIds);

    for (Service service : serviceList) {
      if (serviceId != null && !serviceId.equals(service.getUuid())) {
        continue;
      }
      Application app = wingsPersistence.get(Application.class, service.getAppId());
      if (app == null) {
        continue;
      }

      List<CVConfiguration> cvConfigurationList = null;
      cvConfigurationList = wingsPersistence.createQuery(CVConfiguration.class)
                                .filter("serviceId", service.getUuid())
                                .field("envId")
                                .in(allowedEnvs)
                                .filter("appId", service.getAppId())
                                .asList();

      if (isNotEmpty(cvConfigurationList)) {
        for (CVConfiguration cvConfiguration : cvConfigurationList) {
          cvConfigurationService.fillInServiceAndConnectorNames(cvConfiguration);
        }
        cvEnabledServices.add(CVEnabledService.builder()
                                  .service(service)
                                  .appName(app.getName())
                                  .appId(app.getUuid())
                                  .cvConfig(cvConfigurationList)
                                  .build());
      }
    }

    // Wrap into a pageResponse and return
    int totalSize = cvEnabledServices.size();
    if (offset < cvEnabledServices.size()) {
      int endIndex = Math.min(cvEnabledServices.size(), offset + limit);
      cvEnabledServices = cvEnabledServices.subList(offset, endIndex);
    } else {
      cvEnabledServices = new ArrayList<>();
    }

    if (isNotEmpty(cvEnabledServices)) {
      return PageResponseBuilder.aPageResponse()
          .withResponse(cvEnabledServices)
          .withOffset(String.valueOf(offset + cvEnabledServices.size()))
          .withTotal(totalSize)
          .build();
    }
    return PageResponseBuilder.aPageResponse()
        .withResponse(new ArrayList<>())
        .withOffset(String.valueOf(offset + cvEnabledServices.size()))
        .withTotal(totalSize)
        .build();
  }
}
