package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
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

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Path("/activities")
@Produces("application/json")
@AuthRule
@Timed
@ExceptionMetered
public class ActivityResource {
  private ActivityService activityService;
  private LogService logService;

  @Inject
  public ActivityResource(ActivityService activityService, LogService logService) {
    this.activityService = activityService;
    this.logService = logService;
  }

  @GET
  public RestResponse<PageResponse<Activity>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Activity> request) {
    request.addFilter("appId", appId, EQ);
    return new RestResponse<>(activityService.list(request));
  }

  @GET
  @Path("{activityId}")
  public RestResponse<Activity> get(@QueryParam("appId") String appId, @PathParam("activityId") String activityId) {
    return new RestResponse<>(activityService.get(activityId, appId));
  }

  @GET
  @Path("{activityId}/logs")
  public RestResponse<PageResponse<Log>> listLogs(@QueryParam("appId") String appId,
      @PathParam("activityId") String activityId, @BeanParam PageRequest<Log> request) {
    request.addFilter("appId", appId, EQ);
    request.addFilter("activityId", activityId, EQ);
    return new RestResponse<>(logService.list(request));
  }
}
