package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@JsonTypeName("PIPELINE_SOURCE")
public class TriggerArtifactSelectionFromPipelineSource implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactSelectionType artifactSelectionType = ArtifactSelectionType.PIPELINE_SOURCE;
}
