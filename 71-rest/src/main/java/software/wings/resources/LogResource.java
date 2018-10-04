package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.analysis.LogVerificationService;

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
@Scope(ResourceType.APPLICATION)
public class LogResource {
  @Inject private LogService logService;
  @Inject private LogVerificationService logVerificationService;

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
