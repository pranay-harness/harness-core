package io.harness.ng.authenticationsettings.resources;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.ng.accesscontrol.PlatformPermissions.DELETE_AUTHSETTING_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.EDIT_AUTHSETTING_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_AUTHSETTING_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.AUTHSETTING;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.dtos.mechanisms.OAuthSettings;
import io.harness.ng.authenticationsettings.impl.AuthenticationSettingsService;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.user.TwoFactorAdminOverrideSettings;
import io.harness.rest.RestResponse;
import io.harness.stream.BoundedInputStream;

import software.wings.app.MainConfiguration;
import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.security.authentication.SSOConfig;

import com.amazonaws.util.IOUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
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
@Tag(name = "AuthenticationSettings",
    description = "This contains APIs related to Authentication settings as defined in Harness")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsResource {
  AuthenticationSettingsService authenticationSettingsService;
  private final MainConfiguration mainConfiguration;
  private final AccessControlClient accessControlClient;

  @GET
  @Path("/")
  @ApiOperation(value = "Get authentication settings for an account", nickname = "getAuthenticationSettings")
  @Operation(operationId = "getAuthenticationSettings",
      summary = "Get the authentication settings by accountIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns Authentication settings of the Account")
      })
  public RestResponse<AuthenticationSettingsResponse>
  getAuthenticationSettings(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      "accountIdentifier") @NotNull String accountIdentifier) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), VIEW_AUTHSETTING_PERMISSION);
    AuthenticationSettingsResponse response =
        authenticationSettingsService.getAuthenticationSettings(accountIdentifier);
    return new RestResponse<>(response);
  }

  @GET
  @Path("/login-settings/password-strength")
  @ApiOperation(value = "Get Password strength settings", nickname = "getPasswordStrengthSettings")
  @Operation(operationId = "getPasswordStrengthSettings",
      summary = "Get the password strength settings by accountIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns Authentication settings of the Account")
      })
  public RestResponse<PasswordStrengthPolicy>
  getPasswordStrengthSettings(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      "accountIdentifier") @NotNull String accountIdentifier) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), VIEW_AUTHSETTING_PERMISSION);
    PasswordStrengthPolicy response = authenticationSettingsService.getPasswordStrengthSettings(accountIdentifier);
    return new RestResponse<>(response);
  }

  @PUT
  @Hidden
  @Path("/login-settings/{loginSettingsId}")
  @ApiOperation(value = "Update login settings - lockout, expiration, strength", nickname = "putLoginSettings")
  @Operation(operationId = "putLoginSettings", summary = "Updates the login settings",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the updated login settings")
      })
  public RestResponse<LoginSettings>
  updateLoginSettings(
      @Parameter(description = "Login Settings Identifier") @PathParam("loginSettingsId") String loginSettingsId,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @QueryParam(
          "accountIdentifier") @NotEmpty String accountIdentifier,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          description =
              "This is the updated Login Settings. Please provide values for all fields, not just the fields you are updating")
      @NotNull @Valid LoginSettings loginSettings) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), EDIT_AUTHSETTING_PERMISSION);
    LoginSettings updatedLoginSettings =
        authenticationSettingsService.updateLoginSettings(loginSettingsId, accountIdentifier, loginSettings);
    return new RestResponse<>(updatedLoginSettings);
  }

  @PUT
  @Path("/oauth/update-providers")
  @ApiOperation(value = "Update Oauth providers for an account", nickname = "updateOauthProviders")
  @Operation(operationId = "updateOauthProviders", summary = "Updates the Oauth providers by accountIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns success response")
      })
  public RestResponse<Boolean>
  updateOauthProviders(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                           "accountIdentifier") @NotNull String accountIdentifier,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          description =
              "This is the updated OAuthSettings. Please provide values for all fields, not just the fields you are updating")
      OAuthSettings oAuthSettings) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), EDIT_AUTHSETTING_PERMISSION);
    authenticationSettingsService.updateOauthProviders(accountIdentifier, oAuthSettings);
    return new RestResponse<>(true);
  }

  @DELETE
  @Path("/oauth/remove-mechanism")
  @ApiOperation(value = "Remove Oauth mechanism for an account", nickname = "removeOauthMechanism")
  @Operation(operationId = "removeOauthMechanism", summary = "Deletes OAuth mechanism by accountIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns success response")
      })
  public RestResponse<Boolean>
  removeOauthMechanism(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      "accountIdentifier") @NotNull String accountIdentifier) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), DELETE_AUTHSETTING_PERMISSION);
    authenticationSettingsService.removeOauthMechanism(accountIdentifier);
    return new RestResponse<>(true);
  }

  @PUT
  @Path("/update-auth-mechanism")
  @ApiOperation(value = "Update Auth mechanism for an account", nickname = "updateAuthMechanism")
  @Operation(operationId = "updateAuthMechanism", summary = "Updates the Auth mechanism by accountIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns success response")
      })
  public RestResponse<Boolean>
  updateAuthMechanism(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                          "accountIdentifier") @NotNull String accountIdentifier,
      @Parameter(description = "Type of Authentication Mechanism SSO or NON_SSO") @QueryParam(
          "authenticationMechanism") AuthenticationMechanism authenticationMechanism) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), EDIT_AUTHSETTING_PERMISSION);
    authenticationSettingsService.updateAuthMechanism(accountIdentifier, authenticationMechanism);
    return new RestResponse<>(true);
  }

  @PUT
  @Path("/whitelisted-domains")
  @ApiOperation(value = "Update Whitelisted domains for an account", nickname = "updateWhitelistedDomains")
  @Operation(operationId = "updateWhitelistedDomains", summary = "Updates the Whitelisted domains by accountIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns success response")
      })
  public RestResponse<Boolean>
  updateWhitelistedDomains(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                               "accountIdentifier") @NotNull String accountIdentifier,
      @Parameter(description = "Set of whitelisted domains and IPs for the account") Set<String> whitelistedDomains) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), EDIT_AUTHSETTING_PERMISSION);
    authenticationSettingsService.updateWhitelistedDomains(accountIdentifier, whitelistedDomains);
    return new RestResponse<>(true);
  }

  @Multipart
  @POST
  @Path("/saml-metadata-upload")
  @Consumes("multipart/form-data")
  @ApiOperation(value = "Create SAML Config", nickname = "uploadSamlMetaData")
  @Operation(operationId = "uploadSamlMetaData", summary = "Uploads the SAML metadata by accountId",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns SSO config of the account")
      })
  public RestResponse<SSOConfig>
  uploadSamlMetaData(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam("accountId") String accountId,
      @Parameter(description = "Saml Metadata input file") @FormDataParam("file") InputStream uploadedInputStream,
      @Parameter(description = "Input file metadata") @FormDataParam(
          "fileMetadata") FormDataContentDisposition fileDetail,
      @Parameter(description = "Display Name of the SAML") @FormDataParam("displayName") String displayName,
      @Parameter(description = "Group membership attribute") @FormDataParam(
          "groupMembershipAttr") String groupMembershipAttr,
      @Parameter(description = "Specify whether or not to enable authorization") @FormDataParam("authorizationEnabled")
      Boolean authorizationEnabled, @Parameter(description = "Logout URL") @FormDataParam("logoutUrl") String logoutUrl,
      @Parameter(description = "SAML metadata Identifier") @FormDataParam("entityIdentifier") String entityIdentifier) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountId, null, null), Resource.of(AUTHSETTING, null), EDIT_AUTHSETTING_PERMISSION);
    try {
      byte[] bytes = IOUtils.toByteArray(
          new BoundedInputStream(uploadedInputStream, mainConfiguration.getFileUploadLimits().getCommandUploadLimit()));
      final MultipartBody.Part formData =
          MultipartBody.Part.createFormData("file", null, RequestBody.create(MultipartBody.FORM, bytes));
      SSOConfig response = authenticationSettingsService.uploadSAMLMetadata(
          accountId, formData, displayName, groupMembershipAttr, authorizationEnabled, logoutUrl, entityIdentifier);
      return new RestResponse<>(response);
    } catch (Exception e) {
      throw new GeneralException("Error while creating new SAML Config", e);
    }
  }

  @Multipart
  @PUT
  @Path("/saml-metadata-upload")
  @Consumes("multipart/form-data")
  @ApiOperation(value = "Edit SAML Config", nickname = "updateSamlMetaData")
  @Operation(operationId = "updateSamlMetaData", summary = "Updates the SAML metadata by accountId",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns SSO config of the account")
      })
  public RestResponse<SSOConfig>
  updateSamlMetaData(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam("accountId") @NotNull String accountId,
      @Parameter(description = "SAML Metadata input file") @FormDataParam("file") InputStream uploadedInputStream,
      @Parameter(description = "Input file metadata") @FormDataParam(
          "fileMetadata") FormDataContentDisposition fileDetail,
      @Parameter(description = "Display Name of the SAML") @FormDataParam("displayName") String displayName,
      @Parameter(description = "Group membership attribute") @FormDataParam(
          "groupMembershipAttr") String groupMembershipAttr,
      @Parameter(description = "Specify whether or not to enable authorization") @FormDataParam("authorizationEnabled")
      Boolean authorizationEnabled, @Parameter(description = "Logout URL") @FormDataParam("logoutUrl") String logoutUrl,
      @Parameter(description = "SAML metadata Identifier") @FormDataParam("entityIdentifier") String entityIdentifier) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountId, null, null), Resource.of(AUTHSETTING, null), EDIT_AUTHSETTING_PERMISSION);
    try {
      MultipartBody.Part formData = null;
      if (uploadedInputStream != null) {
        byte[] bytes = IOUtils.toByteArray(new BoundedInputStream(
            uploadedInputStream, mainConfiguration.getFileUploadLimits().getCommandUploadLimit()));
        formData = MultipartBody.Part.createFormData("file", null, RequestBody.create(MultipartBody.FORM, bytes));
      }
      SSOConfig response = authenticationSettingsService.updateSAMLMetadata(
          accountId, formData, displayName, groupMembershipAttr, authorizationEnabled, logoutUrl, entityIdentifier);
      return new RestResponse<>(response);
    } catch (Exception e) {
      throw new GeneralException("Error while editing saml-config", e);
    }
  }

  @DELETE
  @Path("/delete-saml-metadata")
  @ApiOperation(value = "Delete SAML Config", nickname = "deleteSamlMetaData")
  @Operation(operationId = "deleteSamlMetaData", summary = "Deletes SAML meta data by accountIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the enabled SSO OAuth Providers for the account")
      })
  public RestResponse<SSOConfig>
  deleteSamlMetadata(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
      "accountIdentifier") @NotNull String accountIdentifier) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), DELETE_AUTHSETTING_PERMISSION);
    SSOConfig response = authenticationSettingsService.deleteSAMLMetadata(accountIdentifier);
    return new RestResponse<>(response);
  }

  @GET
  @Path("/saml-login-test")
  @ApiOperation(value = "Get SAML Login Test", nickname = "getSamlLoginTest")
  @Operation(operationId = "getSamlLoginTest", summary = "Get the SAML login test by accountId",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the login type enabled for the account")
      })
  public RestResponse<LoginTypeResponse>
  getSamlLoginTest(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam("accountId") @NotNull String accountId) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountId, null, null), Resource.of(AUTHSETTING, null), VIEW_AUTHSETTING_PERMISSION);
    LoginTypeResponse response = authenticationSettingsService.getSAMLLoginTest(accountId);
    return new RestResponse<>(response);
  }

  @PUT
  @Path("/two-factor-admin-override-settings")
  @ApiOperation(value = "Set account level two factor auth setting", nickname = "setTwoFactorAuthAtAccountLevel")
  @Operation(operationId = "setTwoFactorAuthAtAccountLevel",
      summary = "Set two factor auth at account lever by accountIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the boolean status")
      })
  public RestResponse<Boolean>
  setTwoFactorAuthAtAccountLevel(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                                     "accountIdentifier") @NotNull String accountIdentifier,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true, description = "Boolean that specify whether or not to override two factor enabled setting")
      TwoFactorAdminOverrideSettings twoFactorAdminOverrideSettings) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(AUTHSETTING, null), EDIT_AUTHSETTING_PERMISSION);
    boolean response =
        authenticationSettingsService.setTwoFactorAuthAtAccountLevel(accountIdentifier, twoFactorAdminOverrideSettings);
    return new RestResponse<>(response);
  }
}
