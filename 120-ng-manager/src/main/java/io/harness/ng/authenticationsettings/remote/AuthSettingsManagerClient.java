package io.harness.ng.authenticationsettings.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.security.authentication.SSOConfig;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.bouncycastle.cert.ocsp.Req;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.*;

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

  @Multipart
  @POST("/api/ng/sso/saml-idp-metadata-upload")
  Call<RestResponse<SSOConfig>> uploadSAMLMetadata(@Query("accountId") String accountId,
      @Part MultipartBody.Part uploadedInputStream, @Part("displayName") RequestBody displayName,
      @Part("groupMembershipAttr") RequestBody groupMembershipAttr,
      @Part("authorizationEnabled") RequestBody authorizationEnabled, @Part("logoutUrl") RequestBody logoutUrl);
  //                                                   @PartMap() Map<String, RequestBody> partMap);
}