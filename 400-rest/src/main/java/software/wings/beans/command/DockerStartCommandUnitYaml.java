package software.wings.beans.command;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("DOCKER_START")
public class DockerStartCommandUnitYaml extends ExecCommandUnitAbstractYaml {
  public DockerStartCommandUnitYaml() {
    super(CommandUnitType.DOCKER_START.name());
  }

  @lombok.Builder
  public DockerStartCommandUnitYaml(String name, String deploymentType, String workingDirectory, String scriptType,
      String command, List<TailFilePatternEntry.Yaml> filePatternEntryList) {
    super(name, CommandUnitType.DOCKER_START.name(), deploymentType, workingDirectory, scriptType, command,
        filePatternEntryList);
  }
}
