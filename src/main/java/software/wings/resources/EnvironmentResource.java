package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import software.wings.beans.*;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.EnvironmentService;

import javax.ws.rs.*;
import java.util.List;

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
  @Path("{appID}")
  public RestResponse<List<Environment>> listEnvironments(@PathParam("appID") String appID) {
    return new RestResponse<List<Environment>>(envService.listEnvironments(appID));
  }

  @POST
  @Path("{appID}")
  public RestResponse<Environment> createEnvironment(@PathParam("appID") String appID, Environment environment) {
    return new RestResponse<>(envService.createEnvironment(appID, environment));
  }
}
