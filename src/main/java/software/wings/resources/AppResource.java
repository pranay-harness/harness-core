package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.app.WingsBootstrap;
import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import javax.ws.rs.*;

/**
 *  Application Resource class
 *
 *
 * @author Rishi
 *
 */

@Path("/apps")
@AuthRule
@Produces("application/json")
@Timed
@ExceptionMetered
public class AppResource {
  private AppService appService = WingsBootstrap.lookup(AppService.class);

  @GET
  public RestResponse<PageResponse<Application>> list(@BeanParam PageRequest<Application> pageRequest) {
    return new RestResponse<>(appService.list(pageRequest));
  }

  @POST
  public RestResponse<Application> save(Application app) {
    return new RestResponse<>(appService.save(app));
  }

  @PUT
  public RestResponse<Application> update(Application app) {
    return new RestResponse<>(appService.update(app));
  }

  @GET
  @Path("{appID}")
  public RestResponse<Application> get(@PathParam("appID") String appID) {
    return new RestResponse<>(appService.findByUUID(appID));
  }
}
