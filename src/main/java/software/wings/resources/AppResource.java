package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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

// TODO: Auto-generated Javadoc

/**
 * Application Resource class.
 *
 * @author Rishi
 */
@Api("/apps")
@Path("/apps")
@AuthRule
@Produces("application/json")
@Timed
@ExceptionMetered
public class AppResource {
  private AppService appService;

  /**
   * Instantiates a new app resource.
   *
   * @param appService the app service
   */
  @Inject
  public AppResource(AppService appService) {
    this.appService = appService;
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Application>> list(@BeanParam PageRequest<Application> pageRequest) {
    return new RestResponse<>(appService.list(pageRequest));
  }

  /**
   * Save.
   *
   * @param app the app
   * @return the rest response
   */
  @POST
  public RestResponse<Application> save(Application app) {
    return new RestResponse<>(appService.save(app));
  }

  /**
   * Update.
   *
   * @param appId the app id
   * @param app   the app
   * @return the rest response
   */
  @PUT
  @Path("{appId}")
  public RestResponse<Application> update(@PathParam("appId") String appId, Application app) {
    app.setUuid(appId);
    return new RestResponse<>(appService.update(app));
  }

  /**
   * Gets the.
   *
   * @param appId the app id
   * @return the rest response
   */
  @GET
  @Path("{appId}")
  public RestResponse<Application> get(@PathParam("appId") String appId) {
    return new RestResponse<>(appService.findByUuid(appId));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @return the rest response
   */
  @DELETE
  @Path("{appId}")
  public RestResponse delete(@PathParam("appId") String appId) {
    appService.deleteApp(appId);
    return new RestResponse();
  }
}
