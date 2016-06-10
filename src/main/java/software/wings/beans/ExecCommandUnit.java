package software.wings.beans;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static software.wings.beans.CommandUnitType.EXEC;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.hibernate.validator.constraints.NotEmpty;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/25/16.
 */
public class ExecCommandUnit extends CommandUnit {
  @NotEmpty private String commandPath;
  @NotEmpty private String commandString;

  @Override
  public void setup(CommandExecutionContext context) {
    commandPath = isNullOrEmpty(commandPath) ? context.getRuntimePath() : context.getRuntimePath() + commandPath;
    commandString = format("cd %s && %s", commandPath, commandString);
  }

  /**
   * Instantiates a new exec command unit.
   */
  public ExecCommandUnit() {
    super(EXEC);
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

  /**
   * Getter for property 'commandString'.
   *
   * @return Value for property 'commandString'.
   */
  public String getCommandString() {
    return commandString;
  }

  /**
   * Setter for property 'commandString'.
   *
   * @param commandString Value to set for property 'commandString'.
   */
  public void setCommandString(String commandString) {
    this.commandString = commandString;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ExecCommandUnit that = (ExecCommandUnit) o;
    return Objects.equal(commandPath, that.commandPath) && Objects.equal(commandString, that.commandString);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(commandPath, commandString);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("commandPath", commandPath)
        .add("commandString", commandString)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String commandPath;
    private String commandString;
    private String name;
    private String serviceId;
    private String appId;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;
    private boolean artifactNeeded;

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

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
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

    public Builder but() {
      return anExecCommandUnit()
          .withCommandPath(commandPath)
          .withCommandString(commandString)
          .withName(name)
          .withServiceId(serviceId)
          .withAppId(appId)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult)
          .withArtifactNeeded(artifactNeeded);
    }

    public ExecCommandUnit build() {
      ExecCommandUnit execCommandUnit = new ExecCommandUnit();
      execCommandUnit.setCommandPath(commandPath);
      execCommandUnit.setCommandString(commandString);
      execCommandUnit.setName(name);
      execCommandUnit.setServiceId(serviceId);
      execCommandUnit.setAppId(appId);
      execCommandUnit.setCommandUnitType(commandUnitType);
      execCommandUnit.setExecutionResult(executionResult);
      execCommandUnit.setArtifactNeeded(artifactNeeded);
      return execCommandUnit;
    }
  }
}
