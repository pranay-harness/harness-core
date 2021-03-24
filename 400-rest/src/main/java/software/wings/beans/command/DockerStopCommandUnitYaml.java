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
@JsonTypeName("DOCKER_STOP")
public class DockerStopCommandUnitYaml extends ExecCommandUnitAbstractYaml {
  public DockerStopCommandUnitYaml() {
    super(CommandUnitType.DOCKER_STOP.name());
  }

  @lombok.Builder
  public DockerStopCommandUnitYaml(String name, String deploymentType, String workingDirectory, String scriptType,
      String command, List<TailFilePatternEntryYaml> filePatternEntryList) {
    super(name, CommandUnitType.DOCKER_STOP.name(), deploymentType, workingDirectory, scriptType, command,
        filePatternEntryList);
  }
}
