package software.wings.graphql.schema.type.trigger;

import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLTriggerActionInput {
  QLExecutionType executionType;
  String entityId;
  List<QLVariableInput> variables;
  List<QLArtifactSelectionInput> artifactSelections;
  Boolean excludeHostsWithSameArtifact;
  private Boolean continueWithDefaultValues;
}
