package software.wings.beans.approval;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SlackApprovalParams {
  private final String routingId;
  private final String appId;
  private final String appName;
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
  private final String artifactsInvolved;
  private final String workflowUrl;
  private final boolean pipeline;
  private String actionType;
  private String slackUsername;
  private String slackUserId;
  private boolean approve;
  private boolean confirmation;

  // Note: if you add a field above, add it to this method also
  public SlackApprovalParamsBuilder but() {
    return SlackApprovalParams.builder()
        .appId(this.appId)
        .appName(this.appName)
        .routingId(this.routingId)
        .deploymentId(this.deploymentId)
        .workflowId(this.workflowId)
        .workflowExecutionName(this.workflowExecutionName)
        .stateExecutionId(this.stateExecutionId)
        .stateExecutionInstanceName(this.stateExecutionInstanceName)
        .approvalId(this.approvalId)
        .pausedStageName(this.pausedStageName)
        .servicesInvolved(this.servicesInvolved)
        .environmentsInvolved(this.environmentsInvolved)
        .artifactsInvolved(this.artifactsInvolved)
        .confirmation(this.confirmation)
        .pipeline(this.pipeline)
        .workflowUrl(this.workflowUrl)
        .jwtToken(this.jwtToken)
        .slackUsername(this.slackUsername)
        .slackUserId(this.slackUserId)
        .actionType(this.actionType)
        .approve(this.approve)
        .confirmation(this.confirmation);
  }
}
