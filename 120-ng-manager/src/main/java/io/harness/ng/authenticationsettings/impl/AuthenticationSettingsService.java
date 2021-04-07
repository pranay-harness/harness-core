package io.harness.ng.authenticationsettings.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.dtos.mechanisms.OAuthSettings;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.security.authentication.AuthenticationMechanism;

@OwnedBy(HarnessTeam.PL)
public interface AuthenticationSettingsService {
  AuthenticationSettingsResponse getAuthenticationSettings(String accountIdentifier);
  void updateOauthProviders(String accountId, OAuthSettings settings);
  void updateAuthMechanism(String accountId, AuthenticationMechanism authenticationMechanism);
  void removeOauthMechanism(String accountId);
  LoginSettings updateLoginSettings(String loginSettingsId, String accountIdentifier, LoginSettings loginSettings);
}
