package software.wings.beans.command;

import static software.wings.sm.states.HelmDeployState.HELM_COMMAND_NAME;
import static software.wings.sm.states.JenkinsState.COMMAND_UNIT_NAME;
import static software.wings.sm.states.KubernetesSteadyStateCheck.KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME;
import static software.wings.sm.states.pcf.MapRouteState.PCF_MAP_ROUTE_COMMAND;
import static software.wings.sm.states.pcf.PcfDeployState.PCF_RESIZE_COMMAND;
import static software.wings.sm.states.pcf.PcfSetupState.PCF_SETUP_COMMAND;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

/**
 * Created by rsingh on 11/17/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandUnitDetails {
  private String name;
  private CommandExecutionStatus commandExecutionStatus;
  private CommandUnitType commandUnitType;

  public enum CommandUnitType {
    COMMAND("COMMAND"),
    JENKINS(COMMAND_UNIT_NAME),
    HELM(HELM_COMMAND_NAME),
    KUBERNETES_STEADY_STATE_CHECK(KUBERNETES_STEADY_STATE_CHECK_COMMAND_NAME),
    PCF_SETUP(PCF_SETUP_COMMAND),
    PCF_RESIZE(PCF_RESIZE_COMMAND),
    PCF_MAP_ROUTE(PCF_MAP_ROUTE_COMMAND);
    private String name;

    public String getName() {
      return name;
    }

    CommandUnitType(String name) {
      this.name = name;
    }
  }
}
