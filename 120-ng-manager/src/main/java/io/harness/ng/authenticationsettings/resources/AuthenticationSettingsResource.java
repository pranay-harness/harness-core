package io.harness.ng.authenticationsettings.resources;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.dtos.mechanisms.OAuthSettings;
import io.harness.ng.authenticationsettings.impl.AuthenticationSettingsService;
import io.harness.rest.RestResponse;
import io.harness.stream.BoundedInputStream;

import software.wings.app.MainConfiguration;
import software.wings.beans.loginSettings.LoginSettings;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.security.authentication.SSOConfig;

import com.amazonaws.util.IOUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.InputStream;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Multipart;

@Api("authentication-settings")
@Path("/authentication-settings")
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsResource {
  AuthenticationSettingsService authenticationSettingsService;
  private final MainConfiguration mainConfiguration;

  @GET
  @Path("/")
  @ApiOperation(value = "Get authentication settings for an account", nickname = "getAuthenticationSettings")
  public RestResponse<AuthenticationSettingsResponse> getAuthenticationSettings(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier) {
    AuthenticationSettingsResponse response =
        authenticationSettingsService.getAuthenticationSettings(accountIdentifier);
    return new RestResponse<>(response);
  }

  @PUT
  @Path("/login-settings/{loginSettingsId}")
  @ApiOperation(value = "Update login settings - lockout, expiration, strength", nickname = "putLoginSettings")
  public RestResponse<LoginSettings> updateLoginSettings(@PathParam("loginSettingsId") String loginSettingsId,
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier,
      @NotNull @Valid LoginSettings loginSettings) {
    LoginSettings updatedLoginSettings =
        authenticationSettingsService.updateLoginSettings(loginSettingsId, accountIdentifier, loginSettings);
    return new RestResponse<>(updatedLoginSettings);
  }

  @PUT
  @Path("/oauth/update-providers")
  @ApiOperation(value = "Update Oauth providers for an account", nickname = "updateOauthProviders")
  public RestResponse<Boolean> updateOauthProviders(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier, OAuthSettings oAuthSettings) {
    authenticationSettingsService.updateOauthProviders(accountIdentifier, oAuthSettings);
    return new RestResponse<>(true);
  }

  @DELETE
  @Path("/oauth/remove-mechanism")
  @ApiOperation(value = "Remove Oauth mechanism for an account", nickname = "removeOauthMechanism")
  public RestResponse<Boolean> removeOauthMechanism(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier) {
    authenticationSettingsService.removeOauthMechanism(accountIdentifier);
    return new RestResponse<>(true);
  }

  @PUT
  @Path("/update-auth-mechanism")
  @ApiOperation(value = "Update Auth mechanism for an account", nickname = "updateAuthMechanism")
  public RestResponse<Boolean> updateAuthMechanism(@QueryParam("accountIdentifier") @NotEmpty String accountIdentifier,
      @QueryParam("authenticationMechanism") AuthenticationMechanism authenticationMechanism) {
    authenticationSettingsService.updateAuthMechanism(accountIdentifier, authenticationMechanism);
    return new RestResponse<>(true);
  }

  @Multipart
  @POST
  @Path("/saml-metadata-upload")
  @ApiOperation(value = "Create SAML Config", nickname = "uploadSamlMetaData")
  public RestResponse<SSOConfig> uploadSamlMetaData(@QueryParam("accountId") String accountId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("displayName") String displayName,
      @FormDataParam("groupMembershipAttr") String groupMembershipAttr,
      @FormDataParam("authorizationEnabled") Boolean authorizationEnabled,
      @FormDataParam("logoutUrl") String logoutUrl) {
    try {
      byte[] bytes = IOUtils.toByteArray(
          new BoundedInputStream(uploadedInputStream, mainConfiguration.getFileUploadLimits().getCommandUploadLimit()));
      final MultipartBody.Part formData =
          MultipartBody.Part.createFormData("file", null, RequestBody.create(MultipartBody.FORM, bytes));
      SSOConfig response = authenticationSettingsService.uploadSAMLMetadata(
          accountId, formData, displayName, groupMembershipAttr, authorizationEnabled, logoutUrl);
      return new RestResponse<>(response);
    } catch (Exception e) {
      throw new GeneralException("Error while creating new SAML Config", e);
    }
  }

  @Multipart
  @PUT
  @Path("/saml-metadata-upload")
  @ApiOperation(value = "Edit SAML Config", nickname = "updateSamlMetaData")
  public RestResponse<SSOConfig> updateSamlMetaData(@QueryParam("accountId") String accountId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("displayName") String displayName,
      @FormDataParam("groupMembershipAttr") String groupMembershipAttr,
      @FormDataParam("authorizationEnabled") Boolean authorizationEnabled,
      @FormDataParam("logoutUrl") String logoutUrl) {
    try {
      MultipartBody.Part formData = null;
      if (uploadedInputStream != null) {
        byte[] bytes = IOUtils.toByteArray(new BoundedInputStream(
            uploadedInputStream, mainConfiguration.getFileUploadLimits().getCommandUploadLimit()));
        formData = MultipartBody.Part.createFormData("file", null, RequestBody.create(MultipartBody.FORM, bytes));
      }
      SSOConfig response = authenticationSettingsService.updateSAMLMetadata(
          accountId, formData, displayName, groupMembershipAttr, authorizationEnabled, logoutUrl);
      return new RestResponse<>(response);
    } catch (Exception e) {
      throw new GeneralException("Error while editing saml-config", e);
    }
  }

  @DELETE
  @Path("/delete-saml-metadata")
  @ApiOperation(value = "Delete SAML Config", nickname = "deleteSamlMetaData")
  public RestResponse<SSOConfig> deleteSamlMetadata(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier) {
    SSOConfig response = authenticationSettingsService.deleteSAMLMetadata(accountIdentifier);
    return new RestResponse<>(response);
  }

  @GET
  @Path("/saml-login-test")
  @ApiOperation(value = "Get SAML Login Test", nickname = "getSamlLoginTest")
  public RestResponse<LoginTypeResponse> getSamlLoginTest(@QueryParam("accountId") @NotEmpty String accountId) {
    LoginTypeResponse response = authenticationSettingsService.getSAMLLoginTest(accountId);
    return new RestResponse<>(response);
  }
}
