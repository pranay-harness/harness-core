/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.slack;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.service.impl.slack.SlackApprovalUtils.createBody;
import static software.wings.service.impl.slack.SlackApprovalUtils.createMessageFromTemplate;
import static software.wings.service.impl.slack.SlackApprovalUtils.slackPostRequest;
import static software.wings.sm.states.ApprovalState.JSON;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.rest.RestResponse;

import software.wings.api.ApprovalStateExecutionData;
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
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ConfirmationHandler implements SlackActionHandler {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private SlackApprovalUtils slackApprovalUtils;

  @Override
  public RestResponse<Boolean> handle(final SlackApprovalParams.External slackApprovalParams,
      String slackNotificationMessage, String sessionTimedOutMessage, String responseUrl) throws IOException {
    boolean approve = slackApprovalParams.getActionType().equals(SlackApprovalMessageKeys.BUTTON_ACCEPT);
    final SlackApprovalParams.External confirmationParams =
        slackApprovalParams.toBuilder().confirmation(true).approve(approve).build();

    // Verifying JWT token
    if (!slackApprovalUtils.verifyJwtToken(slackApprovalParams)) {
      RequestBody sessionTimeOutBody = createBody(sessionTimedOutMessage, true);
      return slackPostRequest(sessionTimeOutBody, responseUrl);
    }

    String buttonValue = new JSONObject(confirmationParams).toString();
    buttonValue = StringEscapeUtils.escapeJson(buttonValue);
    String confirmationMessage = slackNotificationMessage + "\nAre you sure you want to "
        + (confirmationParams.getActionType().equals(SlackApprovalMessageKeys.BUTTON_ACCEPT) ? "*Approve*" : "*Reject*")
        + " ?";

    // Creating APPLICATION_JSON payload from additional-confirmation.json template
    URL templateUrl = this.getClass().getResource(SlackApprovalMessageKeys.ADDITIONAL_CONFIRMATION_MESSAGE_TEMPLATE);
    Map<String, String> templateFillers = new HashMap<>();
    templateFillers.put(SlackApprovalMessageKeys.CONFIRMATION_MESSAGE, confirmationMessage);
    templateFillers.put(SlackApprovalMessageKeys.SLACK_APPROVAL_PARAMS, buttonValue);
    String confirmationPayload = createMessageFromTemplate(templateUrl, templateFillers);

    // Sending payload to Slack
    StateExecutionInstance stateExecutionInstance = workflowExecutionService.getStateExecutionData(
        confirmationParams.getAppId(), confirmationParams.getStateExecutionId());
    ExecutionStatus currentStatus = stateExecutionInstance.getStatus();
    if (currentStatus == ExecutionStatus.PAUSED) {
      RequestBody confirmationBody = RequestBody.create(JSON, confirmationPayload);
      return slackPostRequest(confirmationBody, responseUrl);
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
