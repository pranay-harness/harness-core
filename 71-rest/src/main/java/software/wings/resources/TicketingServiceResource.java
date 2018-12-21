package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.ExecutionStatus;
import io.harness.waiter.WaitNotifyEngine;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.http.Body;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.WorkflowExecutionService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Api("ticketing")
@Path("/ticketing")
@Consumes("application/json")
@Produces("application/json")
public class TicketingServiceResource {
  private JiraHelperService jiraHelperService;
  private WorkflowExecutionService workflowExecutionService;
  private WaitNotifyEngine waitNotifyEngine;
  private static final Logger logger = LoggerFactory.getLogger(TicketingServiceResource.class);

  @Inject
  public TicketingServiceResource(JiraHelperService jiraHelperService,
      WorkflowExecutionService workflowExecutionService, WaitNotifyEngine waitNotifyEngine) {
    this.workflowExecutionService = workflowExecutionService;
    this.jiraHelperService = jiraHelperService;
    this.waitNotifyEngine = waitNotifyEngine;
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/jira-approval/{token}")
  @PublicApi
  public RestResponse<ExecutionStatus> jiraWebhookCallback(@PathParam("token") String token, @Body String reqJson) {
    try {
      return new RestResponse<>(jiraHelperService.checkApprovalFromWebhookCallback(token, reqJson));
    } catch (Exception e) {
      logger.error("Ecxception in TicketingServiceResource: " + e);
      return new RestResponse<>();
    }
  }
}
