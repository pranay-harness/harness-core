package software.wings.service.impl.slack;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static software.wings.service.impl.slack.SlackApprovalUtils.approve;
import static software.wings.service.impl.slack.SlackApprovalUtils.createBody;
import static software.wings.service.impl.slack.SlackApprovalUtils.createMessageFromTemplate;
import static software.wings.service.impl.slack.SlackApprovalUtils.slackPostRequest;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.rest.RestResponse;
import okhttp3.RequestBody;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.approval.SlackApprovalParams;
import software.wings.service.impl.notifications.SlackApprovalMessageKeys;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.slack.SlackActionHandler;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(CDC)
public class ProceedResponseHandler implements SlackActionHandler {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private SlackApprovalUtils slackApprovalUtils;

  @Override
  public RestResponse<Boolean> handle(SlackApprovalParams slackApprovalParams, String slackNotificationMessage,
      String sessionTimedOutMessage, String responseUrl) throws IOException {
    // Verifying JWT token
    if (!slackApprovalUtils.verifyJwtToken(slackApprovalParams)) {
      RequestBody sessionTimeOutBody = createBody(sessionTimedOutMessage, true);
      return slackPostRequest(sessionTimeOutBody, responseUrl);
    }

    String slackActionTakerName = slackApprovalParams.getSlackUsername();
    // Creating payload according to action : Approve/Reject
    ApprovalDetails details = new ApprovalDetails();
    StringBuilder displayText = new StringBuilder();
    displayText.append(slackNotificationMessage);
    URL slackActionTemplateUrl = this.getClass().getResource(SlackApprovalMessageKeys.SLACK_ACTION_MESSAGE_TEMPLATE);
    Map<String, String> slackActionTemplateFillers = new HashMap<>();
    slackActionTemplateFillers.put(SlackApprovalMessageKeys.SLACK_USER_ID, slackApprovalParams.getSlackUserId());

    if (slackApprovalParams.isApprove()) {
      details.setAction(Action.APPROVE);
      slackActionTemplateFillers.put(SlackApprovalMessageKeys.ACTION, "Approved");
      details.setComments("Approved by: " + slackActionTakerName + " via Slack.");
    } else {
      details.setAction(Action.REJECT);
      slackActionTemplateFillers.put(SlackApprovalMessageKeys.ACTION, "Rejected");
      details.setComments("Rejected by: " + slackActionTakerName + " via Slack.");
    }

    displayText.append(createMessageFromTemplate(slackActionTemplateUrl, slackActionTemplateFillers));
    details.setApprovalId(slackApprovalParams.getApprovalId());
    details.setApprovedBy(
        EmbeddedUser.builder().uuid(slackApprovalParams.getSlackUsername()).name(slackActionTakerName).build());
    details.setApprovalFromSlack(true);

    // Sending payload to Slack
    ExecutionStatus currentStatus =
        workflowExecutionService
            .getStateExecutionData(slackApprovalParams.getAppId(), slackApprovalParams.getStateExecutionId())
            .getStatus();
    if (currentStatus == ExecutionStatus.PAUSED) {
      approve(slackApprovalParams.getAppId(), slackApprovalParams.getDeploymentId(),
          slackApprovalParams.getStateExecutionId(), details, workflowExecutionService);
      return slackPostRequest(createBody(displayText.toString(), true), responseUrl);
    } else {
      RequestBody alreadyApprovedMessageBody =
          createBody(SlackApprovalMessageKeys.APPROVAL_STATE_CHANGED_MESSAGE, true);
      return slackPostRequest(alreadyApprovedMessageBody, responseUrl);
    }
  }
}