package io.harness.ng.authenticationsettings.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;

@OwnedBy(HarnessTeam.PL)
public interface AuthenticationSettingsService {
  AuthenticationSettingsResponse getAuthenticationSettings(String accountId);
}
