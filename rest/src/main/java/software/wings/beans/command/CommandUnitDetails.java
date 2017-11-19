package software.wings.beans.command;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

/**
 * Created by rsingh on 11/17/17.
 */
@Data
@Builder
public class CommandUnitDetails {
  private String name;
  private CommandExecutionStatus commandExecutionStatus;
  private CommandUnitType commandUnitType;

  public enum CommandUnitType { COMMAND, JENKINS }
}
