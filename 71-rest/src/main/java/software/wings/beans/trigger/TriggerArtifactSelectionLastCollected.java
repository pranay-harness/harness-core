package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@JsonTypeName("LAST_COLLECTED")
public class TriggerArtifactSelectionLastCollected implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactSelectionType artifactSelectionType = ArtifactSelectionType.LAST_COLLECTED;
  @NotEmpty private String artifactStreamId;
  @NotEmpty private String artifactServerId;
  private transient String artifactStreamName;
  private transient String artifactServerName;
  private transient String artifactStreamType;
  private String artifactFilter;
}
