package software.wings.beans.command;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SCP")
public class ScpCommandUnitYaml extends SshCommandUnitYaml {
  // maps to fileCategory
  private String source;
  private String destinationDirectoryPath;
  private String artifactVariableName;

  public ScpCommandUnitYaml() {
    super(CommandUnitType.SCP.name());
  }

  @lombok.Builder
  public ScpCommandUnitYaml(
      String name, String deploymentType, String source, String destinationDirectoryPath, String artifactVariableName) {
    super(name, CommandUnitType.SCP.name(), deploymentType);
    this.source = source;
    this.destinationDirectoryPath = destinationDirectoryPath;
    this.artifactVariableName = artifactVariableName;
  }
}
