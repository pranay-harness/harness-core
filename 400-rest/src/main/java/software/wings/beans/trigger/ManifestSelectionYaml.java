package software.wings.beans.trigger;

import io.harness.yaml.BaseYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ManifestSelectionYaml extends BaseYaml {
  String type;
  private String versionRegex;
  String workflowName;
  String pipelineName;
  String serviceName;

  @lombok.Builder
  public ManifestSelectionYaml(
      String type, String workflowName, String versionRegex, String serviceName, String pipelineName) {
    this.workflowName = workflowName;
    this.pipelineName = pipelineName;
    this.versionRegex = versionRegex;
    this.type = type;
    this.serviceName = serviceName;
  }
}
