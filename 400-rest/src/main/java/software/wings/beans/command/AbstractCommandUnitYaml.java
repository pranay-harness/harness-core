package software.wings.beans.command;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import software.wings.yaml.command.CommandRefYaml;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandUnitType", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = SetupEnvCommandUnit.Yaml.class, name = "SETUP_ENV")
  , @JsonSubTypes.Type(value = ExecCommandUnit.Yaml.class, name = "EXEC"),
      @JsonSubTypes.Type(value = ScpCommandUnit.Yaml.class, name = "SCP"),
      @JsonSubTypes.Type(value = CopyConfigCommandUnit.Yaml.class, name = "COPY_CONFIGS"),
      @JsonSubTypes.Type(value = CommandRefYaml.class, name = "COMMAND"),
      @JsonSubTypes.Type(value = DockerStartCommandUnit.Yaml.class, name = "DOCKER_START"),
      @JsonSubTypes.Type(value = DockerStopCommandUnit.Yaml.class, name = "DOCKER_STOP"),
      @JsonSubTypes.Type(value = ProcessCheckRunningCommandUnit.Yaml.class, name = "PROCESS_CHECK_RUNNING"),
      @JsonSubTypes.Type(value = ProcessCheckStoppedCommandUnit.Yaml.class, name = "PROCESS_CHECK_STOPPED"),
      @JsonSubTypes.Type(value = PortCheckClearedCommandUnitYaml.class, name = "PORT_CHECK_CLEARED"),
      @JsonSubTypes.Type(value = PortCheckListeningCommandUnit.Yaml.class, name = "PORT_CHECK_LISTENING"),
      @JsonSubTypes.Type(value = ResizeCommandUnit.Yaml.class, name = "RESIZE"),
      @JsonSubTypes.Type(value = CodeDeployCommandUnit.Yaml.class, name = "CODE_DEPLOY"),
      @JsonSubTypes.Type(value = AwsLambdaCommandUnit.Yaml.class, name = "AWS_LAMBDA"),
      @JsonSubTypes.Type(value = AmiCommandUnitYaml.class, name = "AWS_AMI"),
      @JsonSubTypes.Type(value = KubernetesResizeCommandUnit.Yaml.class, name = "RESIZE_KUBERNETES"),
      @JsonSubTypes.Type(value = KubernetesSetupCommandUnit.Yaml.class, name = "KUBERNETES_SETUP"),
      @JsonSubTypes.Type(value = EcsSetupCommandUnit.Yaml.class, name = "ECS_SETUP"),
      @JsonSubTypes.Type(value = DownloadArtifactCommandUnit.Yaml.class, name = "DOWNLOAD_ARTIFACT")
})

public abstract class AbstractCommandUnitYaml extends BaseYaml {
  private String name;
  private String commandUnitType;
  private String deploymentType;

  public AbstractCommandUnitYaml(String commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  public AbstractCommandUnitYaml(String name, String commandUnitType, String deploymentType) {
    this.name = name;
    this.commandUnitType = commandUnitType;
    this.deploymentType = deploymentType;
  }
}
