package software.wings.service.impl;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.SSO_PROVIDER_NOT_REACHABLE_NOTIFICATION;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.InvalidRequestException;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.Delegate;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.SSOSyncFailedAlert;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.beans.sso.SamlSettings;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.LdapGroupSyncJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.security.authentication.AuthenticationMechanism;
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
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class SSOSettingServiceImpl implements SSOSettingService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManager secretManager;
  @Inject private UserGroupService userGroupService;
  @Inject private AlertService alertService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private NotificationService notificationService;
  @Inject private DelegateService delegateService;
  @Inject private AccountService accountService;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Override
  public SamlSettings getSamlSettingsByIdpUrl(String idpUrl) {
    return wingsPersistence.createQuery(SamlSettings.class).field("url").equal(idpUrl).get();
  }

  public SamlSettings getSamlSettingsByAccountId(String accountId) {
    return wingsPersistence.createQuery(SamlSettings.class)
        .field("accountId")
        .equal(accountId)
        .field("type")
        .equal(SSOType.SAML)
        .get();
  }

  @Override
  public SamlSettings saveSamlSettings(SamlSettings settings) {
    SamlSettings queriedSettings = getSamlSettingsByAccountId(settings.getAccountId());
    if (queriedSettings != null) {
      queriedSettings.setUrl(settings.getUrl());
      queriedSettings.setMetaDataFile(settings.getMetaDataFile());
      queriedSettings.setDisplayName(settings.getDisplayName());
      queriedSettings.setOrigin(settings.getOrigin());
      return wingsPersistence.saveAndGet(SamlSettings.class, queriedSettings);
    } else {
      return wingsPersistence.saveAndGet(SamlSettings.class, settings);
    }
  }

  @Override
  public boolean deleteSamlSettings(String accountId) {
    SamlSettings samlSettings = getSamlSettingsByAccountId(accountId);
    if (samlSettings != null) {
      return wingsPersistence.delete(samlSettings);
    }
    return false;
  }

  @Override
  public SamlSettings getSamlSettingsByOrigin(String origin) {
    return wingsPersistence.createQuery(SamlSettings.class).field("origin").equal(origin).get();
  }

  @Override
  public Iterator<SamlSettings> getSamlSettingsIteratorByOrigin(@NotNull String origin) {
    return wingsPersistence.createQuery(SamlSettings.class).field("origin").equal(origin).iterator();
  }

  @Override
  public LdapSettings createLdapSettings(@NotNull LdapSettings settings) {
    if (getLdapSettingsByAccountId(settings.getAccountId()) != null) {
      throw new InvalidRequestException("Ldap settings already exist for this account.");
    }
    settings.encryptFields(secretManager);
    LdapSettings savedSettings = wingsPersistence.saveAndGet(LdapSettings.class, settings);
    LdapGroupSyncJob.add(jobScheduler, savedSettings.getAccountId(), savedSettings.getUuid());
    return savedSettings;
  }

  @Override
  public LdapSettings updateLdapSettings(@NotNull LdapSettings settings) {
    LdapSettings oldSettings = getLdapSettingsByAccountId(settings.getAccountId());
    if (oldSettings == null) {
      throw new InvalidRequestException("No existing Ldap settings found for this account.");
    }
    settings.getConnectionSettings().setEncryptedBindPassword(
        oldSettings.getConnectionSettings().getEncryptedBindPassword());
    oldSettings.setUrl(settings.getUrl());
    oldSettings.setDisplayName(settings.getDisplayName());
    oldSettings.setConnectionSettings(settings.getConnectionSettings());
    oldSettings.setUserSettings(settings.getUserSettings());
    oldSettings.setGroupSettings(settings.getGroupSettings());
    oldSettings.encryptFields(secretManager);
    LdapSettings savedSettings = wingsPersistence.saveAndGet(LdapSettings.class, oldSettings);
    LdapGroupSyncJob.add(jobScheduler, savedSettings.getAccountId(), savedSettings.getUuid());
    return savedSettings;
  }

  @Override
  public LdapSettings deleteLdapSettings(@NotBlank String accountId) {
    LdapSettings settings = getLdapSettingsByAccountId(accountId);
    if (settings == null) {
      throw new InvalidRequestException("No Ldap settings found for this account.");
    }
    if (userGroupService.existsLinkedUserGroup(settings.getUuid())) {
      throw new InvalidRequestException(
          "Deleting SSO provider with linked user groups is not allowed. Unlink the user groups first.");
    }
    secretManager.deleteSecretUsingUuid(settings.getConnectionSettings().getEncryptedBindPassword());
    wingsPersistence.delete(settings);
    LdapGroupSyncJob.delete(jobScheduler, this, accountId, settings.getUuid());
    return settings;
  }

  @Override
  public LdapSettings getLdapSettingsByAccountId(@NotBlank String accountId) {
    return wingsPersistence.createQuery(LdapSettings.class)
        .field("accountId")
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
    SSOSyncFailedAlert alertData =
        SSOSyncFailedAlert.builder().accountId(accountId).ssoId(ssoId).message(message).build();
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
        PageRequestBuilder.aPageRequest().addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId).build());
    String hostNamesForDelegates = "\n" + delegates.stream().map(Delegate::getHostName).collect(joining("\n"));

    String hostNamesForDelegatesHtml =
        "<br />" + delegates.stream().map(Delegate::getHostName).collect(joining("<br />"));

    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    NotificationRule notificationRule = aNotificationRule().withNotificationGroups(notificationGroups).build();

    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(GLOBAL_APP_ID)
            .withAccountId(accountId)
            .withNotificationTemplateId(SSO_PROVIDER_NOT_REACHABLE_NOTIFICATION.name())
            .withNotificationTemplateVariables(ImmutableMap.of("SSO_PROVIDER_NAME", settings.getDisplayName(),
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
    if (authenticationMechanism.equals(AuthenticationMechanism.LDAP)) {
      if (ssoSettings.getType().equals(SSOType.LDAP)) {
        return true;
      }
    }

    if (authenticationMechanism.equals(AuthenticationMechanism.SAML)) {
      if (ssoSettings.getType().equals(SSOType.SAML)) {
        return true;
      }
    }

    return false;
  }
}
