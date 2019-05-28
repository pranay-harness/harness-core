package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("NEW_ARTIFACT")
@JsonPropertyOrder({"harnessApiVersion"})
public class ArtifactTriggerConditionYaml extends TriggerConditionYaml {
  private String serviceName;
  private String artifactStreamName;
  private boolean regex;
  private String artifactFilter;

  ArtifactTriggerConditionYaml() {
    super.setType("NEW_ARTIFACT");
  }
  @lombok.Builder
  ArtifactTriggerConditionYaml(String serviceName, String artifactStreamName, boolean regex, String artifactFilter) {
    super.setType("NEW_ARTIFACT");
    this.serviceName = serviceName;
    this.artifactFilter = artifactFilter;
    this.artifactStreamName = artifactStreamName;
    this.regex = regex;
  }
}
