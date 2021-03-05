package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
