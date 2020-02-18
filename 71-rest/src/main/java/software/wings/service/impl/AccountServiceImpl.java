package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeAuthorityCount;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.AppContainer.Builder.anAppContainer;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.RoleType.ACCOUNT_ADMIN;
import static software.wings.beans.RoleType.APPLICATION_ADMIN;
import static software.wings.beans.RoleType.NON_PROD_SUPPORT;
import static software.wings.beans.RoleType.PROD_SUPPORT;
import static software.wings.beans.SystemCatalog.CatalogType.APPSTACK;
import static software.wings.utils.KubernetesConvention.getAccountIdentifier;
import static software.wings.utils.Misc.generateSecretKey;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.account.ProvisionStep;
import io.harness.account.ProvisionStep.ProvisionStepKeys;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.event.handler.impl.segment.SegmentGroupEventJobService;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.publisher.EventPublisher;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;
import io.harness.seeddata.SampleDataProviderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
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
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.AppContainer;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.LicenseInfo;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.Service;
import software.wings.beans.SubdomainUrl;
import software.wings.beans.SystemCatalog;
import software.wings.beans.TechStack;
import software.wings.beans.UrlInfo;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapSettings.LdapSettingsKeys;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOType;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerConditionType;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.features.GovernanceFeature;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.licensing.LicenseService;
import software.wings.resources.UserResource;
import software.wings.scheduler.AlertCheckJob;
import software.wings.scheduler.InstanceStatsCollectorJob;
import software.wings.scheduler.LdapGroupSyncJob;
import software.wings.scheduler.LimitVicinityCheckerJob;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.authentication.AccountSettingsResponse;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.OauthProviderType;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.impl.event.AccountEntityEvent;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertNotificationRuleService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.utils.CacheManager;
import software.wings.verification.CVConfiguration;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
@Singleton
@ValidateOnExecution
@Slf4j
public class AccountServiceImpl implements AccountService {
  private static final SecureRandom random = new SecureRandom();
  private static final int SIZE_PER_SERVICES_REQUEST = 25;
  private static final String UNLIMITED_PAGE_SIZE = "UNLIMITED";
  private static final String ILLEGAL_ACCOUNT_NAME_CHARACTERS = "[~!@#$%^*\\[\\]{}<>'\"/:;\\\\]";
  private static final int MAX_ACCOUNT_NAME_LENGTH = 50;
  private static final String WELCOME_EMAIL_TEMPLATE_NAME = "welcome_email";
  private static final String GENERATE_SAMPLE_DELEGATE_CURL_COMMAND_FORMAT_STRING =
      "curl -s -X POST -H 'content-type: application/json' "
      + "--url https://app.harness.io/gateway/gratis/api/webhooks/cmnhGRyXyBP5RJzz8Ae9QP7mqUATVotr7v2knjOf "
      + "-d '{\"application\":\"4qPkwP5dQI2JduECqGZpcg\","
      + "\"parameters\":{\"Environment\":\"%s\",\"delegate\":\"delegate\","
      + "\"account_id\":\"%s\",\"account_id_short\":\"%s\",\"account_secret\":\"%s\"}}'";
  private static final String SAMPLE_DELEGATE_NAME = "harness-sample-k8s-delegate";
  private static final String SAMPLE_DELEGATE_STATUS_ENDPOINT_FORMAT_STRING = "http://%s/account-%s.txt";
  private static final String DELIMITER = "####";
  @Inject protected AuthService authService;
  @Inject protected CacheManager cacheManager;
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
  @Inject private AlertNotificationRuleService notificationRuleService;
  @Inject private SampleDataProviderService sampleDataProviderService;
  @Inject private GovernanceConfigService governanceConfigService;
  @Inject private SSOSettingServiceImpl ssoSettingService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private UserService userService;
  @Inject private LoginSettingsService loginSettingsService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private DashboardStatisticsService dashboardStatisticsService;
  @Inject private UsageMetricsEventPublisher usageMetricsEventPublisher;
  @Inject private HarnessUserGroupService harnessUserGroupService;
  @Inject private EventPublisher eventPublisher;
  @Inject private SegmentGroupEventJobService segmentGroupEventJobService;
  @Inject private UserResource userResource;
  @Inject private AccountService accountService;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private GovernanceFeature governanceFeature;
  private Map<String, UrlInfo> techStackDocLinks;

