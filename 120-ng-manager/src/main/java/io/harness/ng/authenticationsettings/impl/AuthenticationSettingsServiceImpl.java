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
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsServiceImpl implements AuthenticationSettingsService {
  private AuthSettingsManagerClient managerClient;

  @Override
  public AuthenticationSettingsResponse getAuthenticationSettings(String accountIdentifier) {
    Set<String> whitelistedDomains = getResponse(managerClient.getWhitelistedDomains(accountIdentifier));
    log.info("Whitelisted domains: {}", whitelistedDomains);
    SSOConfig ssoConfig = getResponse(managerClient.getAccountAccessManagementSettings(accountIdentifier));

    List<NGAuthSettings> settingsList = buildAuthSettingsList(ssoConfig, accountIdentifier);
    log.info("settings list: {}", settingsList);

    boolean twoFactorEnabled = getResponse(managerClient.twoFactorEnabled(accountIdentifier));

    return AuthenticationSettingsResponse.builder()
        .whitelistedDomains(whitelistedDomains)
        .ngAuthSettings(settingsList)
        .twoFactorEnabled(twoFactorEnabled)
        .build();
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

  private RequestBody createPartFromString(String string) {
    if (string == null) {
      return null;
    }
    return RequestBody.create(MultipartBody.FORM, string);
  }

  @Override
  public SSOConfig uploadSAMLMetadata(@NotNull String accountId, @NotNull MultipartBody.Part inputStream,
      @NotNull String displayName, String groupMembershipAttr, @NotNull Boolean authorizationEnabled,
      String logoutUrl) {
    RequestBody displayNamePart = createPartFromString(displayName);
    RequestBody groupMembershipAttrPart = createPartFromString(groupMembershipAttr);
    RequestBody authorizationEnabledPart = createPartFromString(String.valueOf(authorizationEnabled));
    RequestBody logoutUrlPart = createPartFromString(logoutUrl);

    SSOConfig ssoConfig = getResponse(managerClient.uploadSAMLMetadata(
        accountId, inputStream, displayNamePart, groupMembershipAttrPart, authorizationEnabledPart, logoutUrlPart));
    return ssoConfig;
  }

  @Override
  public SSOConfig updateSAMLMetadata(@NotNull String accountId, @NotNull MultipartBody.Part inputStream,
      @NotNull String displayName, String groupMembershipAttr, @NotNull Boolean authorizationEnabled,
      String logoutUrl) {
    RequestBody displayNamePart = createPartFromString(displayName);
    RequestBody groupMembershipAttrPart = createPartFromString(groupMembershipAttr);
    RequestBody authorizationEnabledPart = createPartFromString(String.valueOf(authorizationEnabled));
    RequestBody logoutUrlPart = createPartFromString(logoutUrl);

    SSOConfig ssoConfig = getResponse(managerClient.updateSAMLMetadata(
        accountId, inputStream, displayNamePart, groupMembershipAttrPart, authorizationEnabledPart, logoutUrlPart));
    return ssoConfig;
  }

  @Override
  public SSOConfig deleteSAMLMetadata(@NotNull String accountIdentifier) {
    SSOConfig ssoConfig = getResponse(managerClient.deleteSAMLMetadata(accountIdentifier));
    return ssoConfig;
  }
}
