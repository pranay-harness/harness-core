package software.wings.resources;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.LoginSettingsService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
@TargetModule(HarnessModule._120_NG_MANAGER)
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
