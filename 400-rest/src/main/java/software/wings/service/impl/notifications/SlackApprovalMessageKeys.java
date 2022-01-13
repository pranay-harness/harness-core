/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.notifications;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class SlackApprovalMessageKeys {
  // The paths to pick the Templates of the messages and the JSON payloads

  public static final String MESSAGE_IDENTIFIER = "suppressTraditionalApprovalNotification";
  public static final String APPROVAL_MESSAGE_PAYLOAD_TEMPLATE = "/slack/approval-message.json";
  public static final String APPROVAL_MESSAGE_WITHOUT_CHANNEL_NAME_PAYLOAD_TEMPLATE =
      "/slack/approval-message-without-channel-name.json";
  public static final String APPROVAL_MESSAGE_WITHOUT_BUTTONS_PAYLOAD_TEMPLATE =
      "/slack/approval-message-without-buttons.json";
  public static final String APPROVAL_MESSAGE_WITHOUT_BUTTONS_WITHOUT_CHANNEL_NAME_PAYLOAD_TEMPLATE =
      "/slack/approval-message-without-buttons-and-channel-name.json";
  public static final String PIPELINE_APPROVAL_MESSAGE_TEMPLATE = "/slack/pipeline-approval-message.txt";
  public static final String WORKFLOW_APPROVAL_MESSAGE_TEMPLATE = "/slack/workflow-approval-message.txt";
  public static final String APPROVAL_EXPIRED_MESSAGE_TEMPLATE = "/slack/approval-expired-message.txt";
  public static final String ADDITIONAL_CONFIRMATION_MESSAGE_TEMPLATE = "/slack/additional-confirmation.json";
  public static final String SLACK_ACTION_MESSAGE_TEMPLATE = "/slack/slack-action-message.txt";
  public static final String APPROVAL_STATE_CHANGED_MESSAGE =
      "The Approval state can no longer be *Approved/Rejected*\nThe state has been *modified* through *Harness UI*.";
  public static final String APPROVAL_STATE_CHANGED_MESSAGE_VIA_API =
      "The Approval state can no longer be *Approved/Rejected*\nThe state has been *modified* through *Harness API*.";

  // Slack Message template keys
  // Note: Update these keys if any changes to slack message templates is made
  // pipeline-approval-message.txt and workflow-approval-message.txt template placeholder keys
  public static final String PAUSED_STAGE_NAME = "pausedStageName";
  public static final String WORKFLOW_URL = "workflowUrl";
  public static final String WORKFLOW_EXECUTION_NAME = "workflowExecutionName";
  public static final String APP_NAME = "appName";
  public static final String SERVICES = "services";
  public static final String ENVIRONMENTS = "environments";
  public static final String INFRASTRUCTURE_DEFINITIONS = "infrastructureDefinitions";
  public static final String ARTIFACTS = "artifacts";

  // approval-message.json and additional-confirmation.json template placeholder keys
  public static final String APPROVAL_MESSAGE = "approvalMessage";
  public static final String CONFIRMATION_MESSAGE = "confirmationMessage";
  public static final String SLACK_APPROVAL_PARAMS = "slackApprovalParams";
  public static final String BUTTON_ACCEPT = "button_accept";
  public static final String BUTTON_PROCEED = "button_proceed";

  // slack-action-message.txt template placeholder keys
  public static final String ACTION = "action";
  public static final String SLACK_USER_ID = "slackUserId";
  public static final String START_TS_SECS = "START_TS_SECS";
  public static final String END_TS_SECS = "END_TS_SECS";
  public static final String EXPIRES_TS_SECS = "EXPIRES_TS_SECS";
  public static final String START_DATE = "START_DATE";
  public static final String END_DATE = "END_DATE";
  public static final String EXPIRES_DATE = "EXPIRES_DATE";
  public static final String VERB = "VERB";

  // Paused Stage name Values
  public static final String PAUSED_STAGE_NAME_DEFAULT = " ";
}
