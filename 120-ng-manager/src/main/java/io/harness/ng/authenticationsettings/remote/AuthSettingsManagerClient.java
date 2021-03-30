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
  @GET("/api/sso/access-management/{accountId}")
  Call<RestResponse<SSOConfig>> getAccountAccessManagementSettings(@Path("accountId") @NotEmpty String accountId);

  @GET("/api/account/{accountId}/whitelisted-domains")
  Call<RestResponse<Set<String>>> getWhitelistedDomains(@Path("accountId") @NotEmpty String accountId);

  @GET("/api/loginSettings/get-login-settings")
  Call<RestResponse<LoginSettings>> getUserNamePasswordSettings(@Query("accountId") @NotEmpty String accountId);
}
