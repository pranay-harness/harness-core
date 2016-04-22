package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import software.wings.beans.Environment;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.EnvironmentService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Created by anubhaw on 4/1/16.
 */
@Path("/env")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class EnvironmentResource {
  @Inject private EnvironmentService envService;

  @GET
  @Path("{appId}")
  public RestResponse<PageResponse<Environment>> listEnvironments(
      @PathParam("appId") String appId, @BeanParam PageRequest<Environment> pageRequest) {
    pageRequest.addFilter("appId", appId, SearchFilter.Operator.EQ);
    return new RestResponse<PageResponse<Environment>>(envService.listEnvironments(pageRequest));
  }

  @POST
  @Path("{appId}")
  public RestResponse<Environment> createEnvironment(@PathParam("appId") String appId, Environment environment) {
    return new RestResponse<>(envService.createEnvironment(appId, environment));
  }
}
