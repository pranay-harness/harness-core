package software.wings.beans;

import com.google.common.collect.Lists;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
public class Command extends CommandUnit {
  @NotEmpty private String name;

  @NotEmpty private List<CommandUnit> commandUnits = Lists.newArrayList();

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

  public static final class Builder {
    private String name;
    private List<CommandUnit> commandUnits = Lists.newArrayList();
    private String serviceId;
    private ExecutionResult executionResult;

    private Builder() {}

    public static Builder aCommand() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withCommandUnits(List<CommandUnit> commandUnits) {
      this.commandUnits = commandUnits;
      return this;
    }

    public Builder addCommandUnit(CommandUnit commandUnit) {
      this.commandUnits.add(commandUnit);
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    public Builder but() {
      return aCommand()
          .withName(name)
          .withCommandUnits(commandUnits)
          .withServiceId(serviceId)
          .withExecutionResult(executionResult);
    }

    public Command build() {
      Command command = new Command();
      command.setName(name);
      command.setCommandUnits(commandUnits);
      command.setServiceId(serviceId);
      command.setExecutionResult(executionResult);
      return command;
    }
  }
}
