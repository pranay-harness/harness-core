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
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface AuthSettingsManagerClient {
  @GET("/api/ng/sso/access-management/{accountId}")
  Call<RestResponse<SSOConfig>> getAccountAccessManagementSettings(@Path("accountId") @NotEmpty String accountId);

  @GET("/api/ng/accounts/{accountId}/whitelisted-domains")
  Call<RestResponse<Set<String>>> getWhitelistedDomains(@Path("accountId") @NotEmpty String accountId);

  @GET("/api/ng/login-settings/username-password")
  Call<RestResponse<LoginSettings>> getUserNamePasswordSettings(@Query("accountId") @NotEmpty String accountId);

  @GET("/api/ng/accounts/two-factor-enabled/{accountId}")
  Call<RestResponse<Boolean>> twoFactorEnabled(@Path("accountId") @NotEmpty String accountId);
}