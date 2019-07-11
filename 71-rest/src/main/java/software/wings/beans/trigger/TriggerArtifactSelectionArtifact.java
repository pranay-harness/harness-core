package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@JsonTypeName("ARTIFACT")
public class TriggerArtifactSelectionArtifact implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactVariableType artifactVariableType = ArtifactVariableType.ARTIFACT;

  @NotEmpty private String artifactStreamId;
  @NotEmpty private transient String artifactSourceName;
  @NotEmpty private transient String artifactStreamType;
  private String artifactFilter;
}
