package software.wings.service.impl;

import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.SSO_PROVIDER_NOT_REACHABLE_NOTIFICATION;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Event;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.SSOSyncFailedAlert;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.beans.sso.SamlSettings;
import software.wings.dl.WingsPersistence;
import software.wings.features.LdapFeature;
import software.wings.features.SamlFeature;
import software.wings.features.api.GetAccountId;
import software.wings.features.api.RestrictedApi;
import software.wings.features.extractors.LdapSettingsAccountIdExtractor;
import software.wings.features.extractors.SamlSettingsAccountIdExtractor;
import software.wings.scheduler.LdapGroupSyncJob;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.OauthProviderType;
import software.wings.security.authentication.oauth.OauthOptions;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class SSOSettingServiceImpl implements SSOSettingService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManager secretManager;
  @Inject private UserGroupService userGroupService;
  @Inject private AlertService alertService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private NotificationService notificationService;
  @Inject private DelegateService delegateService;
  @Inject private AccountService accountService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private OauthOptions oauthOptions;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  static final int ONE_DAY = 86400000;

  @Override
  public SamlSettings getSamlSettingsByIdpUrl(String idpUrl) {
    return wingsPersistence.createQuery(SamlSettings.class).field("url").equal(idpUrl).get();
  }

  @Override
  public SamlSettings getSamlSettingsByAccountId(String accountId) {
    return wingsPersistence.createQuery(SamlSettings.class)
        .field(SamlSettings.ACCOUNT_ID_KEY)
        .equal(accountId)
        .field("type")
        .equal(SSOType.SAML)
        .get();
  }

  @Override
  public OauthSettings getOauthSettingsByAccountId(String accountId) {
    return wingsPersistence.createQuery(OauthSettings.class)
        .field("accountId")
        .equal(accountId)
        .field("type")
        .equal(SSOType.OAUTH)
        .get();
  }

  @Override
  @RestrictedApi(SamlFeature.class)
  public SamlSettings saveSamlSettings(@GetAccountId(SamlSettingsAccountIdExtractor.class) SamlSettings settings) {
    SamlSettings queriedSettings = getSamlSettingsByAccountId(settings.getAccountId());
    SamlSettings savedSettings;
    if (queriedSettings != null) {
      queriedSettings.setUrl(settings.getUrl());
      queriedSettings.setMetaDataFile(settings.getMetaDataFile());
      queriedSettings.setDisplayName(settings.getDisplayName());
      queriedSettings.setOrigin(settings.getOrigin());
      queriedSettings.setGroupMembershipAttr(settings.getGroupMembershipAttr());
      queriedSettings.setLogoutUrl(settings.getLogoutUrl());
      String ssoSettingUuid = wingsPersistence.save(queriedSettings);
      savedSettings = wingsPersistence.get(SamlSettings.class, ssoSettingUuid);
    } else {
      String ssoSettingUuid = wingsPersistence.save(settings);
      savedSettings = wingsPersistence.get(SamlSettings.class, ssoSettingUuid);
      eventPublishHelper.publishSSOEvent(settings.getAccountId());
    }
    auditServiceHelper.reportForAuditingUsingAccountId(settings.getAccountId(), null, settings, Event.Type.CREATE);
    logger.info("Auditing creation of SAML Settings for account={}", settings.getAccountId());

    return savedSettings;
  }

  @Override
  public OauthSettings saveOauthSettings(OauthSettings settings) {
    OauthSettings queriedSettings = getOauthSettingsByAccountId(settings.getAccountId());
    OauthSettings savedSettings;
    if (queriedSettings != null) {
      queriedSettings.setUrl(settings.getUrl());
      queriedSettings.setDisplayName(settings.getDisplayName());
      queriedSettings.setAllowedProviders(settings.getAllowedProviders());
      queriedSettings.setFilter(settings.getFilter());
      wingsPersistence.save(queriedSettings);
      savedSettings = wingsPersistence.get(OauthSettings.class, queriedSettings.getUuid());
    } else {
      String ssoSettingUuid = wingsPersistence.save(settings);
      savedSettings = wingsPersistence.get(OauthSettings.class, ssoSettingUuid);
      eventPublishHelper.publishSSOEvent(settings.getAccountId());
    }
    Account account = accountService.get(settings.getAccountId());
    accountService.update(account);
    auditServiceHelper.reportForAuditingUsingAccountId(account.getUuid(), null, settings, Event.Type.CREATE);
    logger.info("Auditing creation of OAUTH Settings for account={}", settings.getAccountId());
    return savedSettings;
  }

  @Override
  public OauthSettings updateOauthSettings(String accountId, String filter, Set<OauthProviderType> allowedProviders) {
    OauthSettings oldSettings = getOauthSettingsByAccountId(accountId);
    if (oldSettings == null) {
      throw new InvalidRequestException("No existing Oauth settings found for this account.");
    }
    oldSettings.setFilter(filter);
    oldSettings.setAllowedProviders(allowedProviders);
    oldSettings.setDisplayName(allowedProviders.stream().map(OauthProviderType::name).collect(Collectors.joining(",")));
    wingsPersistence.save(oldSettings);
    OauthSettings newSettings = wingsPersistence.get(OauthSettings.class, oldSettings.getUuid());
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, newSettings, Event.Type.UPDATE);
    logger.info("Auditing updation of OAUTH Settings for account={}", newSettings.getAccountId());
    return newSettings;
  }

  @Override
  public boolean deleteOauthSettings(String accountId) {
    OauthSettings settings = getOauthSettingsByAccountId(accountId);
    if (settings == null) {
      throw new InvalidRequestException("No Oauth settings found for this account.");
    }
    Account account = accountService.get(accountId);
    if (AuthenticationMechanism.OAUTH == account.getAuthenticationMechanism()) {
      throw new InvalidRequestException("Oauth settings cannot be deleted as authentication mechanism is OAUTH.");
    }
    account.setOauthEnabled(false);
    accountService.update(account);
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, settings);
    logger.info("Auditing deletion of OAUTH Settings for account={}", accountId);
    return wingsPersistence.delete(settings);
  }

  @Override
  public boolean deleteSamlSettings(String accountId) {
    SamlSettings samlSettings = getSamlSettingsByAccountId(accountId);
    if (samlSettings == null) {
      throw new InvalidRequestException("No Saml settings found for this account");
    }
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, samlSettings);
    logger.info("Auditing deletion of SAML Settings for account={}", accountId);
    return deleteSamlSettings(samlSettings);
  }

  @Override
  public boolean deleteSamlSettings(SamlSettings samlSettings) {
    if (userGroupService.existsLinkedUserGroup(samlSettings.getUuid())) {
      throw new InvalidRequestException(
          "Deleting Saml provider with linked user groups is not allowed. Unlink the user groups first.");
    }
    return wingsPersistence.delete(samlSettings);
  }

  @Override
  public SamlSettings getSamlSettingsByOrigin(String origin) {
    return wingsPersistence.createQuery(SamlSettings.class).field("origin").equal(origin).get();
  }

  @Override
  public Iterator<SamlSettings> getSamlSettingsIteratorByOrigin(@NotNull String origin) {
    return new HIterator(
        wingsPersistence.createQuery(SamlSettings.class, excludeAuthority).field("origin").equal(origin).fetch());
  }

  @Override
  @RestrictedApi(LdapFeature.class)
  public LdapSettings createLdapSettings(
      @GetAccountId(LdapSettingsAccountIdExtractor.class) @NotNull LdapSettings settings) {
    if (getLdapSettingsByAccountId(settings.getAccountId()) != null) {
      throw new InvalidRequestException("Ldap settings already exist for this account.");
    }
    settings.encryptFields(secretManager);
    LdapSettings savedSettings = wingsPersistence.saveAndGet(LdapSettings.class, settings);
    LdapGroupSyncJob.add(jobScheduler, savedSettings.getAccountId(), savedSettings.getUuid());
    auditServiceHelper.reportForAuditingUsingAccountId(settings.getAccountId(), null, settings, Event.Type.CREATE);
    logger.info("Auditing creation of LDAP Settings for account={}", settings.getAccountId());
    eventPublishHelper.publishSSOEvent(settings.getAccountId());
    return savedSettings;
  }

  @Override
  @RestrictedApi(LdapFeature.class)
  public LdapSettings updateLdapSettings(
      @GetAccountId(LdapSettingsAccountIdExtractor.class) @NotNull LdapSettings settings) {
    LdapSettings oldSettings = getLdapSettingsByAccountId(settings.getAccountId());
    if (oldSettings == null) {
      throw new InvalidRequestException("No existing Ldap settings found for this account.");
    }
    settings.getConnectionSettings().setEncryptedBindPassword(
        oldSettings.getConnectionSettings().getEncryptedBindPassword());
    oldSettings.setUrl(settings.getUrl());
    oldSettings.setDisplayName(settings.getDisplayName());
    oldSettings.setConnectionSettings(settings.getConnectionSettings());
    oldSettings.setUserSettingsList(settings.getUserSettingsList());
    oldSettings.setGroupSettingsList(settings.getGroupSettingsList());
    oldSettings.encryptFields(secretManager);
    LdapSettings savedSettings = wingsPersistence.saveAndGet(LdapSettings.class, oldSettings);
    auditServiceHelper.reportForAuditingUsingAccountId(
        settings.getAccountId(), oldSettings, savedSettings, Event.Type.UPDATE);
    logger.info("Auditing updation of LDAP for account={}", savedSettings.getAccountId());
    LdapGroupSyncJob.add(jobScheduler, savedSettings.getAccountId(), savedSettings.getUuid());
    return savedSettings;
  }

  @Override
  public LdapSettings deleteLdapSettings(@NotBlank String accountId) {
    LdapSettings settings = getLdapSettingsByAccountId(accountId);
    if (settings == null) {
      throw new InvalidRequestException("No Ldap settings found for this account.");
    }
    return deleteLdapSettings(settings);
  }

  @Override
  public LdapSettings deleteLdapSettings(@NotNull LdapSettings settings) {
    if (userGroupService.existsLinkedUserGroup(settings.getUuid())) {
      throw new InvalidRequestException(
          "Deleting SSO provider with linked user groups is not allowed. Unlink the user groups first.");
    }
    secretManager.deleteSecretUsingUuid(settings.getConnectionSettings().getEncryptedBindPassword());
    wingsPersistence.delete(settings);
    LdapGroupSyncJob.delete(jobScheduler, this, settings.getAccountId(), settings.getUuid());
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(settings.getAccountId(), settings);
    logger.info("Auditing deletion of LDAP Settings for account={}", settings.getAccountId());
    return settings;
  }

  @Override
  public LdapSettings getLdapSettingsByAccountId(@NotBlank String accountId) {
    return wingsPersistence.createQuery(LdapSettings.class)
        .field(LdapSettings.ACCOUNT_ID_KEY)
        .equal(accountId)
        .field("type")
        .equal(SSOType.LDAP)
        .get();
  }

  @Override
  public LdapSettings getLdapSettingsByUuid(@NotBlank String uuid) {
    return wingsPersistence.createQuery(LdapSettings.class)
        .field("uuid")
        .equal(uuid)
        .field("type")
        .equal(SSOType.LDAP)
        .get();
  }

  @Override
  public boolean isLdapSettingsPresent(@NotBlank String uuid) {
    return 0
        != wingsPersistence.createQuery(LdapSettings.class)
               .field("uuid")
               .equal(uuid)
               .field("type")
               .equal(SSOType.LDAP)
               .count();
  }

  @Override
  public SSOSettings getSsoSettings(String uuid) {
    return wingsPersistence.createQuery(SSOSettings.class).field("uuid").equal(uuid).get();
  }

  @Override
  public void raiseSyncFailureAlert(String accountId, String ssoId, String message) {
    // We will send the alert message every 24 hours
    SSOSyncFailedAlert alertData =
        SSOSyncFailedAlert.builder().accountId(accountId).ssoId(ssoId).message(message).build();
    Optional<Alert> existingAlert =
        alertService.findExistingAlert(accountId, GLOBAL_APP_ID, AlertType.USERGROUP_SYNC_FAILED, alertData);
    if (existingAlert.isPresent()) {
      Alert alert = existingAlert.get();
      long lastUpdatedAt = alert.getLastUpdatedAt();
      // If the previous alert happened within 24 hours, then we skip that alert
      if (System.currentTimeMillis() - lastUpdatedAt < ONE_DAY) {
        return;
      }
    }
    closeSyncFailureAlertIfOpen(accountId, ssoId);
    alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.USERGROUP_SYNC_FAILED, alertData);
  }

  @Override
  public void closeSyncFailureAlertIfOpen(String accountId, String ssoId) {
    SSOSyncFailedAlert alertData = SSOSyncFailedAlert.builder().accountId(accountId).ssoId(ssoId).build();
    alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.USERGROUP_SYNC_FAILED, alertData);
  }

  @Override
  public void sendSSONotReachableNotification(String accountId, SSOSettings settings) {
    List<Delegate> delegates = delegateService.list(
        PageRequestBuilder.aPageRequest().addFilter(DelegateKeys.accountId, Operator.EQ, accountId).build());
    String hostNamesForDelegates = "\n" + delegates.stream().map(Delegate::getHostName).collect(joining("\n"));

    String hostNamesForDelegatesHtml =
        "<br />" + delegates.stream().map(Delegate::getHostName).collect(joining("<br />"));

    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    NotificationRule notificationRule = aNotificationRule().withNotificationGroups(notificationGroups).build();

    notificationService.sendNotificationAsync(
        InformationNotification.builder()
            .appId(GLOBAL_APP_ID)
            .accountId(accountId)
            .notificationTemplateId(SSO_PROVIDER_NOT_REACHABLE_NOTIFICATION.name())
            .notificationTemplateVariables(ImmutableMap.of("SSO_PROVIDER_NAME", settings.getDisplayName(),
                "SSO_PROVIDER_TYPE", settings.getType().name(), "SSO_PROVIDER_URL", settings.getUrl(), "DELEGATE_HOSTS",
                hostNamesForDelegates, "DELEGATE_HOSTS_HTML", hostNamesForDelegatesHtml))
            .build(),
        singletonList(notificationRule));
  }

  @Override
  public boolean isDefault(String accountId, String ssoId) {
    SSOSettings ssoSettings = getSsoSettings(ssoId);
    if (null == ssoSettings) {
      return false;
    }

    AuthenticationMechanism authenticationMechanism = accountService.get(accountId).getAuthenticationMechanism();
    if (authenticationMechanism == AuthenticationMechanism.LDAP) {
      if (ssoSettings.getType() == SSOType.LDAP) {
        return true;
      }
    }

    if (authenticationMechanism == AuthenticationMechanism.SAML) {
      if (ssoSettings.getType() == SSOType.SAML) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(SSOSettings.class)
                                .disableValidation()
                                .filter(SSOSettings.ACCOUNT_ID_KEY, accountId));
  }

  @Override
  public List<SSOSettings> getAllSsoSettings(String accountId) {
    return wingsPersistence.createQuery(SSOSettings.class)
        .disableValidation()
        .filter(SSOSettings.ACCOUNT_ID_KEY, accountId)
        .asList();
  }
}
