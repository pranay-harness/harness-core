package io.harness.ng.authenticationsettings.impl;

import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.dtos.mechanisms.LDAPSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.NGAuthSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.OAuthSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.SAMLSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.UsernamePasswordSettings;
import io.harness.ng.authenticationsettings.remote.AuthSettingsManagerClient;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.PasswordExpirationPolicy;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;
import software.wings.beans.loginSettings.UserLockoutPolicy;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.beans.sso.SamlSettings;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsServiceImpl implements AuthenticationSettingsService {
  private final AuthSettingsManagerClient managerClient;

  @Override
  public AuthenticationSettingsResponse getAuthenticationSettings(String accountIdentifier) {
    Set<String> whitelistedDomains = getResponse(managerClient.getWhitelistedDomains(accountIdentifier));
    log.info("Whitelisted domains for accountId {}: {}", accountIdentifier, whitelistedDomains);
    SSOConfig ssoConfig = getResponse(managerClient.getAccountAccessManagementSettings(accountIdentifier));

    List<NGAuthSettings> settingsList = buildAuthSettingsList(ssoConfig, accountIdentifier);
    log.info("NGAuthSettings list for accountId {}: {}", accountIdentifier, settingsList);

    boolean twoFactorEnabled = getResponse(managerClient.twoFactorEnabled(accountIdentifier));

    return AuthenticationSettingsResponse.builder()
        .whitelistedDomains(whitelistedDomains)
        .ngAuthSettings(settingsList)
        .authenticationMechanism(ssoConfig.getAuthenticationMechanism())
        .twoFactorEnabled(twoFactorEnabled)
        .build();
  }

  @Override
  public void updateOauthProviders(String accountId, OAuthSettings oAuthSettings) {
    getResponse(managerClient.uploadOauthSettings(accountId,
        OauthSettings.builder()
            .allowedProviders(oAuthSettings.getAllowedProviders())
            .filter(oAuthSettings.getFilter())
            .accountId(accountId)
            .build()));
  }

  @Override
  public void updateAuthMechanism(String accountId, AuthenticationMechanism authenticationMechanism) {
    getResponse(managerClient.updateAuthMechanism(accountId, authenticationMechanism));
  }

  @Override
  public void removeOauthMechanism(String accountId) {
    getResponse(managerClient.deleteOauthSettings(accountId));
  }

  @Override
  public void updatePasswordStrengthSettings(String accountId, PasswordStrengthPolicy passwordStrengthPolicy) {
    getResponse(managerClient.updatePasswordStrengthSettings(accountId, passwordStrengthPolicy));
  }

  @Override
  public void updateExpirationSettings(String accountId, PasswordExpirationPolicy passwordExpirationPolicy) {
    getResponse(managerClient.updateExpirationSettings(accountId, passwordExpirationPolicy));
  }

  @Override
  public void updateLockoutSettings(String accountId, UserLockoutPolicy userLockoutPolicy) {
    getResponse(managerClient.updateLockoutSettings(accountId, userLockoutPolicy));
  }

  private List<NGAuthSettings> buildAuthSettingsList(SSOConfig ssoConfig, String accountIdentifier) {
    List<NGAuthSettings> settingsList = new ArrayList<>();
    AuthenticationMechanism authenticationMechanism = ssoConfig.getAuthenticationMechanism();
    if (authenticationMechanism == AuthenticationMechanism.USER_PASSWORD) {
      LoginSettings loginSettings = getResponse(managerClient.getUserNamePasswordSettings(accountIdentifier));
      settingsList.add(UsernamePasswordSettings.builder()
                           .passwordExpirationPolicy(loginSettings.getPasswordExpirationPolicy())
                           .userLockoutPolicy(loginSettings.getUserLockoutPolicy())
                           .passwordStrengthPolicy(loginSettings.getPasswordStrengthPolicy())
                           .build());
      if (isOauthEnabled(ssoConfig)) {
        OauthSettings oAuthSettings = (OauthSettings) (ssoConfig.getSsoSettings().get(0));
        settingsList.add(OAuthSettings.builder()
                             .allowedProviders(oAuthSettings.getAllowedProviders())
                             .filter(oAuthSettings.getFilter())
                             .build());
      }
    } else if (authenticationMechanism == AuthenticationMechanism.OAUTH) {
      OauthSettings oAuthSettings = (OauthSettings) (ssoConfig.getSsoSettings().get(0));
      settingsList.add(OAuthSettings.builder()
                           .allowedProviders(oAuthSettings.getAllowedProviders())
                           .filter(oAuthSettings.getFilter())
                           .build());
    } else if (authenticationMechanism == AuthenticationMechanism.LDAP) {
      LdapSettings ldapSettings = (LdapSettings) (ssoConfig.getSsoSettings().get(0));
      settingsList.add(LDAPSettings.builder()
                           .connectionSettings(ldapSettings.getConnectionSettings())
                           .userSettingsList(ldapSettings.getUserSettingsList())
                           .groupSettingsList(ldapSettings.getGroupSettingsList())
                           .build());
    } else if (authenticationMechanism == AuthenticationMechanism.SAML) {
      SamlSettings samlSettings = (SamlSettings) (ssoConfig.getSsoSettings().get(0));
      settingsList.add(SAMLSettings.builder()
                           .groupMembershipAttr(samlSettings.getGroupMembershipAttr())
                           .logoutUrl(samlSettings.getLogoutUrl())
                           .origin(samlSettings.getOrigin())
                           .build());
    }
    return settingsList;
  }

  private boolean isOauthEnabled(SSOConfig ssoConfig) {
    List<SSOSettings> ssoSettings = ssoConfig.getSsoSettings();
    return !ssoSettings.isEmpty() && ssoSettings.get(0).getType().equals(SSOType.OAUTH);
  }
}
