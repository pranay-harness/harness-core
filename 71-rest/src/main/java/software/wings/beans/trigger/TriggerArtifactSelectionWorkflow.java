package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@JsonTypeName("ORCHESTRATION")
public class TriggerArtifactSelectionWorkflow implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactVariableType artifactVariableType = ArtifactVariableType.ORCHESTRATION;

  @NotEmpty private String workflowId;
  private transient String workflowName;
  private String variableName;
}
