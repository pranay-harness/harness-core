package software.wings.resources;

import static software.wings.api.JiraExecutionData.JiraApprovalActionType.WAIT_JIRA_APPROVAL;
import static software.wings.service.impl.JiraHelperService.APPROVAL_FIELD_KEY;
import static software.wings.service.impl.JiraHelperService.APPROVAL_ID_KEY;
import static software.wings.service.impl.JiraHelperService.APPROVAL_VALUE_KEY;
import static software.wings.service.impl.JiraHelperService.APP_ID_KEY;
import static software.wings.service.impl.JiraHelperService.WORKFLOW_EXECUTION_ID_KEY;

import com.google.inject.Inject;

import com.auth0.jwt.interfaces.Claim;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.waiter.WaitNotifyEngine;
import io.swagger.annotations.Api;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.http.Body;
import software.wings.api.JiraExecutionData;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.Map;
import java.util.Objects;
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
  public RestResponse<Boolean> pingJiraUpdates(@PathParam("token") String token, @Body String reqJson) {
    try {
      Map<String, Claim> claimMap = jiraHelperService.validateJiraToken(token);
      String appId = claimMap.get(APP_ID_KEY).asString();
      String workflowExecutionId = claimMap.get(WORKFLOW_EXECUTION_ID_KEY).asString();
      String approvalField = claimMap.get(APPROVAL_FIELD_KEY).asString().toLowerCase();
      String approvalValue = claimMap.get(APPROVAL_VALUE_KEY).asString().toLowerCase();
      String approvalId = claimMap.get(APPROVAL_ID_KEY).asString();

      JSONObject jsonObject = JSONObject.fromObject(reqJson);
      JSONObject changeLog = jsonObject.getJSONObject("changelog");
      JSONArray itemsChanged = changeLog.getJSONArray("items");

      boolean isApproved = false;
      for (int i = 0; i < itemsChanged.size(); i++) {
        JSONObject change = itemsChanged.getJSONObject(i);
        if (Objects.equals(change.getString("field").toLowerCase(), approvalField)
            && Objects.equals(change.getString("toString").toLowerCase(), approvalValue)) {
          isApproved = true;
        }
      }

      if (isApproved) {
        JSONObject jiraUser = jsonObject.getJSONObject("user");
        String username = jiraUser.getString("name");
        String email = jiraUser.getString("emailAddress");

        EmbeddedUser user = new EmbeddedUser(null, username, email);

        ApprovalDetails approvalDetails = new ApprovalDetails();
        approvalDetails.setAction(Action.APPROVE);
        approvalDetails.setApprovalId(approvalId);
        approvalDetails.setApprovedBy(user);
        JiraExecutionData executionData = workflowExecutionService.fetchJiraExecutionDataFromWorkflowExecution(
            appId, workflowExecutionId, null, approvalDetails);
        executionData.setStatus(ExecutionStatus.SUCCESS);
        executionData.setApprovedOn(System.currentTimeMillis());
        executionData.setApprovedBy(user);
        executionData.setJiraApprovalActionType(WAIT_JIRA_APPROVAL);
        waitNotifyEngine.notify(approvalId, executionData);
      }
      return new RestResponse<>(isApproved);
    } catch (Exception e) {
      logger.error("Ecxception in TicketingServiceResource: " + e);
      return new RestResponse<>();
    }
  }
}
