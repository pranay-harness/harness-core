package io.harness.ng.authenticationsettings.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.*;
import software.wings.beans.loginSettings.LoginSettings;
import software.wings.security.authentication.SSOConfig;

import java.util.Set;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;

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

  @Multipart
  @POST("/api/ng/sso/saml-idp-metadata-upload")
  Call<RestResponse<SSOConfig>> uploadSAMLMetadata(@Query("accountId") String accountId,
                                                   @Part MultipartBody.Part uploadedInputStream, @Part("displayName") RequestBody displayName,
                                                   @Part("groupMembershipAttr") RequestBody groupMembershipAttr,
                                                   @Part("authorizationEnabled") RequestBody authorizationEnabled, @Part("logoutUrl") RequestBody logoutUrl);
}