package software.wings.beans.command;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SetupEnvCommandUnitYaml extends ExecCommandUnitAbstractYaml {
  public SetupEnvCommandUnitYaml() {
    super(CommandUnitType.SETUP_ENV.name());
  }

  @lombok.Builder
  public SetupEnvCommandUnitYaml(String name, String deploymentType, String workingDirectory, String scriptType,
      String command, List<TailFilePatternEntryYaml> filePatternEntryList) {
    super(name, CommandUnitType.SETUP_ENV.name(), deploymentType, workingDirectory, scriptType, command,
        filePatternEntryList);
  }
}
