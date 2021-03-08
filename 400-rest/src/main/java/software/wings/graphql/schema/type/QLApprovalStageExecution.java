package software.wings.graphql.schema.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApprovalStageExecutionKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLApprovalStageExecution implements QLPipelineStageExecution {
  private String pipelineStageElementId;
  private String pipelineStepName;
  private String pipelineStageName;
  private QLExecutionStatus status;

  private ApprovalStateType approvalStepType;
}
