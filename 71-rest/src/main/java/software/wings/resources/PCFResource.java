package software.wings.resources;

import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.InfrastructureMappingService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("pcfresource")
@Path("pcfresource")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
public class PCFResource {
  @Inject InfrastructureMappingService infrastructureMappingService;

  @POST
  @Path("create-route")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse<String> createRouteForPcf(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @QueryParam("computeProviderId") String computeProviderId, @QueryParam("org") String org,
      @QueryParam("space") String space, @QueryParam("host") String host, @QueryParam("domain") String domain,
      @QueryParam("path") String path, @QueryParam("port") String port,
      @QueryParam("useRandomPort") boolean useRandomPort, @QueryParam("tcpRoute") boolean tcpRoute) {
    return new RestResponse<>(infrastructureMappingService.createRoute(
        appId, computeProviderId, org, space, host, domain, path, tcpRoute, useRandomPort, port));
  }
}
