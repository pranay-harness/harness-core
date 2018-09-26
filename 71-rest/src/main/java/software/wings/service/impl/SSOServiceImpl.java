package software.wings.service.impl;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapTestResponse;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SamlSettings;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.ldap.LdapDelegateService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class SSOServiceImpl implements SSOService {
  @Inject AccountService accountService;
  @Inject SSOSettingService ssoSettingService;
  @Inject SamlClientService samlClientService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;

  @Override
  public SSOConfig uploadSamlConfiguration(String accountId, InputStream inputStream, String displayName) {
    try {
      String fileAsString = org.apache.commons.io.IOUtils.toString(inputStream, Charset.defaultCharset());
      buildAndUploadSamlSettings(accountId, fileAsString, displayName);
      return getAccountAccessManagementSettings(accountId);
    } catch (SamlException | IOException | URISyntaxException e) {
      throw new WingsException(ErrorCode.INVALID_SAML_CONFIGURATION, e);
    }
  }

  @Override
  public SSOConfig deleteSamlConfiguration(String accountId) {
    ssoSettingService.deleteSamlSettings(accountId);
    return setAuthenticationMechanism(accountId, AuthenticationMechanism.USER_PASSWORD);
  }

  @Override
  public SSOConfig setAuthenticationMechanism(String accountId, AuthenticationMechanism mechanism) {
    Account account = accountService.get(accountId);
    account.setAuthenticationMechanism(mechanism);
    accountService.update(account);
    return getAccountAccessManagementSettings(accountId);
  }

  @Override
  public SSOConfig getAccountAccessManagementSettings(String accountId) {
    Account account = accountService.get(accountId);
    return SSOConfig.builder()
        .accountId(accountId)
        .authenticationMechanism(account.getAuthenticationMechanism())
        .ssoSettings(getSSOSettings(account))
        .build();
  }

  private List<SSOSettings> getSSOSettings(Account account) {
    List<SSOSettings> settings = new ArrayList<>();
    SamlSettings samlSettings = ssoSettingService.getSamlSettingsByAccountId(account.getUuid());
    if (samlSettings != null) {
      settings.add(samlSettings.getPublicSSOSettings());
    }
    LdapSettings ldapSettings = ssoSettingService.getLdapSettingsByAccountId(account.getUuid());
    if (ldapSettings != null) {
      settings.add(ldapSettings.getPublicSSOSettings());
    }
    return settings;
  }

  private SamlSettings buildAndUploadSamlSettings(String accountId, String fileAsString, String displayName)
      throws SamlException, URISyntaxException {
    SamlClient samlClient = samlClientService.getSamlClient(fileAsString);
    SamlSettings samlSettings = SamlSettings.builder()
                                    .metaDataFile(fileAsString)
                                    .url(samlClient.getIdentityProviderUrl())
                                    .accountId(accountId)
                                    .displayName(displayName)
                                    .origin(new URI(samlClient.getIdentityProviderUrl()).getHost())
                                    .build();
    return ssoSettingService.saveSamlSettings(samlSettings);
  }

  @Override
  public LdapSettings createLdapSettings(@NotNull LdapSettings settings) {
    return ssoSettingService.createLdapSettings(settings);
  }

  @Override
  public LdapSettings deleteLdapSettings(@NotBlank String accountId) {
    LdapSettings settings = ssoSettingService.deleteLdapSettings(accountId);
    if (accountService.get(accountId).getAuthenticationMechanism().equals(AuthenticationMechanism.LDAP)) {
      setAuthenticationMechanism(accountId, AuthenticationMechanism.USER_PASSWORD);
    }
    return settings;
  }

  @Override
  public LdapSettings updateLdapSettings(@NotNull LdapSettings settings) {
    return ssoSettingService.updateLdapSettings(settings);
  }

  @Override
  public LdapSettings getLdapSettings(@NotBlank String accountId) {
    return ssoSettingService.getLdapSettingsByAccountId(accountId);
  }

  @Override
  public LdapTestResponse validateLdapConnectionSettings(
      @NotNull LdapSettings ldapSettings, @NotBlank final String accountId) {
    boolean temporaryEncryption = !populateEncryptedFields(ldapSettings);
    ldapSettings.encryptFields(secretManager);
    EncryptedDataDetail encryptedDataDetail = ldapSettings.getEncryptedDataDetails(secretManager);
    try {
      SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
          .validateLdapConnectionSettings(ldapSettings, encryptedDataDetail);
    } finally {
      if (temporaryEncryption) {
        secretManager.deleteSecretUsingUuid(encryptedDataDetail.getEncryptedData().getUuid());
      }
    }
  }

  @Override
  public LdapTestResponse validateLdapUserSettings(
      @NotNull LdapSettings ldapSettings, @NotBlank final String accountId) {
    boolean temporaryEncryption = !populateEncryptedFields(ldapSettings);
    ldapSettings.encryptFields(secretManager);
    EncryptedDataDetail encryptedDataDetail = ldapSettings.getEncryptedDataDetails(secretManager);
    try {
      SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
          .validateLdapUserSettings(ldapSettings, encryptedDataDetail);
    } finally {
      if (temporaryEncryption) {
        secretManager.deleteSecretUsingUuid(encryptedDataDetail.getEncryptedData().getUuid());
      }
    }
  }

  @Override
  public LdapTestResponse validateLdapGroupSettings(
      @NotNull LdapSettings ldapSettings, @NotBlank final String accountId) {
    boolean temporaryEncryption = !populateEncryptedFields(ldapSettings);
    ldapSettings.encryptFields(secretManager);
    EncryptedDataDetail encryptedDataDetail = ldapSettings.getEncryptedDataDetails(secretManager);
    try {
      SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
          .validateLdapGroupSettings(ldapSettings, encryptedDataDetail);
    } finally {
      if (temporaryEncryption) {
        secretManager.deleteSecretUsingUuid(encryptedDataDetail.getEncryptedData().getUuid());
      }
    }
  }

  @Override
  public LdapResponse validateLdapAuthentication(LdapSettings ldapSettings, String identifier, String password) {
    EncryptedDataDetail settingsEncryptedDataDetail = ldapSettings.getEncryptedDataDetails(secretManager);

    String encryptedPassword = secretManager.encrypt(ldapSettings.getAccountId(), password, null);
    EncryptedDataDetail passwordEncryptedDataDetail =
        secretManager
            .encryptedDataDetails(ldapSettings.getAccountId(), LdapConstants.USER_PASSWORD_KEY, encryptedPassword)
            .get();
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(ldapSettings.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      return delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
          .authenticate(ldapSettings, settingsEncryptedDataDetail, identifier, passwordEncryptedDataDetail);
    } finally {
      secretManager.deleteSecretUsingUuid(passwordEncryptedDataDetail.getEncryptedData().getUuid());
    }
  }

  @Override
  public Collection<LdapGroupResponse> searchGroupsByName(@NotBlank String ldapSettingsId, @NotBlank String nameQuery) {
    LdapSettings ldapSettings = ssoSettingService.getLdapSettingsByUuid(ldapSettingsId);
    if (null == ldapSettings) {
      throw new InvalidRequestException("Invalid Ldap Settings ID.");
    }
    EncryptedDataDetail encryptedDataDetail = ldapSettings.getEncryptedDataDetails(secretManager);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(ldapSettings.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
        .searchGroupsByName(ldapSettings, encryptedDataDetail, nameQuery);
  }

  private boolean isExistingSetting(@NotNull LdapSettings settings) {
    if (EmptyPredicate.isNotEmpty(settings.getUuid())) {
      if (!ssoSettingService.isLdapSettingsPresent(settings.getUuid())) {
        throw new InvalidRequestException("Invalid Ldap Settings ID.");
      }
      return true;
    }
    return false;
  }

  private boolean populateEncryptedFields(@NotNull LdapSettings settings) {
    if (!isExistingSetting(settings)) {
      if (EmptyPredicate.isEmpty(settings.getConnectionSettings().getBindDN())) {
        return false;
      }
      if (settings.getConnectionSettings().getBindPassword().equals(LdapConstants.MASKED_STRING)) {
        throw new InvalidRequestException("Invalid password.");
      }
      return false;
    }
    if (!settings.getConnectionSettings().getBindPassword().equals(LdapConstants.MASKED_STRING)) {
      return false;
    }
    LdapSettings savedSettings = ssoSettingService.getLdapSettingsByUuid(settings.getUuid());
    settings.getConnectionSettings().setEncryptedBindPassword(
        savedSettings.getConnectionSettings().getEncryptedBindPassword());
    return true;
  }
}
