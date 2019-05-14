package software.wings.beans.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ArtifactStreamBinding {
  private String artifactStreamId;
}
