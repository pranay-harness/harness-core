package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Getter
@OwnedBy(CDC)
@Builder
@FieldNameConstants(innerTypeName = "QLOnPipelineCompletionKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLOnPipelineCompletion implements QLTriggerCondition {
  private QLTriggerConditionType triggerConditionType;
  private String pipelineId;
  private String pipelineName;
}
