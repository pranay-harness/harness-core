package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_AUTHENTICATION_SETTINGS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ff.FeatureFlagService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.loginSettings.*;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Api(value = "/ng/login-settings", hidden = true)
@Path("/ng/login-settings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PL)
public class LoginSettingsResourceNG {
  LoginSettingsService loginSettingsService;

  @Inject
  public LoginSettingsResourceNG(LoginSettingsService loginSettingsService) {
    this.loginSettingsService = loginSettingsService;
  }

  @GET
  @Path("/username-password")
  @Consumes(MediaType.APPLICATION_JSON)
  public RestResponse<LoginSettings> getLoginSettings(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(loginSettingsService.getLoginSettings(accountId));
  }
}
