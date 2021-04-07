package io.harness.ng.authenticationsettings.resources;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.dtos.mechanisms.OAuthSettings;
import io.harness.ng.authenticationsettings.impl.AuthenticationSettingsService;
import io.harness.rest.RestResponse;

import software.wings.beans.loginSettings.PasswordExpirationPolicy;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;
import software.wings.beans.loginSettings.UserLockoutPolicy;
import software.wings.security.authentication.AuthenticationMechanism;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("authentication-settings")
@Path("/authentication-settings")
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsResource {
  AuthenticationSettingsService authenticationSettingsService;

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
  @Path("/username-password/update-password-expiration-settings")
  @ApiOperation(value = "Update Password expiration settings", nickname = "updateExpirationSettings")
  public RestResponse<Boolean> updateExpirationSettings(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier,
      @NotNull @Valid PasswordExpirationPolicy passwordExpirationPolicy) {
    authenticationSettingsService.updateExpirationSettings(accountIdentifier, passwordExpirationPolicy);
    return new RestResponse<>(true);
  }

  @PUT
  @Path("/username-password/update-user-lockout-settings")
  @ApiOperation(value = "Update user lockout settings", nickname = "updateLockoutSettings")
  public RestResponse<Boolean> updateLockoutSettings(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier,
      @NotNull @Valid UserLockoutPolicy userLockoutPolicy) {
    authenticationSettingsService.updateLockoutSettings(accountIdentifier, userLockoutPolicy);
    return new RestResponse<>(true);
  }

  @PUT
  @Path("/username-password/update-password-strength-settings")
  @ApiOperation(value = "Update Password strength settings", nickname = "updatePasswordStrengthSettings")
  public RestResponse<Boolean> updatePasswordStrengthSettings(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier,
      @NotNull @Valid PasswordStrengthPolicy passwordStrengthPolicy) {
    authenticationSettingsService.updatePasswordStrengthSettings(accountIdentifier, passwordStrengthPolicy);
    return new RestResponse<>(true);
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
}
