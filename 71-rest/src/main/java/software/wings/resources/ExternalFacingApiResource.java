package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.exception.InvalidArgumentsException;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ExecutionStatusResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.security.annotations.ExternalFacingApiAuth;
import software.wings.service.intfc.WorkflowExecutionService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/external")
@Path("/external/{version}")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@ExternalFacingApiAuth
public class ExternalFacingApiResource {
  private WorkflowExecutionService workflowExecutionService;

  @Inject
  public ExternalFacingApiResource(WorkflowExecutionService workflowExecutionService) {
    this.workflowExecutionService = workflowExecutionService;
  }

  @GET
  @Path("/executions/{workflowExecutionId}/status")
  @Timed
  @ExceptionMetered
  public ExecutionStatusResponse getWorkFlowExecutionStatus(
      @NotEmpty @PathParam("workflowExecutionId") String workflowExecutionId,
      @NotEmpty @QueryParam("accountId") String accountId, @NotEmpty @QueryParam("appId") String appId) {
    WorkflowExecution execution = workflowExecutionService.getExecutionDetailsWithoutGraph(appId, workflowExecutionId);
    if (execution == null) {
      throw new InvalidArgumentsException(Pair.of("Application Id", appId),
          Pair.of("Workflow Execution Id", workflowExecutionId),
          new IllegalArgumentException("Invalid App Id Or Workflow execution Id"));
    }
    return ExecutionStatusResponse.builder().status(execution.getStatus().name()).build();
  }
}