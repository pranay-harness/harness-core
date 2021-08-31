package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("NEW_ARTIFACT")
@JsonPropertyOrder({"harnessApiVersion"})
@TargetModule(HarnessModule._815_CG_TRIGGERS)
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
