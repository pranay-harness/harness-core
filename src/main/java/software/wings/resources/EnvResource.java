package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import software.wings.beans.*;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.EnvironmentService;

import javax.ws.rs.*;

/**
 * Created by anubhaw on 4/1/16.
 */

@Path("/env")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class EnvResource {
  @Inject private EnvironmentService envService;

  @GET
  @Path("{appID}")
  public RestResponse<PageResponse<Environment>> listEnvironments(
      @PathParam("appID") String appID, @BeanParam PageRequest<Environment> pageRequest) {
    pageRequest.addFilter("appID", appID, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Environment>>(envService.listEnvironments(pageRequest));
  }

  @POST
  @Path("{appID}")
  public RestResponse<Environment> createEnvironment(@PathParam("appID") String appID, Environment environment) {
    return new RestResponse<>(envService.createEnvironment(appID, environment));
  }
}
