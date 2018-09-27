package software.wings.beans.command;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.command.CommandRefYaml;

/**
 * Created by anubhaw on 5/25/16.
 */
public abstract class AbstractCommandUnit implements CommandUnit {
  @SchemaIgnore private String name;
  private CommandUnitType commandUnitType;
  @SchemaIgnore private CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.QUEUED;
  @SchemaIgnore private boolean artifactNeeded;
  @Deprecated @SchemaIgnore private String deploymentType;

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
  @SchemaIgnore
  public CommandExecutionStatus getCommandExecutionStatus() {
    return commandExecutionStatus;
  }

  /**
   * Sets execution status.
   *
   * @param commandExecutionStatus the execution status
   */
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
    return artifactNeeded;
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
  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeInfo(use = Id.NAME, property = "commandUnitType", include = As.EXISTING_PROPERTY)
  @JsonSubTypes({
    @Type(value = SetupEnvCommandUnit.Yaml.class, name = "SETUP_ENV")
    , @Type(value = ExecCommandUnit.Yaml.class, name = "EXEC"), @Type(value = ScpCommandUnit.Yaml.class, name = "SCP"),
        @Type(value = CopyConfigCommandUnit.Yaml.class, name = "COPY_CONFIGS"),
        @Type(value = CommandRefYaml.class, name = "COMMAND"),
        @Type(value = DockerStartCommandUnit.Yaml.class, name = "DOCKER_START"),
        @Type(value = DockerStopCommandUnit.Yaml.class, name = "DOCKER_STOP"),
        @Type(value = ProcessCheckRunningCommandUnit.Yaml.class, name = "PROCESS_CHECK_RUNNING"),
        @Type(value = ProcessCheckStoppedCommandUnit.Yaml.class, name = "PROCESS_CHECK_STOPPED"),
        @Type(value = PortCheckClearedCommandUnit.Yaml.class, name = "PORT_CHECK_CLEARED"),
        @Type(value = PortCheckListeningCommandUnit.Yaml.class, name = "PORT_CHECK_LISTENING"),
        @Type(value = ResizeCommandUnit.Yaml.class, name = "RESIZE"),
        @Type(value = CodeDeployCommandUnit.Yaml.class, name = "CODE_DEPLOY"),
        @Type(value = AwsLambdaCommandUnit.Yaml.class, name = "AWS_LAMBDA"),
        @Type(value = AmiCommandUnit.Yaml.class, name = "AWS_AMI"),
        @Type(value = KubernetesResizeCommandUnit.Yaml.class, name = "RESIZE_KUBERNETES"),
        @Type(value = KubernetesSetupCommandUnit.Yaml.class, name = "KUBERNETES_SETUP"),
        @Type(value = EcsSetupCommandUnit.Yaml.class, name = "ECS_SETUP"),
        @Type(value = DownloadArtifactCommandUnit.Yaml.class, name = "DOWNLOAD_ARTIFACT")
  })
  public abstract static class Yaml extends BaseYaml {
    private String name;
    private String commandUnitType;
    private String deploymentType;

    public Yaml(String commandUnitType) {
      this.commandUnitType = commandUnitType;
    }

    public Yaml(String name, String commandUnitType, String deploymentType) {
      this.name = name;
      this.commandUnitType = commandUnitType;
      this.deploymentType = deploymentType;
    }
  }
}
