package software.wings.beans.command;

import io.harness.expression.ExpressionEvaluator;
import io.harness.logging.CommandExecutionStatus;
import io.harness.yaml.BaseYaml;

import software.wings.beans.Variable;
import software.wings.yaml.command.CommandRefYaml;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 5/25/16.
 */
public abstract class AbstractCommandUnit implements CommandUnit {
  @SchemaIgnore private String name;
  private CommandUnitType commandUnitType;
  @SchemaIgnore private CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.QUEUED;
  @SchemaIgnore private boolean artifactNeeded;
  @Deprecated @SchemaIgnore private String deploymentType;
  private List<Variable> variables = new ArrayList<>();
  /**
   * Instantiates a new Command unit.
   */
  public AbstractCommandUnit() {}

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public AbstractCommandUnit(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  /**
   * Gets command unit type.
   *
   * @return the command unit type
   */
  @Override
  @SchemaIgnore
  public CommandUnitType getCommandUnitType() {
    return commandUnitType;
  }

  /**
   * Sets command unit type.
   *
   * @param commandUnitType the command unit type
   */
  @Override
  public void setCommandUnitType(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  /**
   * Gets execution status.
   *
   * @return the execution status
   */
  @Override
  @SchemaIgnore
  public CommandExecutionStatus getCommandExecutionStatus() {
    return commandExecutionStatus;
  }

  /**
   * Sets execution status.
   *
   * @param commandExecutionStatus the execution status
   */
  @Override
  public void setCommandExecutionStatus(CommandExecutionStatus commandExecutionStatus) {
    this.commandExecutionStatus = commandExecutionStatus;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  @Override
  @SchemaIgnore
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  @Override
  @SchemaIgnore
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Is artifact needed boolean.
   *
   * @return the boolean
   */
  @Override
  @SchemaIgnore
  public boolean isArtifactNeeded() {
    // NOTE: Whenever this method is overridden, updateServiceArtifactVariableNames might also need to be updated to
    // prevent infinite recursion.
    return artifactNeeded;
  }

  @SchemaIgnore
  @Override
  public void updateServiceArtifactVariableNames(Set<String> serviceArtifactVariableNames) {
    if (isArtifactNeeded()) {
      serviceArtifactVariableNames.add(ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME);
    }
  }

  /**
   * Sets artifact needed.
   *
   * @param artifactNeeded the artifact needed
   */
  @Override
  public void setArtifactNeeded(boolean artifactNeeded) {
    this.artifactNeeded = artifactNeeded;
  }

  @Override
  @SchemaIgnore
  public String getDeploymentType() {
    return deploymentType;
  }

  /**
   * Sets deployment type.
   *
   * @param deploymentType the deployment type
   */
  @Override
  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  @Override
  public List<Variable> getVariables() {
    return variables;
  }

  @Override
  public void setVariables(List<Variable> variables) {
    this.variables = variables;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("commandUnitType", commandUnitType)
        .add("commandExecutionStatus", commandExecutionStatus)
        .add("artifactNeeded", artifactNeeded)
        .toString();
  }

  /**
   * The enum Command unit execution status.
   */
  public enum CommandUnitExecutionResult {
    /**
     * Stop command unit execution status.
     */
    STOP,
    /**
     * Continue command unit execution status.
     */
    CONTINUE;
  }
}