  @Override
  public Account save(@Valid Account account, boolean fromDataGen) {
    // Validate if account/company name is valid.
    validateAccount(account);

    account.setCompanyName(account.getCompanyName().trim());
    account.setAccountName(account.getAccountName().trim());

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
      createDefaultAccountEntities(account, fromDataGen);
      // Schedule default account level jobs.
      scheduleAccountLevelJobs(account.getUuid());
    }

    publishAccountChangeEvent(account);

    logger.info("Successfully created account with id {}", account.getUuid());
    return account;
  }

  private void publishAccountChangeEvent(Account account) {
    EventData eventData = EventData.builder().eventInfo(new AccountEntityEvent(account)).build();
    eventPublisher.publishEvent(
        Event.builder().eventData(eventData).eventType(EventType.ACCOUNT_ENTITY_CHANGE).build());
  }

  private void validateAccount(Account account) {
    String companyName = account.getCompanyName();
    String accountName = account.getAccountName();
    if (isBlank(companyName)) {
      throw new InvalidRequestException("Company Name can't be empty.", USER);
    } else if (companyName.length() > MAX_ACCOUNT_NAME_LENGTH) {
      throw new InvalidRequestException(
          "Company Name exceeds " + MAX_ACCOUNT_NAME_LENGTH + " max-allowed characters.", USER);
    } else {
      String[] parts = companyName.split(ILLEGAL_ACCOUNT_NAME_CHARACTERS, 2);
      if (parts.length > 1) {
        throw new InvalidRequestException("Company Name '" + companyName + "' contains illegal characters.", USER);
      }
    }

    if (isBlank(accountName)) {
      throw new InvalidRequestException("Account Name can't be empty.", USER);
    } else if (accountName.length() > MAX_ACCOUNT_NAME_LENGTH) {
      throw new InvalidRequestException(
          "Account Name exceeds " + MAX_ACCOUNT_NAME_LENGTH + " max-allowed characters.", USER);
    } else {
      String[] parts = accountName.split(ILLEGAL_ACCOUNT_NAME_CHARACTERS, 2);
      if (parts.length > 1) {
        throw new InvalidRequestException("Account Name '" + accountName + "' contains illegal characters", USER);
      }
    }

    if (checkDuplicateAccountName(accountName)) {
      String suggestedAccountName = suggestAccountName(accountName);
      if (suggestedAccountName == null) {
        throw new InvalidRequestException("Account Name '" + accountName + "' already exists", USER);
      } else {
        account.setAccountName(suggestedAccountName);
      }
    }
  }

  private void createDefaultAccountEntities(Account account, boolean fromDataGen) {
    createDefaultRoles(account)
        .stream()
        .filter(role -> RoleType.ACCOUNT_ADMIN == role.getRoleType())
        .forEach(role -> createDefaultNotificationGroup(account, role));
    createSystemAppContainers(account);
    authHandler.createDefaultUserGroups(account);
    loginSettingsService.createDefaultLoginSettings(account);
    notificationRuleService.createDefaultRule(account.getUuid());

    executorService.submit(
        () -> templateGalleryService.copyHarnessTemplatesToAccountV2(account.getUuid(), account.getAccountName()));

    enableFeatureFlags(account, fromDataGen);
    sampleDataProviderService.createK8sV2SampleApp(account);
  }

  private void enableFeatureFlags(@NotNull Account account, boolean fromDataGen) {
    featureFlagService.enableAccount(FeatureName.INFRA_MAPPING_REFACTOR, account.getUuid());
    if (!fromDataGen) {
      featureFlagService.enableAccount(FeatureName.USE_PCF_CLI, account.getUuid());
    }
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
    Account account = getFromCacheWithFallback(accountId);
    if (account == null) {
      // Account was hard/physically deleted case
      return AccountStatus.DELETED;
    }
    LicenseInfo licenseInfo = account.getLicenseInfo();
    return licenseInfo == null ? AccountStatus.ACTIVE : licenseInfo.getAccountStatus();
  }

  private void decryptLicenseInfo(List<Account> accounts) {
    if (isEmpty(accounts)) {
      return;
    }

    accounts.forEach(account -> licenseService.decryptLicenseInfo(account, false));
  }

  @Override
  public Account getFromCacheWithFallback(String accountId) {
    Account account = dbCache.get(Account.class, accountId);
    if (account == null) {
      // Some false nulls have been observed. Verify by querying directly from db.
      account = wingsPersistence.get(Account.class, accountId);
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
  public boolean delete(String accountId) {
    logger.info("Started deleting account '{}'", accountId);
    Account account = wingsPersistence.get(Account.class, accountId);
    if (null == account) {
      return false;
    }

    dbCache.invalidate(Account.class, accountId);
    deleteQuartzJobs(accountId);
    executorService.submit(() -> {
      List<OwnedByAccount> services = descendingServices(OwnedByAccount.class);
      services.forEach(service -> service.deleteByAccountId(accountId));
    });
    logger.info("Successfully deleted account {}", accountId);
    return wingsPersistence.delete(account);
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

  /**
   * Takes a valid account name and checks database for duplicates, if duplicate exists appends
   * "-x" (where x is a random number between 0 and 1000) to the name and repeats the process until it generates a
   * unique account name
   * @param accountName user input account name
   * @return uniqueAccountName
   */
  @Override
  public String suggestAccountName(String accountName) {
    int count = 0;
    while (count < 20) {
      String newAccountName = accountName + "-" + (1000 + random.nextInt(9000));
      if (!checkDuplicateAccountName(newAccountName)) {
        return newAccountName;
      }
      count++;
    }
    return null;
  }

  /**
   * Takes an account name and performs a case-insensitive check on all account names in database
   * @param accountName account name
   * @return Returns true if duplicate is found else false
   */
  private Boolean checkDuplicateAccountName(@NotNull String accountName) {
    return wingsPersistence.createQuery(Account.class, excludeAuthority)
               .field(AccountKeys.accountName)
               .equalIgnoreCase(accountName)
               .get()
        != null;
  }

  @Override
  public boolean updateTechStacks(String accountId, Set<TechStack> techStacks) {
    Account accountInDB = get(accountId);
    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB, USER);

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);
    if (isEmpty(techStacks)) {
      updateOperations.unset("techStacks");
    } else {
      updateOperations.set("techStacks", techStacks);
    }
    wingsPersistence.update(accountInDB, updateOperations);
    dbCache.invalidate(Account.class, accountId);

    final List<User> usersOfAccount = userService.getUsersOfAccount(accountId);
    if (isNotEmpty(usersOfAccount)) {
      executorService.submit(() -> usersOfAccount.forEach(user -> sendWelcomeEmail(user, techStacks)));
    }
    eventPublishHelper.publishTechStackEvent(accountId, techStacks);
    return true;
  }

  @Override
  public void updateAccountEvents(String accountId, AccountEvent accountEvent) {
    Account accountInDB = get(accountId);
    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB, USER);
    Set<AccountEvent> accountEvents = Sets.newHashSet(accountEvent);
    Set<AccountEvent> existingEvents = accountInDB.getAccountEvents();
    if (isNotEmpty(existingEvents)) {
      accountEvents.addAll(existingEvents);
    }

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);
    if (isEmpty(accountEvents)) {
      updateOperations.unset("accountEvents");
    } else {
      updateOperations.set("accountEvents", accountEvents);
    }
    wingsPersistence.update(accountInDB, updateOperations);
    dbCache.invalidate(Account.class, accountId);
  }

  private UrlInfo getDocLink(TechStack techStack) {
    String category = techStack.getCategory();
    String technology = techStack.getTechnology();
    if (isEmpty(category)) {
      return null;
    }

    if (isEmpty(technology)) {
      return null;
    }

    String key =
        new StringBuilder(category.substring(0, category.indexOf(' '))).append("-").append(technology).toString();
    return techStackDocLinks.get(key);
  }

  private void sendWelcomeEmail(User user, Set<TechStack> techStackSet) {
    if (techStackDocLinks == null) {
      techStackDocLinks = mainConfiguration.getTechStackLinks();
    }

    try {
      List<String> deployPlatforms = new ArrayList<>();
      List<String> artifacts = new ArrayList<>();
      List<String> monitoringTools = new ArrayList<>();
      if (isNotEmpty(techStackSet)) {
        techStackSet.forEach(techStack -> {
          UrlInfo docLink = getDocLink(techStack);
          if (docLink != null) {
            switch (techStack.getCategory()) {
              case "Deployment Platforms":
                deployPlatforms.add(String.join(DELIMITER, docLink.getTitle(), docLink.getUrl()));
                break;
              case "Artifact Repositories":
                artifacts.add(String.join(DELIMITER, docLink.getTitle(), docLink.getUrl()));
                break;
              case "Monitoring And Logging":
                monitoringTools.add(String.join(DELIMITER, docLink.getTitle(), docLink.getUrl()));
                break;
              default:
                throw new WingsException("Unknown category " + techStack.getCategory());
            }
          }
        });
      }

      if (isEmpty(deployPlatforms)) {
        UrlInfo docLink =
            getDocLink(TechStack.builder().category("Deployment Platforms").technology("General").build());
        if (docLink != null) {
          deployPlatforms.add(String.join(DELIMITER, docLink.getTitle(), docLink.getUrl()));
        }
      }

      if (isEmpty(artifacts)) {
        UrlInfo docLink =
            getDocLink(TechStack.builder().category("Artifact Repositories").technology("General").build());
        if (docLink != null) {
          artifacts.add(String.join(DELIMITER, docLink.getTitle(), docLink.getUrl()));
        }
      }

      if (isEmpty(monitoringTools)) {
        UrlInfo docLink =
            getDocLink(TechStack.builder().category("Monitoring And Logging").technology("General").build());
        if (docLink != null) {
          monitoringTools.add(String.join(DELIMITER, docLink.getTitle(), docLink.getUrl()));
        }
      }

      Map<String, Object> model = new HashMap<>();
      model.put("name", user.getName());
      model.put("deploymentPlatforms", deployPlatforms);
      model.put("artifacts", artifacts);
      model.put("monitoringAndLoggingTools", monitoringTools);
      sendEmail(user.getEmail(), WELCOME_EMAIL_TEMPLATE_NAME, model);
    } catch (Exception e) {
      logger.error("Failed to send welcome email", e);
    }
  }

  private boolean sendEmail(String toEmail, String templateName, Map<String, Object> templateModel) {
    List<String> toList = new ArrayList<>();
    toList.add(toEmail);
    EmailData emailData =
        EmailData.builder().to(toList).templateName(templateName).templateModel(templateModel).build();
    emailData.setRetries(2);
    return emailNotificationService.send(emailData);
  }

  @Override
  public boolean exists(String accountName) {
    return wingsPersistence.createQuery(Account.class, excludeAuthority)
               .field(AccountKeys.accountName)
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
  public Boolean updateCloudCostEnabled(String accountId, boolean cloudCostEnabled) {
    Account account = get(accountId);
    account.setCloudCostEnabled(cloudCostEnabled);
    update(account);
    return true;
  }

  @Override
  public Account update(@Valid Account account) {
    licenseService.decryptLicenseInfo(account, false);

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class)
                                                     .set("companyName", account.getCompanyName())
                                                     .set("twoFactorAdminEnforced", account.isTwoFactorAdminEnforced())
                                                     .set(AccountKeys.oauthEnabled, account.isOauthEnabled())
                                                     .set(AccountKeys.cloudCostEnabled, account.isCloudCostEnabled())
                                                     .set("whitelistedDomains", account.getWhitelistedDomains());

    if (null != account.getLicenseInfo()) {
      updateOperations.set(AccountKeys.licenseInfo, account.getLicenseInfo());
    }

    if (account.getAuthenticationMechanism() != null) {
      updateOperations.set("authenticationMechanism", account.getAuthenticationMechanism());
    }

    wingsPersistence.update(account, updateOperations);
    dbCache.invalidate(Account.class, account.getUuid());
    Account updatedAccount = wingsPersistence.get(Account.class, account.getUuid());
    licenseService.decryptLicenseInfo(updatedAccount, false);

    publishAccountChangeEvent(updatedAccount);

    return updatedAccount;
  }

  @Override
  public Optional<Account> getOnPremAccount() {
    List<Account> accounts = listAccounts(Sets.newHashSet(GLOBAL_ACCOUNT_ID));
    return isNotEmpty(accounts) ? Optional.of(accounts.get(0)) : Optional.empty();
  }

  @Override
  public Account getByName(String companyName) {
    return wingsPersistence.createQuery(Account.class).filter(AccountKeys.companyName, companyName).get();
  }

  @Override
  public List<Account> list(PageRequest<Account> pageRequest) {
    List<Account> accountList = wingsPersistence.query(Account.class, pageRequest, excludeAuthority).getResponse();
    decryptLicenseInfo(accountList);
    return accountList;
  }

  @Override
  public List<Account> listAccounts(Set<String> excludedAccountIds) {
    Query<Account> query = wingsPersistence.createQuery(Account.class, excludeAuthority);
    if (isNotEmpty(excludedAccountIds)) {
      query.field("_id").notIn(excludedAccountIds);
    }

    List<Account> accountList = new ArrayList<>();
    try (HIterator<Account> iterator = new HIterator<>(query.fetch())) {
      for (Account account : iterator) {
        licenseService.decryptLicenseInfo(account, false);
        accountList.add(account);
      }
    }
    return accountList;
  }

  @Override
  public DelegateConfiguration getDelegateConfiguration(String accountId) {
    if (licenseService.isAccountDeleted(accountId)) {
      throw new InvalidRequestException("Deleted AccountId: " + accountId);
    }

    List<Account> accounts = wingsPersistence.createQuery(Account.class, excludeAuthorityCount)
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
    List<Account> accountList = wingsPersistence.createQuery(Account.class, excludeAuthorityCount)
                                    .filter(ApplicationKeys.appId, GLOBAL_APP_ID)
                                    .asList();
    decryptLicenseInfo(accountList);
    return accountList;
  }

  @Override
  public List<Account> listAllAccountWithDefaultsWithoutLicenseInfo() {
    return wingsPersistence.createQuery(Account.class, excludeAuthorityCount)
        .project(ID_KEY, true)
        .project(AccountKeys.accountName, true)
        .project(AccountKeys.companyName, true)
        .filter(ApplicationKeys.appId, GLOBAL_APP_ID)
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
    return wingsPersistence.createQuery(Account.class).filter(AccountKeys.accountName, accountName).get();
  }

  @Override
  public Account getAccountWithDefaults(String accountId) {
    Account account = wingsPersistence.createQuery(Account.class)
                          .project(AccountKeys.accountName, true)
                          .project(AccountKeys.companyName, true)
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
  public boolean disableAccount(String accountId, String migratedToClusterUrl) {
    Account account = get(accountId);
    updateMigratedToClusterUrl(account, migratedToClusterUrl);
    // Also need to prevent all existing users in the migration account from logging in after completion of migration.
    setUserStatusInAccount(accountId, false);
    return setAccountStatusInternal(account, AccountStatus.INACTIVE);
  }

  @Override
  public boolean enableAccount(String accountId) {
    Account account = get(accountId);
    setUserStatusInAccount(accountId, true);
    return setAccountStatusInternal(account, AccountStatus.ACTIVE);
  }

  private void updateMigratedToClusterUrl(Account account, String migratedToClusterUrl) {
    if (isNotEmpty(migratedToClusterUrl)) {
      wingsPersistence.update(account,
          wingsPersistence.createUpdateOperations(Account.class)
              .set(AccountKeys.migratedToClusterUrl, migratedToClusterUrl));
    }
  }

  private void setUserStatusInAccount(String accountId, boolean enable) {
    Query<User> query = wingsPersistence.createQuery(User.class, excludeAuthority).filter(UserKeys.accounts, accountId);
    int count = 0;
    try (HIterator<User> records = new HIterator<>(query.fetch())) {
      for (User user : records) {
        if (userService.canEnableOrDisable(user)) {
          user.setDisabled(!enable);
          wingsPersistence.save(user);
          userService.evictUserFromCache(user.getUuid());
          logger.info("User {} has been set to status disabled: {}", user.getEmail(), !enable);
          count++;
        }
      }
    }
    logger.info("{} users in account {} has been set to status disabled: {}", count, accountId, !enable);
  }

  @Override
  public boolean isAccountMigrated(String accountId) {
    Account account = getFromCacheWithFallback(accountId);
    if (account != null && account.getLicenseInfo() != null) {
      // Old account have empty 'licenseInfo' field in account. Need special handling of those account.
      return AccountStatus.INACTIVE.equals(account.getLicenseInfo().getAccountStatus())
          && isNotEmpty(account.getMigratedToClusterUrl());
    } else {
      return false;
    }
  }

  @Override
  public boolean isCommunityAccount(String accountId) {
    return getAccountType(accountId).map(AccountType::isCommunity).orElse(false);
  }

  @Override
  public String generateSampleDelegate(String accountId) {
    assertTrialAccount(accountId);

    if (isBlank(mainConfiguration.getSampleTargetEnv())) {
      String err = "Sample target env not configured";
      logger.warn(err);
      throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", err);
    }

    String script =
        String.format(GENERATE_SAMPLE_DELEGATE_CURL_COMMAND_FORMAT_STRING, mainConfiguration.getSampleTargetEnv(),
            accountId, getAccountIdentifier(accountId), getFromCache(accountId).getAccountKey());
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
      logger.error("Curl script to generate delegate returned non-zero exit code: {}", exitCode);
    } catch (IOException e) {
      logger.error("Error executing generate delegate curl command", e);
    } catch (InterruptedException e) {
      logger.info("Interrupted", e);
    } catch (TimeoutException e) {
      logger.info("Timed out", e);
    }

    String err = "Failed to provision";
    logger.warn(err);
    throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", err);
  }

  @Override
  public boolean sampleDelegateExists(String accountId) {
    assertTrialAccount(accountId);

    Key<Delegate> delegateKey = wingsPersistence.createQuery(Delegate.class)
                                    .filter(DelegateKeys.accountId, accountId)
                                    .filter(DelegateKeys.delegateName, SAMPLE_DELEGATE_NAME)
                                    .getKey();

    if (delegateKey == null) {
      return false;
    }

    return wingsPersistence.createQuery(DelegateConnection.class)
               .filter(DelegateConnectionKeys.accountId, accountId)
               .filter(DelegateConnectionKeys.delegateId, delegateKey.getId())
               .getKey()
        != null;
  }

  @Override
  public List<ProvisionStep> sampleDelegateProgress(String accountId) {
    assertTrialAccount(accountId);

    if (isBlank(mainConfiguration.getSampleTargetStatusHost())) {
      String err = "Sample target status host not configured";
      logger.warn(err);
      throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", err);
    }

    try {
      String url = String.format(SAMPLE_DELEGATE_STATUS_ENDPOINT_FORMAT_STRING,
          mainConfiguration.getSampleTargetStatusHost(), getAccountIdentifier(accountId));
      logger.info("Fetching delegate provisioning progress for account {} from {}", accountId, url);
      String result = Http.getResponseStringFromUrl(url, 30, 10).trim();
      if (isNotEmpty(result)) {
        logger.info("Provisioning progress for account {}: {}", accountId, result);
        if (result.contains("<title>404 Not Found</title>")) {
          return singletonList(ProvisionStep.builder().step("Provisioning Started").done(false).build());
        }
        List<ProvisionStep> steps = new ArrayList<>();
        for (JsonElement element : new JsonParser().parse(result).getAsJsonArray()) {
          JsonObject jsonObject = element.getAsJsonObject();
          steps.add(ProvisionStep.builder()
                        .step(jsonObject.get(ProvisionStepKeys.step).getAsString())
                        .done(jsonObject.get(ProvisionStepKeys.done).getAsBoolean())
                        .build());
        }
        return steps;
      }
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", String.format("Empty provisioning result for account %s", accountId));
    } catch (SocketTimeoutException e) {
      // Timed out for some reason. Return empty list to indicate unknown progress. UI can ignore and try again.
      logger.info("Timed out getting progress. Returning empty list.");
      return new ArrayList<>();
    } catch (IOException e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, e)
          .addParam("message",
              String.format("Exception in fetching delegate provisioning progress for account %s", accountId));
    }
  }

  private void assertTrialAccount(String accountId) {
    Account account = getFromCache(accountId);

    if (!AccountType.TRIAL.equals(account.getLicenseInfo().getAccountType())) {
      String err = "Not a trial account";
      logger.warn(err);
      throw new InvalidRequestException(err);
    }
  }

  @Override
  public boolean setAccountStatus(String accountId, String accountStatus) {
    return setAccountStatusInternal(get(accountId), accountStatus);
  }

  private boolean setAccountStatusInternal(Account account, String accountStatus) {
    String accountId = account.getUuid();
    if (!AccountStatus.isValid(accountStatus)) {
      throw new WingsException("Invalid account status: " + accountStatus, USER);
    }

    if (AccountStatus.INACTIVE.equals(accountStatus)) {
      updateDeploymentFreeze(accountId, true);
      deleteQuartzJobs(accountId);
    } else if (AccountStatus.ACTIVE.equals(accountStatus)) {
      updateDeploymentFreeze(accountId, false);
      scheduleQuartzJobs(accountId);
    }

    LicenseInfo newLicenseInfo = account.getLicenseInfo();
    newLicenseInfo.setAccountStatus(accountStatus);
    licenseService.updateAccountLicense(accountId, newLicenseInfo);

    logger.info("Updated status for account {}, new status is {}", accountId, accountStatus);
    return true;
  }

  private void updateDeploymentFreeze(String accountId, boolean deploymentFreezeStatus) {
    if (governanceFeature.isAvailableForAccount(accountId)) {
      setDeploymentFreeze(accountId, deploymentFreezeStatus);
    }
  }

  @Override
  public boolean setAuthenticationMechanism(String accountId, AuthenticationMechanism authenticationMechanism) {
    Account account = get(accountId);
    wingsPersistence.update(account,
        wingsPersistence.createUpdateOperations(Account.class)
            .set(AccountKeys.authenticationMechanism, authenticationMechanism));

    return true;
  }

  private void setDeploymentFreeze(String accountId, boolean freeze) {
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    if (governanceConfig == null) {
      governanceConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(freeze).build();
      wingsPersistence.save(governanceConfig);
    } else {
      governanceConfig.setDeploymentFreeze(freeze);
      governanceConfigService.upsert(accountId, governanceConfig);
    }
    logger.info("Set deployment freeze for account {} to: {}", accountId, freeze);
  }

  private void scheduleAccountLevelJobs(String accountId) {
    // Schedule default account level jobs.
    AlertCheckJob.add(jobScheduler, accountId);
    InstanceStatsCollectorJob.add(jobScheduler, accountId);
    LimitVicinityCheckerJob.add(jobScheduler, accountId);
    segmentGroupEventJobService.scheduleJob(accountId);
  }

  private void scheduleQuartzJobs(String accountId) {
    scheduleAccountLevelJobs(accountId);

    List<String> appIds = appService.getAppIdsByAccountId(accountId);

    // 2. ScheduledTriggerJob
    List<Trigger> triggers = getAllScheduledTriggersForAccount(appIds);
    for (Trigger trigger : triggers) {
      // Scheduled triggers is using the cron expression as trigger. No need to add special delay.
      ScheduledTriggerJob.add(jobScheduler, accountId, trigger.getAppId(), trigger.getUuid(), trigger);
    }

    // 3. LdapGroupSyncJob
    List<LdapSettings> ldapSettings = getAllLdapSettingsForAccount(accountId);
    for (LdapSettings ldapSetting : ldapSettings) {
      LdapGroupSyncJob.add(jobScheduler, accountId, ldapSetting.getUuid());
    }
    logger.info("Started all background quartz jobs for account {}", accountId);
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

    // 3. LdapGroupSyncJob
    List<LdapSettings> ldapSettings = getAllLdapSettingsForAccount(accountId);
    for (LdapSettings ldapSetting : ldapSettings) {
      LdapGroupSyncJob.delete(jobScheduler, ssoSettingService, accountId, ldapSetting.getUuid());
    }
    logger.info("Stopped all background quartz jobs for account {}", accountId);
  }

  private List<Trigger> getAllScheduledTriggersForAccount(List<String> appIds) {
    List<Trigger> triggers = new ArrayList<>();
    Query<Trigger> query = wingsPersistence.createQuery(Trigger.class).filter("appId in", appIds);
    try (HIterator<Trigger> iterator = new HIterator<>(query.fetch())) {
      for (Trigger trigger : iterator) {
        if (trigger.getCondition().getConditionType() == TriggerConditionType.SCHEDULED) {
          triggers.add(trigger);
        }
      }
    }

    return triggers;
  }

  private List<LdapSettings> getAllLdapSettingsForAccount(String accountId) {
    List<LdapSettings> ldapSettings = new ArrayList<>();
    Query<LdapSettings> query = wingsPersistence.createQuery(LdapSettings.class)
                                    .filter(LdapSettingsKeys.accountId, accountId)
                                    .filter("type", SSOType.LDAP);
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

  @Override
  public boolean isFeatureFlagEnabled(String featureName, String accountId) {
    for (FeatureName feature : FeatureName.values()) {
      if (feature.name().equals(featureName)) {
        return featureFlagService.isEnabled(FeatureName.valueOf(featureName), accountId);
      }
    }
    return false;
  }

  @Override
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

  @Override
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
        authService.getUserPermissionInfo(accountId, user, false).getAppPermissionMapInternal();

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
        wingsPersistence.createQuery(CVConfiguration.class, excludeAuthorityCount)
            .field("appId")
            .in(userAppPermissions.keySet())
            .asList();
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

  @Override
  public Set<String> getWhitelistedDomains(String accountId) {
    Account account = get(accountId);
    return account.getWhitelistedDomains();
  }

  @Override
  public Account updateWhitelistedDomains(String accountId, Set<String> whitelistedDomains) {
    Set<String> trimmedWhitelistedDomains =
        whitelistedDomains.stream().map(String::trim).filter(EmptyPredicate::isNotEmpty).collect(Collectors.toSet());
    UpdateOperations<Account> whitelistedDomainsUpdateOperations =
        wingsPersistence.createUpdateOperations(Account.class);
    setUnset(whitelistedDomainsUpdateOperations, AccountKeys.whitelistedDomains, trimmedWhitelistedDomains);
    wingsPersistence.update(wingsPersistence.createQuery(Account.class).filter(Mapper.ID_KEY, accountId),
        whitelistedDomainsUpdateOperations);
    return get(accountId);
  }

  @Override
  public Account updateAccountName(String accountId, String accountName, String companyName) {
    notNullCheck("Account name can not be set to null!", accountName);
    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);
    updateOperations.set(AccountKeys.accountName, accountName);
    if (isNotEmpty(companyName)) {
      updateOperations.set(AccountKeys.companyName, companyName);
    }
    wingsPersistence.update(
        wingsPersistence.createQuery(Account.class).filter(Mapper.ID_KEY, accountId), updateOperations);
    return get(accountId);
  }

  @Override
  public AccountSettingsResponse getAuthSettingsByAccountId(String accountId) {
    Account account = get(accountId);
    AuthenticationMechanism authenticationMechanism = account.getAuthenticationMechanism();
    if (authenticationMechanism == null) {
      authenticationMechanism = AuthenticationMechanism.USER_PASSWORD;
    }
    Set<String> whitelistedDomains = account.getWhitelistedDomains();
    OauthSettings oauthSettings = ssoSettingService.getOauthSettingsByAccountId(accountId);
    Set<OauthProviderType> oauthProviderTypes = oauthSettings == null ? null : oauthSettings.getAllowedProviders();
    return AccountSettingsResponse.builder()
        .authenticationMechanism(authenticationMechanism)
        .allowedDomains(whitelistedDomains)
        .oauthProviderTypes(oauthProviderTypes)
        .build();
  }

  @Override
  public boolean postCustomEvent(String accountId, AccountEvent accountEvent, boolean oneTimeOnly, boolean trialOnly) {
    eventPublishHelper.publishAccountEvent(accountId, accountEvent, oneTimeOnly, trialOnly);
    return true;
  }

  @Override
  public boolean isSSOEnabled(Account account) {
    return (account.getAuthenticationMechanism() != null)
        && (account.getAuthenticationMechanism() != AuthenticationMechanism.USER_PASSWORD);
  }

  /**
   * Checks whether the subdomain URL is taken by any other account or not
   * @param subdomainUrl Object of type SubdomainUrl
   * @return true if subdomain URL is duplicate otherwise false
   */
  public boolean checkDuplicateSubdomainUrl(SubdomainUrl subdomainUrl) {
    return wingsPersistence.createQuery(Account.class).filter(AccountKeys.subdomainUrl, subdomainUrl.getUrl()).get()
        != null;
  }

  /**
   * Takes a User ID and does the following checks before adding subdomainUrl to the account
   * Sanity check on Url provided
   * @param subdomainUrl subdomain URL object
   * @return boolean
   */
  @Override
  public boolean validateSubdomainUrl(SubdomainUrl subdomainUrl) {
    // Sanity check for subdomain URL
    String[] schemes = {"https"};
    UrlValidator urlValidator = new UrlValidator(schemes);
    return urlValidator.isValid(subdomainUrl.getUrl());
  }

  /**
   * Function to set subdomain Url of the account
   * @param account Account Object
   * @param subdomainUrl Subdomain URL
   */
  @Override
  public void setSubdomainUrl(Account account, SubdomainUrl subdomainUrl) {
    UpdateOperations<Account> updateOperation = wingsPersistence.createUpdateOperations(Account.class);
    updateOperation.set(AccountKeys.subdomainUrl, subdomainUrl.getUrl());
    wingsPersistence.update(account, updateOperation);
  }

  /**
   * Function to add subdomain URL
   * @param userId
   * @param accountId
   * @param subDomainUrl
   * @return boolean
   */
  @Override
  public Boolean addSubdomainUrl(String userId, String accountId, SubdomainUrl subDomainUrl) {
    Account account = get(accountId);
    if (!isBlank(account.getSubdomainUrl())) {
      throw new InvalidRequestException("Account already has subdomain URL. Updating it is not allowed");
    }

    // Check if the user is a part of Harness User Group
    if (!harnessUserGroupService.isHarnessSupportUser(userId)) {
      throw new UnauthorizedException("User is not authorized to add subdomain URL", USER);
    }

    // Check if URL is not duplicate
    if (checkDuplicateSubdomainUrl(subDomainUrl)) {
      throw new InvalidArgumentsException("Subdomain URL is already taken", USER);
    }

    // Check if URL is valid
    if (!validateSubdomainUrl(subDomainUrl)) {
      throw new InvalidArgumentsException("Subdomain URL provided is invalid", USER);
    }

    // Update Account with the given subdomainUrl
    setSubdomainUrl(get(accountId), subDomainUrl);
    return Boolean.TRUE;
  }
}
