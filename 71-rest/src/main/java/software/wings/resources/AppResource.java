package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.limits.LimitCheckerFactory;
import io.swagger.annotations.Api;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.ListAPI;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AppService;

import java.util.List;
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
@Produces("application/json")
@Scope(APPLICATION)
public class AppResource {
  private AppService appService;
  private LimitCheckerFactory limitCheckerFactory;

  @Inject
  public AppResource(AppService appService, LimitCheckerFactory limitCheckerFactory) {
    this.appService = appService;
    this.limitCheckerFactory = limitCheckerFactory;
  }

  /**
   * List.
   *
   * @param pageRequest        the page request
   * @return the rest response
   */
  @GET
  @ListAPI(APPLICATION)
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Application>> list(@BeanParam PageRequest<Application> pageRequest,
      @QueryParam("appIds") List<String> appIds, @QueryParam("details") @DefaultValue("true") boolean details) {
    return new RestResponse<>(appService.list(pageRequest, details));
  }

  /**
   * Save.
   *
   * @param app the app
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE)
  public RestResponse<Application> save(@QueryParam("accountId") String accountId, Application app) {
    app.setAccountId(accountId);
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
  @Timed
  @ExceptionMetered
  public RestResponse<Application> update(@PathParam("appId") String appId, Application app) {
    app.setUuid(appId);
    return new RestResponse<>(appService.update(app));
  }

  /**
   * Gets the.
   *
   * @param appId  the app id
   * @return the rest response
   */
  @GET
  @Path("{appId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> get(
      @PathParam("appId") String appId, @QueryParam("details") @DefaultValue("true") boolean details) {
    return new RestResponse<>(appService.get(appId, true));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @return the rest response
   */
  @DELETE
  @Path("{appId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.APPLICATION_CREATE_DELETE)
  public RestResponse delete(@PathParam("appId") String appId) {
    appService.delete(appId);
    return new RestResponse();
  }
}
