package io.harness.ng.authenticationsettings.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.remote.AuthSettingsManagerClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
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
    //    Set<String> whitelistedDomains = getResponse(managerClient.getWhitelistedDomains(accountId));
    //
    //    SSOConfig ssoConfig = getResponse(managerClient.getAccountAccessManagementSettings(accountId));
    return AuthenticationSettingsResponse.builder()
        .whitelistedDomains(Collections.emptySet())
        .ngAuthSettings(Collections.emptyList())
        .build();
  }
}
