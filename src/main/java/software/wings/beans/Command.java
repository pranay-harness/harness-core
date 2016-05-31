package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
@Entity(value = "serviceCommands")
public class Command extends CommandUnit {
  private String name;

  private List<CommandUnit> commandUnits;

  public Command() {
    super(CommandUnitType.COMMAND);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<CommandUnit> getCommandUnits() {
    return commandUnits;
  }

  public void setCommandUnits(List<CommandUnit> commandUnits) {
    this.commandUnits = commandUnits;
  }
}
