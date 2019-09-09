package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.trigger.Condition.Type;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("NEW_ARTIFACT")
@JsonPropertyOrder({"harnessApiVersion"})
public class NewArtifactConditionYaml extends ConditionYaml {
  private String artifactServerName;
  private String artifactStreamName;
  private String artifactStreamType;
  private String artifactFilter;

  NewArtifactConditionYaml() {
    super.setType(Type.NEW_ARTIFACT.name());
  }

  @Builder
  NewArtifactConditionYaml(
      String artifactServerName, String artifactStreamName, String artifactStreamType, String artifactFilter) {
    super.setType(Type.NEW_ARTIFACT.name());
    this.artifactServerName = artifactServerName;
    this.artifactFilter = artifactFilter;
    this.artifactStreamName = artifactStreamName;
    this.artifactStreamType = artifactStreamType;
  }
}
