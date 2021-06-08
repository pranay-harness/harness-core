package io.harness.ng.activitytracker;

import io.harness.NGResourceFilterConstants;
import io.harness.ng.activitytracker.models.apiresponses.ActivityHistoryDetailsResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsByProjectResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsByUserResponse;
import io.harness.ng.activitytracker.models.apiresponses.StatsDetailsResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("activitytracker")
@Path("/activitytracker")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class ActivityTrackerResource {
  @Inject ActivityHistoryService activityHistoryServiceImpl;

  @GET
  @Path("/statsdetails")
  @ApiOperation(value = "Get activity stats", nickname = "getActivityStats")
  public ResponseDTO<StatsDetailsResponse> getActivityStats(@QueryParam("projectId") String projectId,
      @QueryParam("userId") String userId, @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endTime) {
    return ResponseDTO.newResponse(activityHistoryServiceImpl.getStatsDetails(projectId, userId, startTime, endTime));
  }

  @GET
  @Path("/statsdetailsbyusers")
  @ApiOperation(value = "Get activity stats by users", nickname = "getActivityStatsByUsers")
  public ResponseDTO<StatsDetailsByUserResponse> getActivityStatsByUsers(@QueryParam("projectId") String projectId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endTime) {
    return ResponseDTO.newResponse(activityHistoryServiceImpl.getStatsDetailsByUsers(projectId, startTime, endTime));
  }

  @GET
  @Path("/statsdetailsbyprojects")
  @ApiOperation(value = "Get activity stats by projects", nickname = "getActivityStatsByProjects")
  public ResponseDTO<StatsDetailsByProjectResponse> getActivityStatsByProjects(@QueryParam("userId") String userId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endTime) {
    return ResponseDTO.newResponse(activityHistoryServiceImpl.getStatsDetailsByProjects(userId, startTime, endTime));
  }

  @GET
  @Path("/activityhistory")
  @ApiOperation(value = "Get activity hiroty", nickname = "getActivityHistory")
  public ResponseDTO<ActivityHistoryDetailsResponse> getActivityHistory(@QueryParam("projectId") String projectId,
      @QueryParam("userId") String userId, @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endTime) {
    return ResponseDTO.newResponse(
        activityHistoryServiceImpl.getActivityHistoryDetails(projectId, userId, startTime, endTime));
  }
}