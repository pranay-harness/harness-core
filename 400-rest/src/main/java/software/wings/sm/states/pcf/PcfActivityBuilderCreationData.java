package software.wings.sm.states.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

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
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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
