package software.wings.beans;

import com.github.reinert.jjschema.SchemaIgnore;

/**
 * Created by anubhaw on 7/12/16.
 */
public abstract class AbstractExecCommandUnit extends CommandUnit {
  @SchemaIgnore private String command;

  /**
   * Instantiates a new Abstract exec command unit.
   *
   * @param commandUnitType the command unit type
   */
  public AbstractExecCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
  }

  /**
   * Gets command.
   *
   * @return the command
   */
  @SchemaIgnore
  public String getCommand() {
    return command;
  }

  /**
   * Sets command.
   *
   * @param command the command
   */
  public void setCommand(String command) {
    this.command = command;
  }
}
