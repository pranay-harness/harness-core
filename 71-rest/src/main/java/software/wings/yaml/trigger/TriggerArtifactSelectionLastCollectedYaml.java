package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.TriggerArtifactSelectionValue.ArtifactSelectionType;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("LAST_COLLECTED")
@JsonPropertyOrder({"harnessApiVersion"})
public class TriggerArtifactSelectionLastCollectedYaml extends TriggerArtifactSelectionValueYaml {
  private String artifactStreamName;
  private String artifactFilter;
  private String artifactStreamType;
  private String artifactServerName;

  public TriggerArtifactSelectionLastCollectedYaml() {
    super.setType(ArtifactSelectionType.LAST_COLLECTED.name());
  }

  @Builder
  public TriggerArtifactSelectionLastCollectedYaml(
      String artifactStreamName, String artifactFilter, String artifactStreamType, String artifactServerName) {
    super.setType(ArtifactSelectionType.LAST_COLLECTED.name());
    this.artifactStreamName = artifactStreamName;
    this.artifactFilter = artifactFilter;
    this.artifactStreamType = artifactStreamType;
    this.artifactServerName = artifactServerName;
  }
}
