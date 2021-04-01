package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.security.annotations.AuthRule;
import software.wings.security.authentication.SSOConfig;
import software.wings.service.intfc.SSOService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

@Api(value = "/ng/sso", hidden = true)
@Path("/ng/sso")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class SSOResourceNG {
  private SSOService ssoService;

  @Inject
  public SSOResourceNG(SSOService ssoService) {
    this.ssoService = ssoService;
  }

  @GET
  @Path("get-access-management")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> getAccountAccessManagementSettings(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(ssoService.getAccountAccessManagementSettings(accountId));
  }
}
