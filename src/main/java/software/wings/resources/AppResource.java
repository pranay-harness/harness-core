package software.wings.resources;

import static com.google.common.collect.ImmutableMap.of;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Application Resource class.
 *
 * @author Rishi
 */
@Path("/apps")
@AuthRule
@Produces("application/json")
@Timed
@ExceptionMetered
public class AppResource {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private AppService appService;

  @Inject
  public AppResource(AppService appService) {
    this.appService = appService;
  }

  @GET
  public RestResponse<PageResponse<Application>> list(@BeanParam PageRequest<Application> pageRequest) {
    return new RestResponse<>(appService.list(pageRequest));
  }

  @POST
  public RestResponse<Application> save(Application app) {
    return new RestResponse<>(appService.save(app));
  }

  @PUT
  @Path("{appId}")
  public RestResponse<Application> update(@PathParam("appId") String appId, Application app) {
    app.setUuid(appId);
    return new RestResponse<>(appService.update(app));
  }

  @GET
  @Path("{appId}")
  public RestResponse<Application> get(@PathParam("appId") String appId) {
    return new RestResponse<>(appService.findByUuid(appId));
  }

  @DELETE
  @Path("{appId}")
  public RestResponse delete(@PathParam("appId") String appId) {
    appService.deleteApp(appId);
    return new RestResponse(of("status", "success"));
  }
}
