package software.wings.beans;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static software.wings.beans.CommandUnitType.EXEC;

import com.github.reinert.jjschema.Attributes;
import org.hibernate.validator.constraints.NotEmpty;

import java.nio.file.Paths;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/25/16.
 */
public class ExecCommandUnit extends AbstractExecCommandUnit {
  @Attributes(title = "Execution Directory", description = "Relative to ${RuntimePath}")
  @NotEmpty
  private String commandPath;
  @Attributes(title = "Command") @NotEmpty private String commandString;

  /**
   * Instantiates a new exec command unit.
   */
  public ExecCommandUnit() {
    super(EXEC);
  }

  @Override
  public void setup(CommandExecutionContext context) {
    commandPath =
        Paths.get(isNullOrEmpty(commandPath) ? context.getRuntimePath() : context.getRuntimePath() + "/" + commandPath)
            .toString();
    setCommand(format("cd %s && set -m; %s", commandPath, commandString.trim()));
  }

  /**
   * Getter for property 'commandPath'.
   *
   * @return Value for property 'commandPath'.
   */
  public String getCommandPath() {
    return commandPath;
  }

  /**
   * Setter for property 'commandPath'.
   *
   * @param commandPath Value to set for property 'commandPath'.
   */
  public void setCommandPath(String commandPath) {
    this.commandPath = commandPath;
  }

  public String getCommandString() {
    return commandString;
  }

  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }

  public static final class Builder {
    private String commandPath;
    private String commandString;
    private String command;
    private String name;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;
    private boolean artifactNeeded = false;
    private boolean processCommandOutput = false;

    private Builder() {}

    public static Builder anExecCommandUnit() {
      return new Builder();
    }

    public Builder withCommandPath(String commandPath) {
      this.commandPath = commandPath;
      return this;
    }

    public Builder withCommandString(String commandString) {
      this.commandString = commandString;
      return this;
    }

    public Builder withCommand(String command) {
      this.command = command;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    public Builder withArtifactNeeded(boolean artifactNeeded) {
      this.artifactNeeded = artifactNeeded;
      return this;
    }

    public Builder withProcessCommandOutput(boolean processCommandOutput) {
      this.processCommandOutput = processCommandOutput;
      return this;
    }

    public Builder but() {
      return anExecCommandUnit()
          .withCommandPath(commandPath)
          .withCommandString(commandString)
          .withCommand(command)
          .withName(name)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult)
          .withArtifactNeeded(artifactNeeded)
          .withProcessCommandOutput(processCommandOutput);
    }

    public ExecCommandUnit build() {
      ExecCommandUnit execCommandUnit = new ExecCommandUnit();
      execCommandUnit.setCommandPath(commandPath);
      execCommandUnit.setCommandString(commandString);
      execCommandUnit.setCommand(command);
      execCommandUnit.setName(name);
      execCommandUnit.setCommandUnitType(commandUnitType);
      execCommandUnit.setExecutionResult(executionResult);
      execCommandUnit.setArtifactNeeded(artifactNeeded);
      execCommandUnit.setProcessCommandOutput(processCommandOutput);
      return execCommandUnit;
    }
  }
}
