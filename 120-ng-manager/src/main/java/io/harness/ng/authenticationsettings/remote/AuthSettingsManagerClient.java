package io.harness.ng.authenticationsettings.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.security.authentication.SSOConfig;

import java.util.Set;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface AuthSettingsManagerClient {
  @GET("/api/ng/sso/get-access-management")
  Call<RestResponse<SSOConfig>> getAccountAccessManagementSettings(@Query("accountId") @NotEmpty String accountId);

  @GET("/api/ng/accounts/get-whitelisted-domains")
  Call<RestResponse<Set<String>>> getWhitelistedDomains(@Query("accountId") @NotEmpty String accountId);

  @GET("/api/ng/login-settings/username-password")
  Call<RestResponse<LoginSettings>> getUserNamePasswordSettings(@Query("accountId") @NotEmpty String accountId);

  @GET("/api/ng/accounts/two-factor-enabled")
  Call<RestResponse<Boolean>> twoFactorEnabled(@Query("accountId") @NotEmpty String accountId);
}