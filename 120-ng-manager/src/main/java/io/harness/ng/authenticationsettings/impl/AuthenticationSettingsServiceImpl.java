package io.harness.ng.authenticationsettings.impl;

import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.dtos.mechanisms.NGAuthSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.OAuthSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.UsernamePasswordSettings;
import io.harness.ng.authenticationsettings.remote.AuthSettingsManagerClient;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsServiceImpl implements AuthenticationSettingsService {
  private AuthSettingsManagerClient managerClient;
  @Override
  public AuthenticationSettingsResponse getAuthenticationSettings(String accountId) {
    Set<String> whitelistedDomains = getResponse(managerClient.getWhitelistedDomains(accountId));
    log.info("Whitelisted domains: {}", whitelistedDomains);
    SSOConfig ssoConfig = getResponse(managerClient.getAccountAccessManagementSettings(accountId));

    List<NGAuthSettings> settingsList = buildAuthSettingsList(ssoConfig, accountId);
    log.info("settings list: {}", settingsList);

    boolean twoFactorEnabled = getResponse(managerClient.twoFactorEnabled(accountId));

    return AuthenticationSettingsResponse.builder()
        .whitelistedDomains(whitelistedDomains)
        .ngAuthSettings(settingsList)
        .twoFactorEnabled(twoFactorEnabled)
        .build();
  }

  private List<NGAuthSettings> buildAuthSettingsList(SSOConfig ssoConfig, String accountId) {
    List<NGAuthSettings> settingsList = new ArrayList<>();
    AuthenticationMechanism authenticationMechanism = ssoConfig.getAuthenticationMechanism();
    if (authenticationMechanism == AuthenticationMechanism.USER_PASSWORD
        || authenticationMechanism == AuthenticationMechanism.OAUTH) {
      LoginSettings loginSettings = getResponse(managerClient.getUserNamePasswordSettings(accountId));
      settingsList.add(UsernamePasswordSettings.builder()
                           .passwordExpirationPolicy(loginSettings.getPasswordExpirationPolicy())
                           .userLockoutPolicy(loginSettings.getUserLockoutPolicy())
                           .passwordStrengthPolicy(loginSettings.getPasswordStrengthPolicy())
                           .build());
      if (!ssoConfig.getSsoSettings().isEmpty()) {
        SSOSettings ssoSettings = ssoConfig.getSsoSettings().get(0);
        if (ssoSettings.getType() == SSOType.OAUTH) {
          OauthSettings oAuthSettings = (OauthSettings) ssoSettings;
          settingsList.add(OAuthSettings.builder()
                               .allowedProviders(oAuthSettings.getAllowedProviders())
                               .filter(oAuthSettings.getFilter())
                               .build());
        }
      }
    } else if (authenticationMechanism == AuthenticationMechanism.LDAP) {
      //      settingsList.add();
    } else if (authenticationMechanism == AuthenticationMechanism.SAML) {
      //      settingsList.add();
    }
    return settingsList;
  }
}
