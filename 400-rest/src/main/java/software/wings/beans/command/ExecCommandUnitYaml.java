package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("EXEC")
public class ExecCommandUnitYaml extends ExecCommandUnitAbstractYaml {
  public ExecCommandUnitYaml() {
    super(CommandUnitType.EXEC.name());
  }

  @lombok.Builder
  public ExecCommandUnitYaml(String name, String deploymentType, String workingDirectory, String scriptType,
      String command, List<TailFilePatternEntryYaml> filePatternEntryList) {
    super(
        name, CommandUnitType.EXEC.name(), deploymentType, workingDirectory, scriptType, command, filePatternEntryList);
  }
}
