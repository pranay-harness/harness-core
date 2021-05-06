package software.wings.resources.cvng;

import static io.harness.annotations.dev.HarnessTeam.CV;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.client.CVNGService;
import io.harness.rest.RestResponse;

import software.wings.common.VerificationConstants;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.DeploymentAuthHandler;
import software.wings.service.intfc.cvng.CVNGStateService;
import software.wings.sm.states.CVNGState.StepStatus;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cvng-state")
@Path("/cvng-state")
@Produces("application/json")
@Scope(SETTING)
@OwnedBy(CV)
public class CVNGStateResource {
  @Inject private CVNGService cvngService;
  @Inject private DeploymentAuthHandler deploymentAuthHandler;
  @Inject private CVNGStateService cvngStateService;

  @GET
  @Path("/verification-jobs")
  @Timed
  @ExceptionMetered
  public RestResponse<VerificationJobDTO> getVerificationJobForUrl(
      @QueryParam("accountId") String accountId, @QueryParam("verificationJobUrl") @NotNull String verificationJobUrl) {
    return new RestResponse<>(cvngService.getVerificationJobs(accountId, verificationJobUrl));
  }

  @POST
  @Path(VerificationConstants.NOTIFY_WORKFLOW_CVNG_STATE)
  @Timed
  public RestResponse<Void> notifyWorkflowCVNGState(@QueryParam("accountId") String accountId,
      @Valid @QueryParam("appId") String appId, @Valid @QueryParam("workflowId") String workflowId,
      @Valid @QueryParam("workflowExecutionId") String workflowExecutionId,
      @Valid @QueryParam("activityId") String activityId, @Valid @QueryParam("status") StepStatus status) {
    deploymentAuthHandler.authorizeWithWorkflowExecutionId(appId, workflowExecutionId);
    cvngStateService.notifyWorkflowCVNGState(activityId, status);
    return new RestResponse<>();
  }
}