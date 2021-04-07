package io.harness.ng.authenticationsettings.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import okhttp3.MultipartBody;
import software.wings.security.authentication.SSOConfig;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PL)
public interface AuthenticationSettingsService {
  AuthenticationSettingsResponse getAuthenticationSettings(String accountIdentifier);

  SSOConfig uploadSAMLMetadata(@NotNull String accountId, @NotNull MultipartBody.Part inputStream,
                               @NotNull String displayName, String groupMembershipAttr, @NotNull Boolean authorizationEnabled, String logoutUrl);
}
