package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowActionKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLWorkflowAction implements QLTriggerAction {
  String workflowId;
  String workflowName;
  List<QLTriggerVariableValue> variables;
  List<QLArtifactSelection> artifactSelections;
}
