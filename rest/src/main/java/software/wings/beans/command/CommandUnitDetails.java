package software.wings.beans.command;

import static software.wings.sm.states.HelmDeployState.HELM_COMMAND_NAME;
import static software.wings.sm.states.JenkinsState.COMMAND_UNIT_NAME;

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
    HELM(HELM_COMMAND_NAME);
    private String name;

    public String getName() {
      return name;
    }

    CommandUnitType(String name) {
      this.name = name;
    }
  }
}
