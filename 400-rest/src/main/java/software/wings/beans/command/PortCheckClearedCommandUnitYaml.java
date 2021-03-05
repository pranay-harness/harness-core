package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("PORT_CHECK_CLEARED")
public class PortCheckClearedCommandUnitYaml extends ExecCommandUnitAbstractYaml {
  public PortCheckClearedCommandUnitYaml() {
    super(CommandUnitType.PORT_CHECK_CLEARED.name());
  }

  @lombok.Builder
  public PortCheckClearedCommandUnitYaml(String name, String deploymentType, String workingDirectory, String scriptType,
      String command, List<TailFilePatternEntryYaml> filePatternEntryList) {
    super(name, CommandUnitType.PORT_CHECK_CLEARED.name(), deploymentType, workingDirectory, scriptType, command,
        filePatternEntryList);
  }
}
