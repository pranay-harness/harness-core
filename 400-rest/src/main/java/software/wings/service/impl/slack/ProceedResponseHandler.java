/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.slack;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.service.impl.slack.SlackApprovalUtils.approve;
import static software.wings.service.impl.slack.SlackApprovalUtils.createBody;
import static software.wings.service.impl.slack.SlackApprovalUtils.createMessageFromTemplate;
import static software.wings.service.impl.slack.SlackApprovalUtils.slackPostRequest;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.rest.RestResponse;

import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.approval.SlackApprovalParams;
import software.wings.service.impl.notifications.SlackApprovalMessageKeys;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.slack.SlackActionHandler;
import software.wings.sm.StateExecutionInstance;

import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import okhttp3.RequestBody;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ProceedResponseHandler implements SlackActionHandler {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private SlackApprovalUtils slackApprovalUtils;

  @Override
  public RestResponse<Boolean> handle(SlackApprovalParams.External slackApprovalParams, String slackNotificationMessage,
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
    StateExecutionInstance stateExecutionInstance = workflowExecutionService.getStateExecutionData(
        slackApprovalParams.getAppId(), slackApprovalParams.getStateExecutionId());
    ExecutionStatus currentStatus = stateExecutionInstance.getStatus();
    if (currentStatus == ExecutionStatus.PAUSED) {
      approve(slackApprovalParams.getAppId(), slackApprovalParams.getDeploymentId(),
          slackApprovalParams.getStateExecutionId(), details, workflowExecutionService);
      return slackPostRequest(
          createBody(SlackApprovalUtils.resetToInitialMessage(displayText.toString()), true), responseUrl);
    } else {
      RequestBody alreadyApprovedMessageBody = null;
      if (stateExecutionInstance.fetchStateExecutionData() instanceof ApprovalStateExecutionData) {
        ApprovalStateExecutionData approvalStateExecutionData =
            (ApprovalStateExecutionData) stateExecutionInstance.fetchStateExecutionData();
        String alreadyApprovedMessageString = approvalStateExecutionData.isApprovalFromGraphQL()
            ? SlackApprovalMessageKeys.APPROVAL_STATE_CHANGED_MESSAGE_VIA_API
            : SlackApprovalMessageKeys.APPROVAL_STATE_CHANGED_MESSAGE;
        alreadyApprovedMessageBody = createBody(alreadyApprovedMessageString, true);
        return slackPostRequest(alreadyApprovedMessageBody, responseUrl);
      }
      return slackPostRequest(createBody(SlackApprovalMessageKeys.APPROVAL_STATE_CHANGED_MESSAGE, true), responseUrl);
    }
  }
}
