package io.harness.commandlibrary.server.resources;

import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.security.annotations.PublicApi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Api("command-stores")
@Path("/command-stores")
@Produces("application/json")
//@Scope(PermissionAttribute.ResourceType.SETTING)
public class CommandStoreResource {
  @GET
  @Path("test")
  @PublicApi
  public RestResponse<String> testApi() {
    return new RestResponse<>("hello world");
  }
}
