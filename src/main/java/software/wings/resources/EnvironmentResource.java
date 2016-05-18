package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.Environment;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 4/1/16.
 */
@Path("/environments")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class EnvironmentResource {
  @Inject private EnvironmentService envService;
  @Inject private HostService hostService;

  @GET
  public RestResponse<PageResponse<Environment>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Environment> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(envService.list(pageRequest));
  }

  @POST
  public RestResponse<Environment> save(@QueryParam("appId") String appId, Environment environment) {
    environment.setAppId(appId);
    return new RestResponse<>(envService.save(environment));
  }

  @GET
  @Path("{envId}")
  public RestResponse<Environment> list(@QueryParam("appId") String appId, @PathParam("envId") String envId) {
    return new RestResponse<>(envService.get(appId, envId));
  }

  @PUT
  @Path("{envId}")
  public RestResponse<Environment> update(
      @QueryParam("appId") String appId, @PathParam("envId") String envId, Environment environment) {
    environment.setUuid(envId);
    environment.setAppId(appId);
    return new RestResponse<>(envService.update(environment));
  }

  @DELETE
  @Path("{envId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("envId") String envId) {
    envService.delete(envId);
    return new RestResponse();
  }
}
