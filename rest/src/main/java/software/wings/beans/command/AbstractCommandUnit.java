package software.wings.beans.command;

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
@Data
public abstract class AbstractCommandUnit implements CommandUnit {
  @SchemaIgnore private String name;
  private CommandUnitType commandUnitType;
  @SchemaIgnore private CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.QUEUED;
  @SchemaIgnore private boolean artifactNeeded = false;
  @SchemaIgnore private String deploymentType;

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
        @Type(value = KubernetesResizeCommandUnit.Yaml.class, name = "RESIZE_KUBERNETES")
  })
  public static abstract class Yaml extends BaseYaml {
    private String name;
    private String commandUnitType;
    private String deploymentType;

    public static abstract class Builder {
      protected String name;
      protected String commandUnitType;
      protected String deploymentType;

      protected Builder() {}

      public Builder withName(String name) {
        this.name = name;
        return this;
      }

      public Builder withCommandUnitType(String commandUnitType) {
        this.commandUnitType = commandUnitType;
        return this;
      }

      public Builder withDeploymentType(String deploymentType) {
        this.deploymentType = deploymentType;
        return this;
      }

      public <T extends AbstractCommandUnit.Yaml> T build() {
        T yaml = getCommandUnitYaml();
        yaml.setName(name);
        yaml.setCommandUnitType(commandUnitType);
        yaml.setDeploymentType(deploymentType);
        return yaml;
      }

      protected abstract <T extends AbstractCommandUnit.Yaml> T getCommandUnitYaml();
    }
  }
}
