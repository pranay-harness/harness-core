package software.wings.beans;

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
   * The Class Builder.
   */
  public static final class Builder {
    private String commandPath;
    private String commandString;
    private String name;
    private String serviceId;
    private CommandUnitType commandUnitType;
    private ExecutionResult executionResult;

    /**
     * Do not instantiate Builder.
     */
    private Builder() {}

    /**
     * An exec command unit.
     *
     * @return the builder
     */
    public static Builder anExecCommandUnit() {
      return new Builder();
    }

    /**
     * With command path.
     *
     * @param commandPath the command path
     * @return the builder
     */
    public Builder withCommandPath(String commandPath) {
      this.commandPath = commandPath;
      return this;
    }

    /**
     * With command string.
     *
     * @param commandString the command string
     * @return the builder
     */
    public Builder withCommandString(String commandString) {
      this.commandString = commandString;
      return this;
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With service id.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With command unit type.
     *
     * @param commandUnitType the command unit type
     * @return the builder
     */
    public Builder withCommandUnitType(CommandUnitType commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    /**
     * With execution result.
     *
     * @param executionResult the execution result
     * @return the builder
     */
    public Builder withExecutionResult(ExecutionResult executionResult) {
      this.executionResult = executionResult;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return anExecCommandUnit()
          .withCommandPath(commandPath)
          .withCommandString(commandString)
          .withName(name)
          .withServiceId(serviceId)
          .withCommandUnitType(commandUnitType)
          .withExecutionResult(executionResult);
    }

    /**
     * Builds the.
     *
     * @return the exec command unit
     */
    public ExecCommandUnit build() {
      ExecCommandUnit execCommandUnit = new ExecCommandUnit();
      execCommandUnit.setCommandPath(commandPath);
      execCommandUnit.setCommandString(commandString);
      execCommandUnit.setName(name);
      execCommandUnit.setServiceId(serviceId);
      execCommandUnit.setCommandUnitType(commandUnitType);
      execCommandUnit.setExecutionResult(executionResult);
      return execCommandUnit;
    }
  }
}
