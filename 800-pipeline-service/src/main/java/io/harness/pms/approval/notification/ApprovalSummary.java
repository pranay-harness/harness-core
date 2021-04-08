package io.harness.pms.approval.notification;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "ApprovalSummaryKeys")
public class ApprovalSummary {
  String pipelineName;
  String projectName;
  String approvalMessage;
  String startedAt;
  String triggeredBy;
  Set<String> finishedStages;
  Set<String> runningStages;
  Set<String> upcomingStages;

  String pipelineExecutionLink;
  String timeRemainingForApproval;

  public Map<String, String> toParams() {
    Map<String, String> params = new HashMap<>();
    params.put(ApprovalSummaryKeys.pipelineName, pipelineName);
    params.put(ApprovalSummaryKeys.projectName, projectName);
    params.put(ApprovalSummaryKeys.approvalMessage, approvalMessage);
    params.put(ApprovalSummaryKeys.startedAt, startedAt);
    params.put(ApprovalSummaryKeys.triggeredBy, triggeredBy);
    params.put(ApprovalSummaryKeys.finishedStages, String.join(",", finishedStages));
    params.put(ApprovalSummaryKeys.runningStages, String.join(",", runningStages));
    params.put(ApprovalSummaryKeys.upcomingStages, String.join(",", upcomingStages));

    params.put(ApprovalSummaryKeys.pipelineExecutionLink, pipelineExecutionLink);
    params.put(ApprovalSummaryKeys.timeRemainingForApproval, timeRemainingForApproval);
    return params;
  }
}
