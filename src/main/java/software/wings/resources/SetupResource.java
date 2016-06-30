package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.Setup;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SetupService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 6/29/16.
 */
@Api("setup")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
@Path("setup")
public class SetupResource {
  @Inject private SetupService setupService;

  @GET
  @Path("/applications/{appId}")
  public RestResponse<Setup> verifyApplication(@PathParam("appId") String appId) {
    return new RestResponse<>(setupService.getApplicationSetupStatus(appId));
  }

  @GET
  @Path("/services/{serviceId}")
  public RestResponse<Setup> verifyService(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(setupService.getServiceSetupStatus(appId, serviceId));
  }

  @GET
  @Path("/environments/{envId}")
  public RestResponse<Setup> verifyEnvironment(@QueryParam("appId") String appId, @PathParam("envId") String envId) {
    return new RestResponse<>(setupService.getEnvironmentSetupStatus(appId, envId));
  }
}
