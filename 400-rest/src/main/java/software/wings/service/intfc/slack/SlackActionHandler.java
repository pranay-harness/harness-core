/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.slack;

import io.harness.rest.RestResponse;

import software.wings.beans.approval.SlackApprovalParams;

import java.io.IOException;

public interface SlackActionHandler {
  RestResponse<Boolean> handle(SlackApprovalParams.External slackApprovalParams, String slackNotificationMessage,
      String sessionTimedOutMessage, String responseUrl) throws IOException;
}
