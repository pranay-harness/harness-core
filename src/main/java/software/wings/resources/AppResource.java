package software.wings.resources;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import io.dropwizard.jersey.caching.CacheControl;
import software.wings.app.WingsBootstrap;
import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.AppService;
/**
 *  Application Resource class
 *
 *
 * @author Rishi
 *
 */
@Path("/apps")
public class AppResource {
  private static final Logger logger = LoggerFactory.getLogger(AppResource.class);

  private AppService appService;

  public AppResource() {
    appService = WingsBootstrap.lookup(AppService.class);
  }
  public AppResource(AppService appService) {
    this.appService = appService;
  }

  @GET
  @Timed
  @ExceptionMetered
  @CacheControl(maxAge = 15, maxAgeUnit = TimeUnit.MINUTES)
  @Produces("application/json")
  public RestResponse<PageResponse<Application>> list(@BeanParam PageRequest<Application> pageRequest) {
    return new RestResponse<PageResponse<Application>>(appService.list(pageRequest));
  }

  @GET
  @Path("{name}")
  @Produces("application/json")
  public RestResponse<Application> get(@PathParam("name") String name) {
    return new RestResponse<Application>(appService.findByName(name));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Application> save(Application app) {
    return new RestResponse<Application>(appService.save(app));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Application> update(Application app) {
    return new RestResponse<Application>(appService.update(app));
  }
}
