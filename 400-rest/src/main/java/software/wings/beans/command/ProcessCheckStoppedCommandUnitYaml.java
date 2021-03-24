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
@JsonTypeName("PROCESS_CHECK_STOPPED")
public class ProcessCheckStoppedCommandUnitYaml extends ExecCommandUnitAbstractYaml {
  public ProcessCheckStoppedCommandUnitYaml() {
    super(CommandUnitType.PROCESS_CHECK_STOPPED.name());
  }

  @lombok.Builder
  public ProcessCheckStoppedCommandUnitYaml(String name, String deploymentType, String workingDirectory,
      String scriptType, String command, List<TailFilePatternEntryYaml> filePatternEntryList) {
    super(name, CommandUnitType.PROCESS_CHECK_STOPPED.name(), deploymentType, workingDirectory, scriptType, command,
        filePatternEntryList);
  }
}
