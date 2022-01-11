/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_AUTHENTICATION_SETTINGS;

import io.harness.ff.FeatureFlagService;
import io.harness.rest.RestResponse;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.loginSettings.PasswordExpirationPolicy;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;
import software.wings.beans.loginSettings.UserLockoutPolicy;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Api("loginSettings")
@Path("/loginSettings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.SETTING)
@AuthRule(permissionType = MANAGE_AUTHENTICATION_SETTINGS)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginSettingsResource {
  FeatureFlagService featureFlagService;
  LoginSettingsService loginSettingsService;

  @Inject
  public LoginSettingsResource(FeatureFlagService featureFlagService, LoginSettingsService loginSettingsService) {
    this.featureFlagService = featureFlagService;
    this.loginSettingsService = loginSettingsService;
  }

  @PUT
  @Path("update-policy-settings")
  @Consumes(MediaType.APPLICATION_JSON)
  public RestResponse<LoginSettings> updatePasswordPolicy(
      @QueryParam("accountId") String accountId, @NotNull @Valid PasswordExpirationPolicy passwordExpirationPolicy) {
    return new RestResponse<>(loginSettingsService.updatePasswordExpirationPolicy(accountId, passwordExpirationPolicy));
  }

  @PUT
  @Path("update-lockout-settings")
  @Consumes(MediaType.APPLICATION_JSON)
  public RestResponse<LoginSettings> updateUserLockoutSettings(
      @QueryParam("accountId") String accountId, @NotNull @Valid UserLockoutPolicy userLockoutPolicy) {
    return new RestResponse<>(loginSettingsService.updateUserLockoutPolicy(accountId, userLockoutPolicy));
  }

  @PUT
  @Path("update-password-strength-settings")
  @Consumes(MediaType.APPLICATION_JSON)
  public RestResponse<LoginSettings> updatePasswordStrengthSettings(
      @QueryParam("accountId") String accountId, @NotNull @Valid PasswordStrengthPolicy passwordStrengthPolicy) {
    return new RestResponse<>(loginSettingsService.updatePasswordStrengthPolicy(accountId, passwordStrengthPolicy));
  }

  @GET
  @Path("get-login-settings")
  @Consumes(MediaType.APPLICATION_JSON)
  public RestResponse<LoginSettings> getLoginSettings(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(loginSettingsService.getLoginSettings(accountId));
  }
}
