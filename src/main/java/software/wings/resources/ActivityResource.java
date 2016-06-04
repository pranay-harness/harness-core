package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Api("activities")
@Path("/activities")
@Produces("application/json")
@AuthRule
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
    return new RestResponse<>(activityService.list(appId, envId, request));
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
   * @param request    the request
   * @return the rest response
   */
  @GET
  @Path("{activityId}/logs")
  public RestResponse<PageResponse<Log>> listLogs(@QueryParam("appId") String appId,
      @PathParam("activityId") String activityId, @BeanParam PageRequest<Log> request) {
    request.addFilter("appId", appId, EQ);
    request.addFilter("activityId", activityId, EQ);
    return new RestResponse<>(logService.list(request));
  }
}
