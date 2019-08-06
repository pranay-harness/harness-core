package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.CVActivityLog;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(VerificationConstants.CV_ACTIVITY_LOGS_PATH)
@Path(VerificationConstants.CV_ACTIVITY_LOGS_PATH)
@Produces("application/json")
@Scope(ResourceType.SERVICE)
public class CVActivityLogResource {
  @Inject private CVActivityLogService cvActivityLogService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVActivityLog>> getActivityLogs(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("stateExecutionId") final String stateExecutionId,
      @QueryParam("cvConfigId") @Valid final String cvConfigId, @QueryParam("startTime") long startTime,
      @QueryParam("endTime") long endTime) {
    List<CVActivityLog> cvActivityLogs;
    if (stateExecutionId != null) {
      cvActivityLogs = cvActivityLogService.findByStateExecutionId(stateExecutionId);
    } else {
      cvActivityLogs = cvActivityLogService.findByCVConfigId(cvConfigId, startTime, endTime);
    }
    return new RestResponse<>(cvActivityLogs);
  }
}
