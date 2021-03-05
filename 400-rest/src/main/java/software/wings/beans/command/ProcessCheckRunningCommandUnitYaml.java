package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("PROCESS_CHECK_RUNNING")
public class ProcessCheckRunningCommandUnitYaml extends ExecCommandUnitAbstractYaml {
  public ProcessCheckRunningCommandUnitYaml() {
    super(CommandUnitType.PROCESS_CHECK_RUNNING.name());
  }

  @lombok.Builder
  public ProcessCheckRunningCommandUnitYaml(String name, String deploymentType, String workingDirectory,
      String scriptType, String command, List<TailFilePatternEntryYaml> filePatternEntryList) {
    super(name, CommandUnitType.PROCESS_CHECK_RUNNING.name(), deploymentType, workingDirectory, scriptType, command,
        filePatternEntryList);
  }
}
