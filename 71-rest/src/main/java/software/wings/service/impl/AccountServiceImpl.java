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
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.AppContainer.Builder.anAppContainer;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.Delegate.ACCOUNT_ID_KEY;
import static software.wings.beans.Delegate.DELEGATE_NAME_KEY;
import static software.wings.beans.DelegateConnection.DELEGATE_ID_KEY;
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
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.AppContainer;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.LicenseInfo;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.Service;
import software.wings.beans.SystemCatalog;
import software.wings.beans.User;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.SSOType;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerConditionType;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.scheduler.AlertCheckJob;
import software.wings.scheduler.InstanceStatsCollectorJob;
import software.wings.scheduler.InstanceSyncJob;
import software.wings.scheduler.LdapGroupSyncJob;
import software.wings.scheduler.LimitVicinityCheckerJob;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertNotificationRuleService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.utils.CacheHelper;
import software.wings.utils.KubernetesConvention;
import software.wings.verification.CVConfiguration;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
  private static final String GENERATE_SAMPLE_DELEGATE_CURL_COMMAND_FORMAT_STRING =
      "curl -X POST -H 'content-type: application/json' "
      + "--url https://app.harness.io/gateway/gratis/api/webhooks/cmnhGRyXyBP5RJzz8Ae9QP7mqUATVotr7v2knjOf "
      + "-d '{\"application\":\"4qPkwP5dQI2JduECqGZpcg\","
      + "\"parameters\":{\"Environment\":\"%s\",\"delegate\":\"delegate\","
      + "\"account_id\":\"%s\",\"account_id_short\":\"%s\",\"account_secret\":\"%s\",\"expired_after\":\"%d\"}}'";
  private static final long SAMPLE_DELEGATE_TTL = Duration.ofHours(1).getSeconds();
  private static final String SAMPLE_DELEGATE_NAME = "harness-sample-k8s-delegate";
  @Inject protected AuthService authService;
  @Inject protected CacheHelper cacheHelper;
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
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private GenericDbCache dbCache;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private SampleDataProviderService sampleDataProviderService;
  @Inject private AlertNotificationRuleService notificationRuleService;
  @Inject private GovernanceConfigService governanceConfigService;
  @Inject private SSOSettingServiceImpl ssoSettingService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private Clock clock;

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

    sampleDataProviderService.createK8sV2SampleApp(account);
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
  public void updateTwoFactorEnforceInfo(String accountId, boolean enabled) {
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
  public Optional<String> getAccountType(String accountId) {
    Account account = getFromCache(accountId);
    if (account == null) {
      logger.warn("accountId={} doesn't exist", accountId);
      return Optional.empty();
    }

    LicenseInfo licenseInfo = account.getLicenseInfo();
    if (null == licenseInfo) {
      logger.warn("License info not present for account. accountId={}", accountId);
      return Optional.empty();
    }

    String accountType = licenseInfo.getAccountType();
    if (!AccountType.isValid(accountType)) {
      logger.warn("Invalid account type. accountType={}, accountId={}", accountType, accountId);
      return Optional.empty();
    }

    return Optional.of(accountType);
  }

  @Override
  public Account update(@Valid Account account) {
    // Need to update the account status if the account status is not null.
    if (account.getLicenseInfo() != null) {
      LicenseInfo licenseInfo = account.getLicenseInfo();

      AuthenticationMechanism authMechanism = account.getAuthenticationMechanism();
      boolean shouldisableAuthMechanism = AuthenticationMechanism.DISABLED_FOR_LITE.contains(authMechanism);
      boolean isLite = AccountType.isLite(licenseInfo.getAccountType());

      if (isLite && shouldisableAuthMechanism) {
        logger.info("[LITE_DOWNGRADE] Auth Mechanism. current={} new={} accountId={}",
            account.getAuthenticationMechanism(), AuthenticationMechanism.USER_PASSWORD, account.getUuid());
        account.setAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD);
      }
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

  @Override
  public boolean isAccountLite(String accountId) {
    return getAccountType(accountId).map(AccountType::isLite).orElse(false);
  }

  @Override
  public String generateSampleDelegate(String accountId) {
    Account account = get(accountId);

    if (!AccountType.TRIAL.equals(account.getLicenseInfo().getAccountType())) {
      return "Not a trial account";
    }

    if (isBlank(mainConfiguration.getSampleTargetEnv())) {
      String err = "Sample target env not configured";
      logger.error(err);
      return err;
    }

    long expiresAtSeconds = clock.instant().plusSeconds(SAMPLE_DELEGATE_TTL).toEpochMilli() / 1000L;
    String script =
        String.format(GENERATE_SAMPLE_DELEGATE_CURL_COMMAND_FORMAT_STRING, mainConfiguration.getSampleTargetEnv(),
            accountId, KubernetesConvention.getAccountIdentifier(accountId), account.getAccountKey(), expiresAtSeconds);
    Logger scriptLogger = LoggerFactory.getLogger("generate-delegate-" + accountId);
    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                scriptLogger.info(line);
                                              }
                                            })
                                            .redirectError(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                scriptLogger.error(line);
                                              }
                                            });
      int exitCode = processExecutor.execute().getExitValue();
      if (exitCode == 0) {
        return "SUCCESS";
      }

    } catch (IOException e) {
      logger.error("Error executing generate delegate curl command", e);
    } catch (InterruptedException e) {
      logger.info("Interrupted", e);
    } catch (TimeoutException e) {
      logger.info("Timed out", e);
    }

    return "FAILED";
  }

  @Override
  public boolean sampleDelegateExists(String accountId) {
    Key<Delegate> delegateKey = wingsPersistence.createQuery(Delegate.class)
                                    .filter(ACCOUNT_ID_KEY, accountId)
                                    .filter(DELEGATE_NAME_KEY, SAMPLE_DELEGATE_NAME)
                                    .getKey();

    if (delegateKey == null) {
      return false;
    }

    return wingsPersistence.createQuery(DelegateConnection.class).filter(DELEGATE_ID_KEY, delegateKey.getId()).getKey()
        != null;
  }

  @Override
  public boolean setAccountStatus(String accountId, String accountStatus) {
    if (!AccountStatus.isValid(accountStatus)) {
      throw new WingsException("Invalid account status: " + accountStatus, USER);
    }

    Account account = get(accountId);

    LicenseInfo newLicenseInfo = account.getLicenseInfo();
    newLicenseInfo.setAccountStatus(accountStatus);
    licenseService.updateAccountLicense(accountId, newLicenseInfo);

    if (AccountStatus.MIGRATING.equals(accountStatus)) {
      // 1. To freeze the deployments once the account is set to status MIGRATING
      freezeDeployments(accountId);
      // 2. Stop all quartz jobs associated with this account
      deleteQuartzJobs(accountId);
    }

    return true;
  }

  private void freezeDeployments(String accountId) {
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    if (governanceConfig == null) {
      governanceConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(true).build();
      wingsPersistence.save(governanceConfig);
    } else {
      governanceConfig.setDeploymentFreeze(true);
      governanceConfigService.update(accountId, governanceConfig);
    }
    logger.info("Freezed deployment for account {}", accountId);
  }

  private void deleteQuartzJobs(String accountId) {
    // 1. Account level jobs
    AlertCheckJob.delete(jobScheduler, accountId);
    InstanceStatsCollectorJob.delete(jobScheduler, accountId);
    LimitVicinityCheckerJob.delete(jobScheduler, accountId);

    List<String> appIds = appService.getAppIdsByAccountId(accountId);

    // 2. ScheduledTriggerJob
    List<Trigger> triggers = getAllScheduledTriggersForAccount(appIds);
    for (Trigger trigger : triggers) {
      // Scheduled triggers is using the cron expression as trigger. No need to add special delay.
      ScheduledTriggerJob.delete(jobScheduler, trigger.getUuid());
    }

    // 3. InstanceSyncJob:
    for (String appId : appIds) {
      InstanceSyncJob.delete(jobScheduler, appId);
    }

    // 4. LdapGroupSyncJob
    List<LdapSettings> ldapSettings = getAllLdapSettingsForAccount(accountId);
    for (LdapSettings ldapSetting : ldapSettings) {
      LdapGroupSyncJob.delete(jobScheduler, ssoSettingService, accountId, ldapSetting.getUuid());
    }
    logger.info("Stopped all background quartz jobs for account {}", accountId);
  }

  private List<Trigger> getAllScheduledTriggersForAccount(List<String> appIds) {
    List<Trigger> triggers = new ArrayList<>();
    Query<Trigger> query = wingsPersistence.createQuery(Trigger.class).filter("appId in", appIds);
    Iterator<Trigger> iterator = query.iterator();
    while (iterator.hasNext()) {
      Trigger trigger = iterator.next();
      if (trigger.getCondition().getConditionType() == TriggerConditionType.SCHEDULED) {
        triggers.add(trigger);
      }
    }

    return triggers;
  }

  private List<LdapSettings> getAllLdapSettingsForAccount(String accountId) {
    List<LdapSettings> ldapSettings = new ArrayList<>();
    Query<LdapSettings> query =
        wingsPersistence.createQuery(LdapSettings.class).filter("accountId", accountId).filter("type", SSOType.LDAP);
    Iterator<LdapSettings> iterator = query.iterator();
    while (iterator.hasNext()) {
      ldapSettings.add(iterator.next());
    }

    return ldapSettings;
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

  public boolean isFeatureFlagEnabled(String featureName, String accountId) {
    for (FeatureName feature : FeatureName.values()) {
      if (feature.name().equals(featureName)) {
        return featureFlagService.isEnabled(FeatureName.valueOf(featureName), accountId);
      }
    }
    return false;
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

    // fetch the list of apps, services and environments that the user has permissions to.
    Map<String, AppPermissionSummary> userAppPermissions =
        authService.getUserPermissionInfo(accountId, user).getAppPermissionMapInternal();

    final List<String> services = new ArrayList<>();
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

    // Fetch and build he cvConfigs for the service/env that the user has permissions to, in parallel.
    final List<CVEnabledService> cvEnabledServices = Collections.synchronizedList(new ArrayList<>());
    if (serviceId != null) {
      // in thiscase we want to get the data only for this service.
      services.clear();
      services.add(serviceId);
    }

    List<CVConfiguration> cvConfigurationList =
        wingsPersistence.createQuery(CVConfiguration.class).field("appId").in(userAppPermissions.keySet()).asList();
    if (cvConfigurationList == null) {
      return null;
    }
    Map<String, List<CVConfiguration>> serviceCvConfigMap = new HashMap<>();
    cvConfigurationList.forEach(cvConfiguration -> {
      String serviceIdOfConfig = cvConfiguration.getServiceId();
      if (!cvConfiguration.isWorkflowConfig() && services.contains(serviceIdOfConfig)
          && allowedEnvs.contains(cvConfiguration.getEnvId())) {
        cvConfigurationService.fillInServiceAndConnectorNames(cvConfiguration);
        List<CVConfiguration> configList = new ArrayList<>();
        if (serviceCvConfigMap.containsKey(serviceIdOfConfig)) {
          configList = serviceCvConfigMap.get(serviceIdOfConfig);
        }
        if (cvConfiguration.isEnabled24x7()) {
          configList.add(cvConfiguration);
          serviceCvConfigMap.put(serviceIdOfConfig, configList);
        }
      }
    });

    serviceCvConfigMap.forEach((configServiceId, cvConfigList) -> {
      String appId = cvConfigList.get(0).getAppId();
      String appName = cvConfigList.get(0).getAppName();
      String serviceName = cvConfigList.get(0).getServiceName();

      cvEnabledServices.add(CVEnabledService.builder()
                                .service(Service.builder().uuid(configServiceId).name(serviceName).appId(appId).build())
                                .appName(appName)
                                .appId(appId)
                                .cvConfig(cvConfigList)
                                .build());
    });

    // Wrap into a pageResponse and return
    int totalSize = cvEnabledServices.size();
    List<CVEnabledService> returnList = cvEnabledServices;
    if (offset < returnList.size()) {
      int endIndex = Math.min(returnList.size(), offset + limit);
      returnList = returnList.subList(offset, endIndex);
    } else {
      returnList = new ArrayList<>();
    }

    if (isNotEmpty(returnList)) {
      return PageResponseBuilder.aPageResponse()
          .withResponse(returnList)
          .withOffset(String.valueOf(offset + returnList.size()))
          .withTotal(totalSize)
          .build();
    }
    return PageResponseBuilder.aPageResponse()
        .withResponse(new ArrayList<>())
        .withOffset(String.valueOf(offset + returnList.size()))
        .withTotal(totalSize)
        .build();
  }
}
