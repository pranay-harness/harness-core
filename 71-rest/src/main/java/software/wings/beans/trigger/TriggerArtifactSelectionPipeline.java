package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@JsonTypeName("PIPELINE")
public class TriggerArtifactSelectionPipeline implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactVariableType artifactVariableType = ArtifactVariableType.PIPELINE;

  @NotEmpty private String pipelineId;
  private transient String pipelineName;
  private String variableName;
}
