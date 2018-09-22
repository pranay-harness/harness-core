package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

/**
 * Created by peeyushaggarwal on 11/14/16.
 */
@JsonTypeInfo(use = Id.NAME, property = "commandUnitType", include = As.EXISTING_PROPERTY)
public interface CommandUnit {
  /**
   * Execute execution result.
   *
   * @param context the context
   * @return the execution result
   */
  CommandExecutionStatus execute(CommandExecutionContext context);

  /**
   * Gets command unit type.
   *
   * @return the command unit type
   */
  @SchemaIgnore CommandUnitType getCommandUnitType();

  /**
   * Sets command unit type.
   *
   * @param commandUnitType the command unit type
   */
  void setCommandUnitType(CommandUnitType commandUnitType);

  /**
   * Gets execution result.
   *
   * @return the execution result
   */
  @SchemaIgnore CommandExecutionStatus getCommandExecutionStatus();

  /**
   * Sets execution result.
   *
   * @param commandExecutionStatus the execution result
   */
  void setCommandExecutionStatus(CommandExecutionStatus commandExecutionStatus);

  /**
   * Gets name.
   *
   * @return the name
   */
  @SchemaIgnore String getName();

  /**
   * Sets name.
   *
   * @param name the name
   */
  @SchemaIgnore void setName(String name);

  /**
   * Is artifact needed boolean.
   *
   * @return the boolean
   */
  @SchemaIgnore boolean isArtifactNeeded();

  /**
   * Sets artifact needed.
   *
   * @param artifactNeeded the artifact needed
   */
  void setArtifactNeeded(boolean artifactNeeded);

  /**
   * Gets deployment type.
   *
   * @return the deployment type
   */
  @Deprecated @SchemaIgnore String getDeploymentType();

  /**
   * Sets deployment type.
   *
   * @param deploymentType the deployment type
   */
  @Deprecated void setDeploymentType(String deploymentType);
}
