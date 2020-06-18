package software.wings.graphql.schema.type.trigger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLTriggerActionInput {
  private QLExecutionType executionType;
  private String entityId;
  private List<QLVariableInput> variables;
  private List<QLArtifactSelectionInput> artifactSelections;
}
