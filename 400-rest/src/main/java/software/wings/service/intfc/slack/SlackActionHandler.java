package software.wings.service.intfc.slack;

import io.harness.rest.RestResponse;

import software.wings.beans.approval.SlackApprovalParams;

import java.io.IOException;

public interface SlackActionHandler {
  RestResponse<Boolean> handle(SlackApprovalParams.External slackApprovalParams, String slackNotificationMessage,
      String sessionTimedOutMessage, String responseUrl) throws IOException;
}
