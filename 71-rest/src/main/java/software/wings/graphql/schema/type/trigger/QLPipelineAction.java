package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPipelineActionKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLPipelineAction implements QLTriggerAction {
  String pipelineId;
  String pipelineName;
  List<QLTriggerVariableValue> variables;
  List<QLArtifactSelection> artifactSelections;
  private Boolean continueWithDefaultValues;
}
