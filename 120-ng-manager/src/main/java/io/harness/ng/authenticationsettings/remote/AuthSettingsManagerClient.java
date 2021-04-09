package io.harness.ng.authenticationsettings.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;

import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface AuthSettingsManagerClient {
  String API_PREFIX = "ng/";

  @GET(API_PREFIX + "sso/get-access-management")
  Call<RestResponse<SSOConfig>> getAccountAccessManagementSettings(@Query("accountId") @NotEmpty String accountId);

  @GET(API_PREFIX + "accounts/get-whitelisted-domains")
  Call<RestResponse<Set<String>>> getWhitelistedDomains(@Query("accountId") @NotEmpty String accountId);

  @GET(API_PREFIX + "login-settings/username-password")
  Call<RestResponse<LoginSettings>> getUserNamePasswordSettings(@Query("accountId") @NotEmpty String accountId);

  @GET(API_PREFIX + "accounts/two-factor-enabled")
  Call<RestResponse<Boolean>> twoFactorEnabled(@Query("accountId") @NotEmpty String accountId);

  @POST(API_PREFIX + "sso/oauth-settings-upload")
  Call<RestResponse<SSOConfig>> uploadOauthSettings(
      @Query("accountId") @NotEmpty String accountId, @Body OauthSettings oauthSettings);

  @DELETE(API_PREFIX + "sso/delete-oauth-settings")
  Call<RestResponse<SSOConfig>> deleteOauthSettings(@Query("accountId") String accountId);

  @PUT(API_PREFIX + "sso/assign-auth-mechanism")
  Call<RestResponse<SSOConfig>> updateAuthMechanism(@Query("accountId") @NotEmpty String accountId,
      @Query("authMechanism") AuthenticationMechanism authenticationMechanism);

  @PUT(API_PREFIX + "login-settings/{loginSettingsId}")
  Call<RestResponse<LoginSettings>> updateLoginSettings(@Path("loginSettingsId") String loginSettingsId,
      @Query("accountId") @NotEmpty String accountId, @Body @NotNull @Valid LoginSettings loginSettings);

  @Multipart
  @POST(API_PREFIX + "sso/saml-idp-metadata-upload")
  Call<RestResponse<SSOConfig>> uploadSAMLMetadata(@Query("accountId") String accountId,
      @Part MultipartBody.Part uploadedInputStream, @Part("displayName") RequestBody displayName,
      @Part("groupMembershipAttr") RequestBody groupMembershipAttr,
      @Part("authorizationEnabled") RequestBody authorizationEnabled, @Part("logoutUrl") RequestBody logoutUrl);

  @Multipart
  @PUT(API_PREFIX + "sso/saml-idp-metadata-upload")
  Call<RestResponse<SSOConfig>> updateSAMLMetadata(@Query("accountId") String accountId,
      @Part MultipartBody.Part uploadedInputStream, @Part("displayName") RequestBody displayName,
      @Part("groupMembershipAttr") RequestBody groupMembershipAttr,
      @Part("authorizationEnabled") RequestBody authorizationEnabled, @Part("logoutUrl") RequestBody logoutUrl);

  @DELETE(API_PREFIX + "sso/delete-saml-idp-metadata")
  Call<RestResponse<SSOConfig>> deleteSAMLMetadata(@Query("accountId") @NotEmpty String accountIdentifier);
}