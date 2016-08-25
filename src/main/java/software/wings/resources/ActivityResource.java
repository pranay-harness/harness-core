package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SortOrder.Builder.aSortOrder;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.command.CommandUnit;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;

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

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Api("activities")
@Path("/activities")
@Produces("application/json")
//@AuthRule
@Timed
@ExceptionMetered
public class ActivityResource {
  private ActivityService activityService;
  private LogService logService;

  /**
   * Instantiates a new activity resource.
   *
   * @param activityService the activity service
   * @param logService      the log service
   */
  @Inject
  public ActivityResource(ActivityService activityService, LogService logService) {
    this.activityService = activityService;
    this.logService = logService;
  }

  /**
   * List.
   *
   * @param appId   the app id
   * @param envId   the env id
   * @param request the request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Activity>> list(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @BeanParam PageRequest<Activity> request) {
    if (!Strings.isNullOrEmpty(appId)) { // Fixme:: Either make call to appService or let auth layer handle it.
      request.addFilter("appId", appId, EQ);
    }
    if (!Strings.isNullOrEmpty(envId)) {
      request.addFilter("environmentId", envId, EQ);
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
  public RestResponse<PageResponse<Log>> listLogs(@QueryParam("appId") String appId,
      @PathParam("activityId") String activityId, @QueryParam("unitName") String unitName,
      @BeanParam PageRequest<Log> request) {
    request.addFilter("appId", appId, EQ);
    request.addFilter("activityId", activityId, EQ);
    request.addFilter("commandUnitName", unitName, EQ);
    request.addOrder(aSortOrder().withField("createdAt", OrderType.ASC).build());
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
  public RestResponse<List<CommandUnit>> getActivityCommandUnits(
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
  public Response exportLogs(@QueryParam("appId") String appId, @PathParam("activityId") String activityId) {
    File logFile = logService.exportLogs(appId, activityId);
    Response.ResponseBuilder response = Response.ok(logFile, "application/x-unknown");
    response.header("Content-Disposition", "attachment; filename=" + logFile.getName());
    return response.build();
  }
}
