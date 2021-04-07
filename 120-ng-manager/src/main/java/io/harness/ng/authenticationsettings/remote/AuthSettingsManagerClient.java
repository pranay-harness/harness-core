package io.harness.ng.authenticationsettings.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.PasswordExpirationPolicy;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;
import software.wings.beans.loginSettings.UserLockoutPolicy;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;

import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface AuthSettingsManagerClient {
  @GET("/api/ng/sso/get-access-management")
  Call<RestResponse<SSOConfig>> getAccountAccessManagementSettings(@Query("accountId") @NotEmpty String accountId);

  @GET("/api/ng/accounts/get-whitelisted-domains")
  Call<RestResponse<Set<String>>> getWhitelistedDomains(@Query("accountId") @NotEmpty String accountId);

  @GET("/api/ng/login-settings/username-password")
  Call<RestResponse<LoginSettings>> getUserNamePasswordSettings(@Query("accountId") @NotEmpty String accountId);

  @PUT("/api/ng/login-settings/username-password/update-password-strength-settings")
  Call<RestResponse<LoginSettings>> updatePasswordStrengthSettings(@Query("accountId") @NotEmpty String accountId,
      @Body @NotNull @Valid PasswordStrengthPolicy passwordStrengthPolicy);

  @PUT("/api/ng/login-settings/username-password/update-expiration-settings")
  Call<RestResponse<LoginSettings>> updateExpirationSettings(@Query("accountId") @NotEmpty String accountId,
      @Body @NotNull @Valid PasswordExpirationPolicy passwordExpirationPolicy);

  @PUT("/api/ng/login-settings/username-password/update-lockout-settings")
  Call<RestResponse<LoginSettings>> updateLockoutSettings(
      @Query("accountId") @NotEmpty String accountId, @Body @NotNull @Valid UserLockoutPolicy userLockoutPolicy);

  @GET("/api/ng/accounts/two-factor-enabled")
  Call<RestResponse<Boolean>> twoFactorEnabled(@Query("accountId") @NotEmpty String accountId);

  @POST("/api/ng/sso/oauth-settings-upload")
  Call<RestResponse<SSOConfig>> uploadOauthSettings(
      @Query("accountId") @NotEmpty String accountId, @Body OauthSettings oauthSettings);

  @DELETE("/api/ng/sso/delete-oauth-settings")
  Call<RestResponse<SSOConfig>> deleteOauthSettings(@Query("accountId") String accountId);

  @PUT("/api/ng/sso/assign-auth-mechanism")
  Call<RestResponse<SSOConfig>> updateAuthMechanism(@Query("accountId") @NotEmpty String accountId,
      @Query("authMechanism") AuthenticationMechanism authenticationMechanism);
}