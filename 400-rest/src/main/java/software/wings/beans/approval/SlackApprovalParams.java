/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.modelmapper.ModelMapper;

@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
public class SlackApprovalParams {
  private final String routingId;
  private final String appId;
  private final String appName;
  private final String nonFormattedAppName;
  private final String workflowId;
  private final String deploymentId;
  private final String approvalId;
  private final String stateExecutionId;
  private final String workflowExecutionName;
  private final String stateExecutionInstanceName;
  private final String jwtToken;
  private final String pausedStageName;
  private final String servicesInvolved;
  private final String environmentsInvolved;
  private final String infraDefinitionsInvolved;
  private final String artifactsInvolved;
  private final String workflowUrl;
  private final boolean pipeline;
  private String actionType;
  private String slackUsername;
  private String slackUserId;
  private boolean approve;
  private boolean confirmation;
  private String startTsSecs;
  private String endTsSecs;
  private String expiryTsSecs;
  private String startDate;
  private String endDate;
  private String expiryDate;
  private String verb;

  @Data
  @Builder(toBuilder = true)
  @NoArgsConstructor
  @AllArgsConstructor
  public static class External {
    private String routingId;
    private String appId;
    private String appName;
    private String nonFormattedAppName;
    private String workflowId;
    private String deploymentId;
    private String approvalId;
    private String stateExecutionId;
    private String workflowExecutionName;
    private String jwtToken;
    private boolean pipeline;
    private String actionType;
    private String slackUsername;
    private String slackUserId;
    private boolean approve;
    private boolean confirmation;
  }

  public static External getExternalParams(SlackApprovalParams slackApprovalParams) {
    ModelMapper mapper = new ModelMapper();
    return mapper.map(slackApprovalParams, External.class);
  }
}
