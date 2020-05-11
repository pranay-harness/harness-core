package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
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
}
