package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.common.Constants;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ThirdPartyApiService;

import java.io.File;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Api("activities")
@Path("/activities")
@Produces("application/json")
@Scope(APPLICATION)
public class ActivityResource {
  private AppService appService;
  private ActivityService activityService;
  private LogService logService;
  private ThirdPartyApiService thirdPartyApiService;

  /**
   * Instantiates a new activity resource.
   *
   * @param appService the activity service
   * @param activityService the activity service
   * @param logService      the log service
   */
  @SuppressFBWarnings("URF_UNREAD_FIELD")
  @Inject
  public ActivityResource(AppService appService, ActivityService activityService, LogService logService,
      ThirdPartyApiService thirdPartyApiService) {
    this.appService = appService;
    this.activityService = activityService;
    this.logService = logService;
    this.thirdPartyApiService = thirdPartyApiService;
  }

  /**
   * List.
   *
   * @param envId   the env id
   * @param request the request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Activity>> list(@QueryParam("accountId") String accountId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<Activity> request) {
    if (isNotEmpty(envId)) {
      request.addFilter("environmentId", EQ, envId);
    }
    if (request.getPageSize() > Constants.DEFAULT_RUNTIME_ENTITY_PAGESIZE) {
      request.setLimit(Constants.DEFAULT_RUNTIME_ENTITY_PAGESIZE_STR);
    }
    return new RestResponse<>(activityService.list(request));
  }

  /**
   * Gets the.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the rest response
   */

  @GET
  @Path("{activityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Activity> get(@QueryParam("appId") String appId, @PathParam("activityId") String activityId) {
    return new RestResponse<>(activityService.get(activityId, appId));
  }

  /**
   * List logs.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @param unitName   the unit name
   * @param request    the request
   * @return the rest response
   */
  @GET
  @Path("{activityId}/logs")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Log>> listLogs(@QueryParam("appId") String appId,
      @PathParam("activityId") String activityId, @QueryParam("unitName") String unitName,
      @BeanParam PageRequest<Log> request) {
    request.addFilter("activityId", EQ, activityId);
    request.addFilter("commandUnitName", EQ, unitName);
    return new RestResponse<>(logService.list(request));
  }

  /**
   * List logs rest response.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the rest response
   */
  @GET
  @Path("{activityId}/units")
  @Timed
  public RestResponse<List<CommandUnitDetails>> getActivityCommandUnits(
      @QueryParam("appId") String appId, @PathParam("activityId") String activityId) {
    return new RestResponse<>(activityService.getCommandUnits(appId, activityId));
  }

  /**
   * Export logs response.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the response
   */
  @GET
  @Path("{activityId}/all-logs")
  @Encoded
  @Timed
  @ExceptionMetered
  public Response exportLogs(@QueryParam("appId") String appId, @PathParam("activityId") String activityId) {
    File logFile = logService.exportLogs(appId, activityId);
    ResponseBuilder response = Response.ok(logFile, "application/x-unknown");
    response.header("Content-Disposition", "attachment; filename=" + logFile.getName());
    return response.build();
  }

  @GET
  @Path("{stateExecutionId}/api-call-logs")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ThirdPartyApiCallLog>> listLogs(@QueryParam("appId") String appId,
      @PathParam("stateExecutionId") String stateExecutionId, @BeanParam PageRequest<ThirdPartyApiCallLog> request) {
    request.addFilter("appId", EQ, appId);
    request.addFilter("stateExecutionId", EQ, stateExecutionId);
    return new RestResponse<>(thirdPartyApiService.list(request));
  }
}
