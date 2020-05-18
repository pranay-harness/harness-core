package software.wings.service.impl.slack;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static software.wings.service.impl.slack.SlackApprovalUtils.createBody;
import static software.wings.service.impl.slack.SlackApprovalUtils.createMessageFromTemplate;
import static software.wings.service.impl.slack.SlackApprovalUtils.slackPostRequest;
import static software.wings.sm.states.ApprovalState.JSON;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.rest.RestResponse;
import okhttp3.RequestBody;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import software.wings.beans.approval.SlackApprovalParams;
import software.wings.service.impl.notifications.SlackApprovalMessageKeys;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.slack.SlackActionHandler;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(CDC)
public class RevertResponseHandler implements SlackActionHandler {
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

    // Creating json payload
    final SlackApprovalParams revertParams = slackApprovalParams.toBuilder().confirmation(false).build();
    String buttonValue = new JSONObject(revertParams).toString();
    buttonValue = StringEscapeUtils.escapeJson(buttonValue);

    URL templateUrl = this.getClass().getResource(SlackApprovalMessageKeys.APPROVAL_MESSAGE_PAYLOAD_TEMPLATE);
    Map<String, String> templateFillers = new HashMap<>();
    templateFillers.put(SlackApprovalMessageKeys.APPROVAL_MESSAGE, slackNotificationMessage);
    templateFillers.put(SlackApprovalMessageKeys.SLACK_APPROVAL_PARAMS, buttonValue);
    String approvalPayload = createMessageFromTemplate(templateUrl, templateFillers);

    ExecutionStatus currentStatus =
        workflowExecutionService.getStateExecutionData(revertParams.getAppId(), revertParams.getStateExecutionId())
            .getStatus();
    if (currentStatus == ExecutionStatus.PAUSED) {
      RequestBody approvalBody = RequestBody.create(JSON, approvalPayload);
      return slackPostRequest(approvalBody, responseUrl);
    } else {
      RequestBody alreadyApprovedMessageBody =
          createBody(SlackApprovalMessageKeys.APPROVAL_STATE_CHANGED_MESSAGE, true);
      return slackPostRequest(alreadyApprovedMessageBody, responseUrl);
    }
  }
}