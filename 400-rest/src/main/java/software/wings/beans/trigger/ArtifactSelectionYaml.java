package software.wings.beans.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ArtifactSelectionYaml extends BaseYaml {
  String type;
  private String artifactStreamName;
  private boolean regex;
  private String artifactFilter;
  String workflowName;
  String pipelineName;
  String serviceName;

  @lombok.Builder
  public ArtifactSelectionYaml(String type, String artifactStreamName, String workflowName, String artifactFilter,
      String serviceName, boolean regex, String pipelineName) {
    this.artifactStreamName = artifactStreamName;
    this.workflowName = workflowName;
    this.pipelineName = pipelineName;
    this.artifactFilter = artifactFilter;
    this.type = type;
    this.regex = regex;
    this.serviceName = serviceName;
  }
}
