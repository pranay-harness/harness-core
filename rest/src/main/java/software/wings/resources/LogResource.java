package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.intfc.LogService;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
@Api("logs")
@Path("/logs")
@Produces("application/json")
@AuthRule(ResourceType.APPLICATION)
public class LogResource {
  private LogService logService;

  @Inject
  public LogResource(LogService logService) {
    this.logService = logService;
  }

  @DelegateAuth
  @POST
  @Path("batched")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> batchSave(List<Log> logs) {
    logService.batchedSave(logs);
    return new RestResponse<>(new ArrayList<>());
  }

  @DelegateAuth
  @POST
  @Path("activity/{activityId}/unit/{unitName}/batched")
  @Timed
  @ExceptionMetered
  public RestResponse<String> batchSave(
      @PathParam("activityId") String activityId, @PathParam("unitName") String unitName, Log log) {
    return new RestResponse<>(logService.batchedSaveCommandUnitLogs(activityId, unitName, log));
  }
}
