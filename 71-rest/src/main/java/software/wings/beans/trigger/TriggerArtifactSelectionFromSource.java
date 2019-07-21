package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@JsonTypeName("ARTIFACT_SOURCE")
public class TriggerArtifactSelectionFromSource implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactSelectionType artifactSelectionType = ArtifactSelectionType.ARTIFACT_SOURCE;
}
