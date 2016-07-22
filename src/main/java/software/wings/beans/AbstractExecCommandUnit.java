package software.wings.beans;

import com.github.reinert.jjschema.SchemaIgnore;

/**
 * Created by anubhaw on 7/12/16.
 */
public abstract class AbstractExecCommandUnit extends CommandUnit {
  @SchemaIgnore private String command;

  public AbstractExecCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
  }

  @SchemaIgnore
  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }
}
