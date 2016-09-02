package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import software.wings.beans.Activity;
import software.wings.beans.Artifact;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceInstanceService;

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 5/26/16.
 */
@Api("/service-instances")
@Path("service-instances")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ServiceInstanceResource {
  /**
   * The Instance service.
   */
  @Inject ServiceInstanceService instanceService;

  /**
   * List.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param serviceId   the service id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<ServiceInstance>> list(@ApiParam @QueryParam("appId") String appId,
      @ApiParam @QueryParam("envId") String envId, @QueryParam("serviceId") String serviceId,
      @BeanParam PageRequest<ServiceInstance> pageRequest) {
    return new RestResponse<>(instanceService.list(pageRequest, appId, envId, serviceId));
  }

  @GET
  @Path("{serviceInstanceId}/artifacts")
  public RestResponse<List<Artifact>> instanceArtifacts(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("serviceInstanceId") String serviceInstanceId) {
    List<Artifact> recentArtifacts = instanceService.getRecentArtifacts(appId, envId, serviceInstanceId);
    return new RestResponse<>(recentArtifacts);
  }

  @GET
  @Path("{serviceInstanceId}/activities")
  public RestResponse<List<Activity>> instanceActivity(@QueryParam("appId") String appId,
      @QueryParam("envId") String envId, @PathParam("serviceInstanceId") String serviceInstanceId) {
    List<Activity> recentActivities = instanceService.getRecentActivities(appId, envId, serviceInstanceId);
    return new RestResponse<>(recentActivities);
  }
}
