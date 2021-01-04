package software.wings.sm.states.pcf;

import software.wings.beans.Activity.Type;
import software.wings.beans.Environment;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.sm.ExecutionContext;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfActivityBuilderCreationData {
  private String appName;
  private String appId;
  private String commandName;
  private Type type;
  private ExecutionContext executionContext;
  private String commandType;
  private CommandUnitType commandUnitType;
  private Environment environment;
  private List<CommandUnit> commandUnits;
}
