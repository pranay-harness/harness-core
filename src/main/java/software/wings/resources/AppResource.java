package software.wings.resources;

import static software.wings.beans.Setup.SetupStatus.COMPLETE;
import static software.wings.security.PermissionAttribute.APP_READ;
import static software.wings.security.PermissionAttribute.APP_WRITE;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.Setup.SetupStatus;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.service.intfc.AppService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
   * @param pageRequest        the page request
   * @param overview           the summary
   * @param numberOfExecutions the number of executions
   * @return the rest response
   */
  @GET
  @AuthRule(APP_READ)
  @ListAPI(APPLICATION)
  public RestResponse<PageResponse<Application>> list(@BeanParam PageRequest<Application> pageRequest,
      @QueryParam("overview") @DefaultValue("false") boolean overview,
      @QueryParam("numberOfExecutions") @DefaultValue("5") int numberOfExecutions) {
    return new RestResponse<>(appService.list(pageRequest, overview, numberOfExecutions));
  }

  /**
   * Save.
   *
   * @param app the app
   * @return the rest response
   */
  @POST
  @AuthRule(APP_WRITE)
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
  @AuthRule(APP_WRITE)
  public RestResponse<Application> update(@PathParam("appId") String appId, Application app) {
    app.setUuid(appId);
    return new RestResponse<>(appService.update(app));
  }

  /**
   * Gets the.
   *
   * @param appId  the app id
   * @param status the status
   * @return the rest response
   */
  @GET
  @Path("{appId}")
  @AuthRule(APP_READ)
  public RestResponse<Application> get(@PathParam("appId") String appId, @QueryParam("status") SetupStatus status) {
    if (status == null) {
      status = COMPLETE; // don't verify setup status
    }
    return new RestResponse<>(appService.get(appId, status));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @return the rest response
   */
  @DELETE
  @AuthRule(APP_WRITE)
  @Path("{appId}")
  public RestResponse delete(@PathParam("appId") String appId) {
    appService.delete(appId);
    return new RestResponse();
  }
}
