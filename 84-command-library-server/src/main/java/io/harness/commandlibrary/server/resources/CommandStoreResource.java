package io.harness.commandlibrary.server.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.security.annotations.AuthRule;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("command-stores")
@Path("/command-stores")
@Produces("application/json")
//@Scope(PermissionAttribute.ResourceType.SETTING)
public class CommandStoreResource {
  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<String> getCommandStores(@QueryParam("accountId") String accountId) {
    return new RestResponse<>("hello world");
  }
}
