package software.wings.beans;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 7/12/16.
 */
public class AbstractExecCommandUnit extends CommandUnit {
  @NotEmpty private String command;
  public AbstractExecCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }
}
