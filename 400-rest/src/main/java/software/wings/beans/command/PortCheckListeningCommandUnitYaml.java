package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("PORT_CHECK_LISTENING")
public class PortCheckListeningCommandUnitYaml extends ExecCommandUnitAbstractYaml {
  public PortCheckListeningCommandUnitYaml() {
    super(CommandUnitType.PORT_CHECK_LISTENING.name());
  }

  @lombok.Builder
  public PortCheckListeningCommandUnitYaml(String name, String deploymentType, String workingDirectory,
      String scriptType, String command, List<TailFilePatternEntryYaml> filePatternEntryList) {
    super(name, CommandUnitType.PORT_CHECK_LISTENING.name(), deploymentType, workingDirectory, scriptType, command,
        filePatternEntryList);
  }
}
