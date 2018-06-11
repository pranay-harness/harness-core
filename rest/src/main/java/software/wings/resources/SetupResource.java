package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.Setup;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
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
@Produces("application/json")
@Consumes("application/json")
@Path("setup")
@Scope(ResourceType.SETTING)
public class SetupResource {
  @Inject private SetupService setupService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;

  /**
   * Verify application rest response.
   *
   * @param appId the app id
   * @return the rest response
   */
  @GET
  @Path("/applications/{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Setup> verifyApplication(@PathParam("appId") String appId) {
    return new RestResponse<>(setupService.getApplicationSetupStatus(appService.get(appId)));
  }

  /**
   * Verify service rest response.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the rest response
   */
  @GET
  @Path("/services/{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Setup> verifyService(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(setupService.getServiceSetupStatus(serviceResourceService.get(appId, serviceId)));
  }

  /**
   * Verify environment rest response.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the rest response
   */
  @GET
  @Path("/environments/{envId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Setup> verifyEnvironment(@QueryParam("appId") String appId, @PathParam("envId") String envId) {
    return new RestResponse<>(setupService.getEnvironmentSetupStatus(environmentService.get(appId, envId, false)));
  }
}
