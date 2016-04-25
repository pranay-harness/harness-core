package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.Environment;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.EnvironmentService;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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

  @GET
  @Path("{appId}")
  public RestResponse<List<Environment>> listEnvironments(@PathParam("appId") String appId) {
    return new RestResponse<>(envService.listEnvironments(appId));
  }

  @POST
  @Path("{appId}")
  public RestResponse<Environment> createEnvironment(@PathParam("appId") String appId, Environment environment) {
    return new RestResponse<>(envService.createEnvironment(appId, environment));
  }
}
